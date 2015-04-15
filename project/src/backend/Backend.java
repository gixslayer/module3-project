package backend;

import events.CancelFileTransferEvent;
import events.Event;
import events.FTTaskCancelledEvent;
import events.FTTaskCompletedEvent;
import events.FTTaskFailedEvent;
import events.FTTaskProgressEvent;
import events.MulticastPacketReceivedEvent;
import events.ReplyToFileTransferEvent;
import events.RequestFileTransferEvent;
import events.SendChatEvent;
import events.SendGroupChatEvent;
import events.SendPacketEvent;
import events.SendPokeEvent;
import events.SendPrivateChatEvent;
import events.SendReliablePacketEvent;
import events.UnicastPacketReceivedEvent;
import filetransfer.FileTransferHandle;
import filetransfer.FileTransfer;
import gui.GUICallbacks;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.util.Queue;

import containers.Priority;
import containers.SynchronizedQueue;
import client.CacheCallbacks;
import client.Client;
import client.ClientCache;
import protocol.*;
import utils.NetworkUtils;
import network.AnnounceSender;
import network.MulticastCallbacks;
import network.MulticastInterface;
import network.NetworkInterface;
import network.TcpCallbacks;
import network.TcpInterface;
import network.UnicastCallbacks;
import network.UnicastInterface;

public class Backend extends Thread implements UnicastCallbacks, MulticastCallbacks, TcpCallbacks, CacheCallbacks, GUICallbacks, NetworkInterface {
	public static final String GROUP = "228.0.0.0";
	public static final int MULTICAST_PORT = 6969;
	public static final int UNICAST_PORT = 6970;
	public static final int ANNOUNCE_INTERVAL = 1000;
	public static final boolean DEBUG_PRINTS = false;
	
	private final SynchronizedQueue<Event> eventQueue;
	private final Client localClient;
	private final ClientCache clientCache;
	private final FileTransfer fileTransfer;
	private final MulticastInterface multicastInterface;
	private final UnicastInterface unicastInterface;
	private final TcpInterface tcpInterface;
	private final AnnounceSender announceSender;
	private final BackendCallbacks callbacks;
	private volatile boolean keepProcessing;
	
	private byte[] masterKey = new byte[32];

	public Backend(String username, BackendCallbacks callbacks) {
		InetAddress localAddress = NetworkUtils.getLocalAddress();
		
		try {
			FileInputStream in = new FileInputStream(new File("res/key.bin"));
			in.read(masterKey, 0, 32);
			in.close();
		} catch (FileNotFoundException e) { e.printStackTrace(); } catch (IOException e) { e.printStackTrace(); }

		this.eventQueue = new SynchronizedQueue<Event>();
		this.localClient = new Client(username, localAddress);
		this.clientCache = new ClientCache(localClient, this);
		this.fileTransfer = new FileTransfer(callbacks, this, eventQueue);
		this.multicastInterface = new MulticastInterface(localAddress, GROUP, MULTICAST_PORT, this);
		this.unicastInterface = new UnicastInterface(localAddress, UNICAST_PORT, this);
		this.tcpInterface = new TcpInterface(unicastInterface, this);
		this.announceSender = new AnnounceSender(multicastInterface, clientCache, ANNOUNCE_INTERVAL);
		this.callbacks = callbacks;
	}

	@Override
	public void run() {
		startup();
		
		while(keepProcessing) {			
			process();
			
			try {
				Thread.sleep(10);
			} catch(InterruptedException e) { }
		}
		
		shutdown();
	}
	
	public void close() {
		keepProcessing = false;
	}
	
	private void startup() {
		// Open the sockets/start the receive threads.
		multicastInterface.start();
		unicastInterface.start();
		
		// Set the thread name for debugging purposes.
		setName("backend");		
		keepProcessing = true;
	}
	
	private void shutdown() {
		// TODO: Reconsider how we want to handle this (call it here, reliable/unreliable etc).
		sendToAll(new DisconnectPacket(localClient));
		
		fileTransfer.close();
		
		// Close the receive threads/sockets.
		multicastInterface.close();
		unicastInterface.close();
	}
	//-------------------------------------------
	// Processing.
	//-------------------------------------------
	private void process() {
		long start = System.currentTimeMillis();
		
		long eventsStart = start;
		// Process all events currently queued.
		int eventsProcessed = processEventQueue();
		long eventsDuration = System.currentTimeMillis() - eventsStart;

		// Let the client cache check for timed-out clients etc...
		clientCache.process();
		
		long tcpStart = System.currentTimeMillis();
		// Let the TCP implementation do the work it needs (check for retransmission/send new packets/etc).
		tcpInterface.process();
		long tcpDuration = System.currentTimeMillis() - tcpStart;
		
		// Send multicast announcement if required.
		announceSender.process();
		
		long totalDuration = System.currentTimeMillis() - start;
		
		if(DEBUG_PRINTS) {
			System.out.printf("PROCESS total=%-5d events=%-5d tcp=%-5d events processed=%d%n", totalDuration, eventsDuration, tcpDuration, eventsProcessed);
		}
	}
	
