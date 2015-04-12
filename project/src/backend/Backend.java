package backend;

import events.Event;
import events.EventQueue;
import events.MulticastPacketReceivedEvent;
import events.UnicastPacketReceivedEvent;
import filetransfer.FileTransferHandle;
import filetransfer.FileTransfer;
import gui.GUICallbacks;

import java.net.InetAddress;
import java.util.Queue;

import client.CacheCallbacks;
import client.Client;
import client.ClientCache;
import project.TCP;
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
	
	private final Client localClient;
	private final ClientCache clientCache;
	private final FileTransfer fileTransfer;
	private final MulticastInterface multicastInterface;
	private final UnicastInterface unicastInterface;
	private final TcpInterface tcpInterface;
	private final AnnounceSender announceSender;
	private final BackendCallbacks callbacks;
	private final EventQueue eventQueue;
	private volatile boolean keepProcessing;

	public Backend(String username, BackendCallbacks callbacks) {
		InetAddress localAddress = NetworkUtils.getLocalAddress();

		this.localClient = new Client(username, localAddress);
		this.clientCache = new ClientCache(localClient, this);
		this.fileTransfer = new FileTransfer(callbacks, this);
		this.multicastInterface = new MulticastInterface(localAddress, GROUP, MULTICAST_PORT, this);
		this.unicastInterface = new UnicastInterface(localAddress, UNICAST_PORT, this);
		this.tcpInterface = new TcpInterface(unicastInterface, this);
		this.announceSender = new AnnounceSender(multicastInterface, clientCache, ANNOUNCE_INTERVAL);
		this.callbacks = callbacks;
		this.eventQueue = new EventQueue();
	}

	public void close() {
		keepProcessing = false;
	}
	
	//-------------------------------------------
	// Processing.
	//-------------------------------------------
	@Override
	public void run() {
		// Startup phase.
		setName("backend");
		
		multicastInterface.start();
		unicastInterface.start();
		keepProcessing = true;
		
		// Enter processing loop.
		while(keepProcessing) {
			process();
			
			// TODO: Should we call a short Thread.sleep here to limit cpu usage?
		}
		
		// Shutdown phase.
		// TODO: Reconsider how we want to handle this (call it here, reliable/unreliable etc).
		sendToAll(new DisconnectPacket(localClient));
		multicastInterface.close();
		unicastInterface.close();
	}
	
	private void process() {
		// Process all events currently queued.
		processEventQueue();
		
		// Send multicast announcement if required.
		announceSender.process();
		
		// Let the client cache check for timed-out clients etc...
		clientCache.process();
	}
	
	private void processEventQueue() {
		Queue<Event> queue = eventQueue.swapBuffers();
		
		while(true) {
			// Grab the next event (if the queue is empty null is returned).
			Event event = queue.poll();
			
			if(event == null) {
				// Queue depleted, break out of loop.
				break;
			}
			
			processEvent(event);
		}
	}
	
	private void processEvent(Event event) {
		int type = event.getType();
		
		if(type == Event.TYPE_MULTICAST_PACKET_RECEIVED) {
			handleMulticastPacketReceivedEvent((MulticastPacketReceivedEvent)event);
		} else if(type == Event.TYPE_UNICAST_PACKET_RECEIVED) {
			handleUnicastPacketReceivedEvent((UnicastPacketReceivedEvent)event);
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
		}
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
		TCP.closeConnection(packet.getSourceAddress());
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
				tcpInterface.send(packet.getSourceAddress(), cannotRoutePacket);
			}

			if(cachedDest.isIndirect()) {
				Client route = cachedDest.getRoute();
				tcpInterface.send(route.getAddress(), packet);
			} else {
				tcpInterface.send(cachedDest.getAddress(), packet);
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
	
	//-------------------------------------------
	// NetworkInterface.
	//-------------------------------------------
	// TODO: Do we even need the concept of 'indirect' clients? Currently we (attempt) to route manually,
	// but if we just try to send UDP traffic (going through our TCP layer or not) won't it do the routing for us?
	
	public void sendTo(Client client, Packet packet) {
		if(client.isIndirect()) {
			Client route = client.getRoute();
			RouteRequestPacket routePacket = new RouteRequestPacket(localClient, client, packet.serialize());
			tcpInterface.send(route.getAddress(), routePacket);
		} else {
			unicastInterface.send(client.getAddress(), packet);
		}
	}
	
	public void sendReliableTo(Client client, Packet packet) {
		if(client.isIndirect()) {
			Client route = client.getRoute();
			RouteRequestPacket routePacket = new RouteRequestPacket(localClient, client, packet.serialize());
			tcpInterface.send(route.getAddress(), routePacket);
		} else {
			tcpInterface.send(client.getAddress(), packet);
		}
	}
	
	public void sendToAll(Packet packet) {
		// TODO: Ensure this is only ever called from the backend thread to avoid multithreading issues on the client cache.
		Client[] clients = clientCache.getClients();
		
		for(Client client : clients) {
			sendTo(client, packet);
		}
	}
	
	public void sendReliableToAll(Packet packet) {
		// TODO: Ensure this is only ever called from the backend thread to avoid multithreading issues on the client cache.
		Client[] clients = clientCache.getClients();
		
		for(Client client : clients) {
			sendReliableTo(client, packet);
		}
	}
	
	public Client getLocalClient() {
		return localClient;
	}
	
	//-------------------------------------------
	// MulticastCallbacks.
	//-------------------------------------------
	@Override
	public void onMulticastPacketReceived(Packet packet) {
		eventQueue.enqueue(new MulticastPacketReceivedEvent(packet));
	}
	
	//-------------------------------------------
	// UnicastCallbacks.
	//-------------------------------------------
	@Override
	public void onUnicastPacketReceived(Packet packet) {
		eventQueue.enqueue(new UnicastPacketReceivedEvent(packet));
	}
	
	//-------------------------------------------
	// TcpCallbacks.
	//-------------------------------------------
	@Override
	public void onTcpPacketReceived(Packet packet) {
		// Clear the packet header so that on the next event queue processing iteration it will be send to the correct event handler
		// instead of being send to the TcpInterface again.
		packet.clearHeader();
		
		eventQueue.enqueue(new UnicastPacketReceivedEvent(packet));
	}
	
	//-------------------------------------------
	// CacheCallbacks.
	//-------------------------------------------
	@Override
	public void onClientTimedOut(Client client) {
		// Communication is no longer possible, force close any existing TCP connections.
		tcpInterface.forceClose(client.getAddress());
		
		callbacks.onClientTimedOut(client);
	}

	@Override
	public void onClientConnected(Client client) {
		// TODO: Perhaps the reliableLayer should be informed of this?
		callbacks.onClientConnected(client);
	}

	@Override
	public void onClientDisconnected(Client client) {
		// Communication is no longer possible, force close any existing TCP connections.
		tcpInterface.forceClose(client.getAddress());

		callbacks.onClientDisconnected(client);
	}
	
	@Override
	public void onClientLostRoute(Client client) {
		// Communication is no longer possible, force close any existing TCP connections.
		tcpInterface.forceClose(client.getAddress());
		
		callbacks.onClientLostRoute(client);
	}
	
	//-------------------------------------------
	// GUICallbacks.
	//-------------------------------------------
	@Override
	public void onSendPrivateMessage(Client client, String message) {
		PrivateChatPacket packet = new PrivateChatPacket(localClient, message);
		
		sendReliableTo(client, packet);
	}
	
	@Override
	public void onSendMessage(String message) {
		ChatPacket packet = new ChatPacket(localClient, message);
		
		sendReliableToAll(packet);
	}
	
	@Override
	public void onSendGroupMessage(String groupName, String message) {
		GroupChatPacket packet = new GroupChatPacket(localClient, groupName, message);
		
		sendReliableToAll(packet);
	}
	
	@Override
	public void onSendPoke(Client client) {
		PokePacket packet = new PokePacket(localClient);

		sendReliableTo(client, packet);
	}
	
	@Override
	public FileTransferHandle onRequestFileTransfer(Client dest, String filePath) {
		return fileTransfer.createRequest(dest, filePath);
	}

	@Override
	public void onReplyToFileTransfer(FileTransferHandle handle, boolean response, String savePath) {
		fileTransfer.sendReply(handle, response, savePath);
	}

	@Override
	public void onCancelFileTransfer(FileTransferHandle handle) {
		fileTransfer.cancel(handle);
	}
}
