package application;

import filetransfer.FileTransferHandle;
import filetransfer.FileTransfer;
import gui.GUICallbacks;

import java.net.InetAddress;

import client.CacheCallbacks;
import client.Client;
import client.ClientCache;
import project.TCP;
import protocol.*;
import utils.NetworkUtils;
import network.AnnounceThread;
import network.MulticastCallbacks;
import network.MulticastInterface;
import network.NetworkInterface;
import network.UnicastCallbacks;
import network.UnicastInterface;

public class Application implements UnicastCallbacks, MulticastCallbacks, CacheCallbacks, GUICallbacks, NetworkInterface {
	public static final String GROUP = "228.0.0.0";
	public static final int MC_PORT = 6969;
	public static final int UDP_PORT = 6970;
	public static final int ANNOUNCE_INTERVAL = 1000;
	
	private final Client localClient;
	private final ClientCache clientCache;
	private final FileTransfer fileTransfer;
	private final MulticastInterface multicastInterface;
	private final UnicastInterface unicastInterface;
	private final AnnounceThread announceThread;
	private final ApplicationCallbacks callbacks;

	public Application(String username, ApplicationCallbacks callbacks) {
		InetAddress localAddress = NetworkUtils.getLocalAddress();

		this.localClient = new Client(username, localAddress);
		this.clientCache = new ClientCache(localClient, this);
		this.fileTransfer = new FileTransfer(callbacks, this);
		this.multicastInterface = new MulticastInterface(localAddress, GROUP, MC_PORT, this);
		this.unicastInterface = new UnicastInterface(localAddress, UDP_PORT, this);
		this.announceThread = new AnnounceThread(multicastInterface, clientCache, ANNOUNCE_INTERVAL);
		this.callbacks = callbacks;
	}
	
	public void start() {
		multicastInterface.start();
		unicastInterface.start();
		announceThread.start();
	}

	public void stop() {
		// TODO: Reconsider how we want to handle this (call it here, reliable/unreliable etc).
		sendToAll(new DisconnectPacket(localClient));
		
		announceThread.close();
		multicastInterface.close();
		unicastInterface.close();
	}
	
	//-------------------------------------------
	// NetworkInterface.
	//-------------------------------------------
	public void sendTo(Client client, Packet packet) {
		if(client.isIndirect()) {
			Client route = client.getRoute();
			RouteRequestPacket routePacket = new RouteRequestPacket(localClient, client, packet.serialize());
			unicastInterface.sendReliable(route.getAddress(), routePacket);
		} else {
			unicastInterface.send(client.getAddress(), packet);
		}
	}
	
	public void sendReliableTo(Client client, Packet packet) {
		if(client.isIndirect()) {
			Client route = client.getRoute();
			RouteRequestPacket routePacket = new RouteRequestPacket(localClient, client, packet.serialize());
			unicastInterface.sendReliable(route.getAddress(), routePacket);
		} else {
			unicastInterface.sendReliable(client.getAddress(), packet);
		}
	}
	
	public void sendToAll(Packet packet) {
		Client[] clients = clientCache.getClients();
		
		for(Client client : clients) {
			sendTo(client, packet);
		}
	}
	
	public void sendReliableToAll(Packet packet) {
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
		int type = packet.getType();

		// Currently only announcement packets should be sent/received using multicast.
		if(type == Packet.TYPE_ANNOUNCE) {
			handleAnnouncePacket((AnnouncePacket)packet);
		}
	}
	
	private void handleAnnouncePacket(AnnouncePacket packet) {
		Client source = packet.getSourceClient();
		source.setAddress(packet.getSourceAddress());
		clientCache.updateDirect(source);
		
		for(Client client : packet.getKnownClients()) {
			clientCache.updateIndirect(source, client);
		}
	}
	
	//-------------------------------------------
	// CacheCallbacks.
	//-------------------------------------------
	@Override
	public void onClientTimedOut(Client client) {
		// TODO: The reliableLayer should be informed of this.
		callbacks.onClientTimedOut(client);
	}

	@Override
	public void onClientConnected(Client client) {
		// TODO: Perhaps the reliableLayer should be informed of this?
		callbacks.onClientConnected(client);
	}

	@Override
	public void onClientDisconnected(Client client) {
		// TODO: The reliableLayer should be informed of this.
		callbacks.onClientDisconnected(client);
	}
	
	@Override
	public void onClientLostRoute(Client client) {
		// TODO: The reliableLayer should be informed of this.
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

	//-------------------------------------------
	// UnicastCallbacks.
	//-------------------------------------------
	@Override
	public void onUnicastPacketReceived(Packet packet) {
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
				unicastInterface.sendReliable(packet.getSourceAddress(), cannotRoutePacket);
			}

			if(cachedDest.isIndirect()) {
				Client route = cachedDest.getRoute();
				unicastInterface.sendReliable(route.getAddress(), packet);
			} else {
				unicastInterface.sendReliable(cachedDest.getAddress(), packet);
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
}