	private int processEventQueue() {
		Queue<Event> queue = eventQueue.swapBuffers();
		int size = queue.size();
		
		while(true) {
			// Grab the next event (if the queue is empty null is returned).
			Event event = queue.poll();
			
			if(event == null) {
				// Queue depleted, break out of loop.
				break;
			}
			
			processEvent(event);
		}
		
		return size;
	}
	
	private void processEvent(Event event) {
		int type = event.getType();
		
		if(type == Event.TYPE_MULTICAST_PACKET_RECEIVED) {
			handleMulticastPacketReceivedEvent((MulticastPacketReceivedEvent)event);
		} else if(type == Event.TYPE_UNICAST_PACKET_RECEIVED) {
			handleUnicastPacketReceivedEvent((UnicastPacketReceivedEvent)event);
		} else if(type == Event.TYPE_SEND_CHAT) {
			handleSendChatEvent((SendChatEvent)event);
		} else if(type == Event.TYPE_SEND_GROUP_CHAT) {
			handleSendGroupChatEvent((SendGroupChatEvent)event);
		} else if(type == Event.TYPE_SEND_POKE) {
			handleSendPokeEvent((SendPokeEvent)event);
		} else if(type == Event.TYPE_SEND_PRIVATE_CHAT) {
			handleSendPrivateChatEvent((SendPrivateChatEvent)event);
		} else if(type == Event.TYPE_REQUEST_FILE_TRANSFER) {
			handleRequestFileTransferEvent((RequestFileTransferEvent)event);
		} else if(type == Event.TYPE_REPLY_TO_FILE_TRANSFER) {
			handleReplyToFileTransferEvent((ReplyToFileTransferEvent)event);
		} else if(type == Event.TYPE_CANCEL_FILE_TRANSFER) {
			handleCancelFileTransferEvent((CancelFileTransferEvent)event);
		} else if(type == Event.TYPE_SEND_PACKET) {
			handleSendPacketEvent((SendPacketEvent)event);
		} else if(type == Event.TYPE_SEND_RELIABLE_PACKET) {
			handleSendReliablePacketEvent((SendReliablePacketEvent)event);
		} else if(type == Event.TYPE_FTTASK_CANCELLED) {
			handleFTTaskCancelledEvent((FTTaskCancelledEvent)event);
		} else if(type == Event.TYPE_FTTASK_COMPLETED) {
			handleFTTaskCompletedEvent((FTTaskCompletedEvent)event);
		} else if(type == Event.TYPE_FTTASK_FAILED) {
			handleFTTaskFailedEvent((FTTaskFailedEvent)event);
		} else if(type == Event.TYPE_FTTASK_PROGRESS) {
			handleFTTaskProgressEvent((FTTaskProgressEvent)event);
		}
	}
	
	//-------------------------------------------
	// Event handlers.
	//-------------------------------------------
	private void handleMulticastPacketReceivedEvent(MulticastPacketReceivedEvent event) {
		Packet packet = event.getPacket();
		int type = packet.getType();
		
		if(type == Packet.TYPE_ANNOUNCE) {
			handleAnnouncePacket((AnnouncePacket)packet);
		}
	}
	
	private void handleUnicastPacketReceivedEvent(UnicastPacketReceivedEvent event) {
		Packet packet = event.getPacket();
		
		// If a packet has a header it is part of TCP communication and we do not want to handle it directly.
		// Send it to the TcpInterface which will add the packet to the event queue again (stripping the header)
		// when it is done processing the packet (or drop the packet entirely if it deems it invalid/control only).
		if(packet.hasHeader()) {
			tcpInterface.onPacketReceived(packet);
			return;
		}
		
		int type = packet.getType();
		
		if(type == Packet.TYPE_DISCONNECT) {
			handleDisconnectPacket((DisconnectPacket)packet);
		} else if(type == Packet.TYPE_CHAT) {
			handleChatPacket((ChatPacket)packet);
		} else if(type == Packet.TYPE_PRIVATE_CHAT) {
			handlePrivateChatPacket((PrivateChatPacket)packet);
		} else if(type == Packet.TYPE_ROUTE_REQUEST) {
			handleRouteRequestPacket((RouteRequestPacket)packet);
		} else if(type == Packet.TYPE_CANNOT_ROUTE) {
			handleCannotRoutePacket((CannotRoutePacket)packet);
		} else if(type == Packet.TYPE_GROUP_CHAT) {
			handleGroupChatPacket((GroupChatPacket)packet);
		} else if(type == Packet.TYPE_POKE) {
			handlePokePacket((PokePacket)packet);
		} else if(type == Packet.TYPE_FT_REQUEST) {
			fileTransfer.onRequestPacketReceived((FTRequestPacket)packet);
		} else if(type == Packet.TYPE_FT_REPLY) {
			fileTransfer.onReplyPacketReceived((FTReplyPacket)packet);
		} else if(type == Packet.TYPE_FT_DATA) {
			fileTransfer.onDataPacketReceived((FTDataPacket)packet);
		} else if(type == Packet.TYPE_FT_CANCEL) {
			fileTransfer.onCancelPacketReceived((FTCancelPacket)packet);
		} else if(type == Packet.TYPE_FT_FAILED) {
			fileTransfer.onFailedPacketReceived((FTFailedPacket)packet);
		} else if(type == Packet.TYPE_FT_PROGRESS) {
			fileTransfer.onProgressPacketReceived((FTProgressPacket)packet);
		} else if(type == Packet.TYPE_FT_COMPLETED) {
			fileTransfer.onCompletedPacketReceived((FTCompletedPacket)packet);
		} else if(type == Packet.TYPE_ENCRYPTION) {
			handleEncryptedPacket((EncryptionPacket)packet);
		}
	}
	
	private void handleSendChatEvent(SendChatEvent event) {
		ChatPacket packet = new ChatPacket(localClient, event.getMessage());
		
		sendReliableToAll(packet, Priority.Normal);
	}
	
	private void handleSendGroupChatEvent(SendGroupChatEvent event) {
		GroupChatPacket packet = new GroupChatPacket(localClient, event.getGroup(), event.getMessage());
		
		sendReliableToAll(packet, Priority.Normal);
	}
	
	private void handleSendPokeEvent(SendPokeEvent event) {
		PokePacket packet = new PokePacket(localClient);
		
		sendReliableTo(event.getClient(), packet, Priority.Normal);
	}
	
	private void handleSendPrivateChatEvent(SendPrivateChatEvent event) {
		PrivateChatPacket packet = new PrivateChatPacket(localClient, event.getMessage());
		EncryptionPacket p = new EncryptionPacket(packet.serialize(), masterKey);
		p.encrypt();
		sendReliableTo(event.getClient(), p, Priority.Normal);
	}
	
	private void handleRequestFileTransferEvent(RequestFileTransferEvent event) {
		fileTransfer.createRequest(event.getClient(), event.getFilePath());
	}
	
	private void handleReplyToFileTransferEvent(ReplyToFileTransferEvent event) {
		fileTransfer.sendReply(event.getHandle(), event.getResponse(), event.getSavePath());
	}
	
	private void handleCancelFileTransferEvent(CancelFileTransferEvent event) {
		fileTransfer.cancel(event.getHandle());
	}
	
	private void handleSendPacketEvent(SendPacketEvent event) {
		sendTo(event.getDestination(), event.getPacket());
	}
	
	private void handleSendReliablePacketEvent(SendReliablePacketEvent event) {
		sendReliableTo(event.getDestination(), event.getPacket(), event.getPriority());
	}
	
	private void handleFTTaskCancelledEvent(FTTaskCancelledEvent event) {
		fileTransfer.taskCancelled(event.getHandle());
	}
	
	private void handleFTTaskCompletedEvent(FTTaskCompletedEvent event) {
		fileTransfer.taskCompleted(event.getHandle());
	}
	
	private void handleFTTaskFailedEvent(FTTaskFailedEvent event) {
		fileTransfer.taskFailed(event.getHandle(), event.getReason());
	}
	
	private void handleFTTaskProgressEvent(FTTaskProgressEvent event) {
		fileTransfer.taskProgress(event.getHandle(), event.getProgress());
	}
	
	//-------------------------------------------
	// Multicast packet handlers.
	//-------------------------------------------
	private void handleAnnouncePacket(AnnouncePacket packet) {
		Client source = packet.getSourceClient();
		source.setAddress(packet.getSourceAddress());
		clientCache.updateDirect(source);
		
		for(Client client : packet.getKnownClients()) {
			clientCache.updateIndirect(source, client);
		}
	}
	
	//-------------------------------------------
	// Unicast packet handlers.
	//-------------------------------------------
	private void handleDisconnectPacket(DisconnectPacket packet) {
		clientCache.clientDisconnected(packet.getClient());
	}
	
	private void handleChatPacket(ChatPacket packet) {
		callbacks.onChatMessageReceived(packet.getClient(), packet.getMessage());
	}
	
	private void handlePrivateChatPacket(PrivateChatPacket packet) {
		callbacks.onPrivateChatMessageReceived(packet.getClient(), packet.getMessage());
	}
	
	private void handleRouteRequestPacket(RouteRequestPacket packet) {
		Client dest = packet.getDest();
		
		if(dest.equals(localClient)) {
			// We received a packet that was indirectly send to us.
			InetAddress originalAddress = packet.getSrc().getAddress();
			Packet originalPacket = Packet.deserialize(originalAddress, packet.getData());
			
			onUnicastPacketReceived(originalPacket);
		} else {
			// We're requested to route this packet to the next hop.
			Client cachedDest = clientCache.getCachedClient(dest);
		
			if(cachedDest == null) {
				// The client that sent this route request thinks it can reach the destination through us, but that is no longer
				// the case. Inform him of this.
				CannotRoutePacket cannotRoutePacket = new CannotRoutePacket(packet.getSrc(), localClient, packet.getDest());
				tcpInterface.send(packet.getSourceAddress(), cannotRoutePacket, Priority.High);
			}

			if(cachedDest.isIndirect()) {
				Client route = cachedDest.getRoute();
				tcpInterface.send(route.getAddress(), packet, Priority.High);
			} else {
				tcpInterface.send(cachedDest.getAddress(), packet, Priority.High);
			}
		}
	}
	
	private void handleCannotRoutePacket(CannotRoutePacket packet) {
		// The route of destination through hop is no longer valid.
		clientCache.routeLost(packet.getDestination());
		
		// If we are not the original sender of the route request, pass this packet to the next hop (direction = to source).
		if(!packet.getSource().equals(localClient)) {
			packet.setHop(localClient);
			
			Client cachedTarget = clientCache.getCachedClient(packet.getSource());
			
			if(cachedTarget == null) {
				// We can no longer route back.
				return;
			}
			
			if(cachedTarget.isIndirect()) {
				Client route = cachedTarget.getRoute();
				unicastInterface.send(route.getAddress(), packet);
			} else {
				unicastInterface.send(cachedTarget.getAddress(), packet);
			}
		}
	}
	
	private void handleGroupChatPacket(GroupChatPacket packet) {
		callbacks.onGroupChatMessageReceived(packet.getSender(), packet.getGroup(), packet.getMessage());
	}

	private void handlePokePacket(PokePacket packet) {
		callbacks.onPokePacketReceived(packet.getClient());
	}
	
	private void handleEncryptedPacket(EncryptionPacket packet) {
		packet.setKey(masterKey);
		packet.decrypt();
		byte[] data = packet.getData();
		UnicastPacketReceivedEvent event = new UnicastPacketReceivedEvent(Packet.deserialize(packet.getSourceAddress(), data));
		handleUnicastPacketReceivedEvent(event);
	}
	
	//-------------------------------------------
	// NetworkInterface.
	//-------------------------------------------
	// TODO: Do we even need the concept of 'indirect' clients? Currently we (attempt) to route manually,
	// but if we just try to send UDP traffic (going through our TCP layer or not) won't it do the routing for us?
	
	public void sendTo(Client client, Packet packet) {
		if(client.isIndirect()) {
			Client route = client.getRoute();
			RouteRequestPacket routePacket = new RouteRequestPacket(localClient, client, packet.serialize());
			tcpInterface.send(route.getAddress(), routePacket, Priority.High);
		} else {
			unicastInterface.send(client.getAddress(), packet);
		}
	}
	
	public void sendReliableTo(Client client, Packet packet, Priority priority) {
		if(client.isIndirect()) {
			Client route = client.getRoute();
			RouteRequestPacket routePacket = new RouteRequestPacket(localClient, client, packet.serialize());
			tcpInterface.send(route.getAddress(), routePacket, Priority.High);
		} else {
			tcpInterface.send(client.getAddress(), packet, priority);
		}
	}
	
	public void sendToAll(Packet packet) {
		// TODO: Ensure this is only ever called from the backend thread to avoid multithreading issues on the client cache.
		Client[] clients = clientCache.getClients();
		
		for(Client client : clients) {
			sendTo(client, packet);
		}
	}
	
	public void sendReliableToAll(Packet packet, Priority priority) {
		// TODO: Ensure this is only ever called from the backend thread to avoid multithreading issues on the client cache.
		Client[] clients = clientCache.getClients();
		
		for(Client client : clients) {
			sendReliableTo(client, packet, priority);
		}
	}
	
	public Client getLocalClient() {
		return localClient;
	}
	
	//-------------------------------------------
	// MulticastCallbacks.
	//-------------------------------------------
	// NOTE: These methods should never process anything backend related directly as they are always called from
	// the multicast receive thread. Push an appropriate event onto the event queue so it can be processed on the backend thread. 
	@Override
	public void onMulticastPacketReceived(Packet packet) {
		eventQueue.enqueue(new MulticastPacketReceivedEvent(packet));
	}
	
	//-------------------------------------------
	// UnicastCallbacks.
	//-------------------------------------------
	// NOTE: These methods should never process anything backend related directly as they are always called from
	// the unicast receive thread. Push an appropriate event onto the event queue so it can be processed on the backend thread. 
	@Override
	public void onUnicastPacketReceived(Packet packet) {
		eventQueue.enqueue(new UnicastPacketReceivedEvent(packet));
	}
	
	//-------------------------------------------
	// TcpCallbacks.
	//-------------------------------------------
	// NOTE: These methods should always be called by the backend thread.
	@Override
	public void onTcpPacketReceived(Packet packet) {
		// We could process the received packet here, but it's nicer to have one central place to process all received packets.
		// Push a new UnicastPacketReceivedEvent onto the event queue, but clear the packet header so that on the next 
		// event queue processing iteration it will be send to the correct event handler instead of being send to the TcpInterface again.
		packet.clearHeader();
		
		eventQueue.enqueue(new UnicastPacketReceivedEvent(packet));
	}
	
	//-------------------------------------------
	// CacheCallbacks.
	//-------------------------------------------
	// NOTE: These methods should always be called by the backend thread.
	@Override
	public void onClientTimedOut(Client client) {
		// Communication is no longer possible, force close any existing TCP connections.
		tcpInterface.forceClose(client.getAddress());
		fileTransfer.cancelActiveTasksFor(client);
		
		callbacks.onClientTimedOut(client);
	}

	@Override
	public void onClientConnected(Client client) {
		callbacks.onClientConnected(client);
	}

	@Override
	public void onClientDisconnected(Client client) {
		// Communication is no longer possible, force close any existing TCP connections.
		tcpInterface.forceClose(client.getAddress());
		fileTransfer.cancelActiveTasksFor(client);
		
		callbacks.onClientDisconnected(client);
	}
	
	@Override
	public void onClientLostRoute(Client client) {
		// Communication is no longer possible, force close any existing TCP connections.
		tcpInterface.forceClose(client.getAddress());
		fileTransfer.cancelActiveTasksFor(client);
		
		callbacks.onClientLostRoute(client);
	}
	
	//-------------------------------------------
	// GUICallbacks.
	//-------------------------------------------
	// NOTE: These methods should never process anything backend related directly as they are always called from
	// the GUI thread. Push an appropriate event onto the event queue so it can be processed on the backend thread. 
	@Override
	public void onSendPrivateMessage(Client client, String message) {
		eventQueue.enqueue(new SendPrivateChatEvent(client, message));
	}
	
	@Override
	public void onSendMessage(String message) {
		eventQueue.enqueue(new SendChatEvent(message));
	}
	
	@Override
	public void onSendGroupMessage(String group, String message) {
		eventQueue.enqueue(new SendGroupChatEvent(group, message));
	}
	
	@Override
	public void onSendPoke(Client client) {
		eventQueue.enqueue(new SendPokeEvent(client));
	}
	
	@Override
	public void onRequestFileTransfer(Client client, String filePath) {
		eventQueue.enqueue(new RequestFileTransferEvent(client, filePath));
	}

	@Override
	public void onReplyToFileTransfer(FileTransferHandle handle, boolean response, String savePath) {
		eventQueue.enqueue(new ReplyToFileTransferEvent(handle, response, savePath));
	}

	@Override
	public void onCancelFileTransfer(FileTransferHandle handle) {
		eventQueue.enqueue(new CancelFileTransferEvent(handle));
	}
}
