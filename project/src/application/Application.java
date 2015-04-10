package application;

import gui.GUICallbacks;

import java.net.InetAddress;

import client.CacheCallbacks;
import client.Client;
import client.ClientCache;
import project.TCP;
import protocol.AnnouncePacket;
import protocol.CannotRoutePacket;
import protocol.ChatPacket;
import protocol.DisconnectPacket;
import protocol.Packet;
import protocol.PrivateChatPacket;
import protocol.RouteRequestPacket;
import subscription.Subscribable;
import subscription.SubscriptionCollection;
import utils.NetworkUtils;
import network.AnnounceThread;
import network.MulticastCallbacks;
import network.MulticastInterface;
import network.NetworkCallbacks;
import network.NetworkInterface;

public class Application implements NetworkCallbacks, MulticastCallbacks, CacheCallbacks, GUICallbacks, Subscribable<ApplicationCallbacks> {
	public static final String GROUP = "228.0.0.0";
	public static final int MC_PORT = 6969;
	public static final int UDP_PORT = 6970;
	public static final int ANNOUNCE_INTERVAL = 1000;
	
	private final MulticastInterface mci;
	private final NetworkInterface ni;
	private final Client localClient;
	private final ClientCache clientCache;
	private final AnnounceThread announceThread;
	private final SubscriptionCollection<ApplicationCallbacks> callbacks;

	public Application(String username) {
		InetAddress localAddress = NetworkUtils.getLocalAddress();

		this.mci = new MulticastInterface(localAddress, GROUP, MC_PORT);
		this.ni = new NetworkInterface(localAddress, UDP_PORT);
		this.localClient = new Client(username, localAddress);
		this.clientCache = new ClientCache(localClient);
		this.announceThread = new AnnounceThread(mci, clientCache, ANNOUNCE_INTERVAL);
		this.callbacks = new SubscriptionCollection<ApplicationCallbacks>();
		
		mci.subscribe(this);
		ni.subscribe(this);
		clientCache.subscribe(this);
	}
	
	public void start() {
		mci.start();
		ni.start();
		announceThread.start();
	}

	public void stop() {
		// TODO: Reconsider how we want to handle this (call it here, reliable/unreliable etc).
		sendToAll(new DisconnectPacket(localClient));
		
		TCP.stopConnections();
		announceThread.close();
		mci.close();
		ni.stop();
	}
	
	public void sendTo(Client dest, Packet packet) {
		if(dest.isIndirect()) {
			Client route = dest.getRoute();
			RouteRequestPacket routePacket = new RouteRequestPacket(localClient, dest, packet.serialize());
			ni.sendReliable(route.getAddress(), routePacket);
		} else {
			ni.send(dest.getAddress(), packet);
		}
	}
	
	public void sendReliableTo(Client dest, Packet packet) {
		if(dest.isIndirect()) {
			Client route = dest.getRoute();
			RouteRequestPacket routePacket = new RouteRequestPacket(localClient, dest, packet.serialize());
			ni.sendReliable(route.getAddress(), routePacket);
		} else {
			ni.sendReliable(dest.getAddress(), packet);
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
		for(ApplicationCallbacks subscriber : callbacks) {
			subscriber.onClientTimedOut(client);
		}
	}

	@Override
	public void onClientConnected(Client client) {
		// TODO: Perhaps the reliableLayer should be informed of this?
		for(ApplicationCallbacks subscriber : callbacks) {
			subscriber.onClientConnected(client);
		}
	}

	@Override
	public void onClientDisconnected(Client client) {
		// TODO: The reliableLayer should be informed of this.
		for(ApplicationCallbacks subscriber : callbacks) {
			subscriber.onClientDisconnected(client);
		}
	}
	
	@Override
	public void onClientLostRoute(Client client) {
		// TODO: The reliableLayer should be informed of this.
		for(ApplicationCallbacks subscriber : callbacks) {
			subscriber.onClientLostRoute(client);
		}
	}
	
	//-------------------------------------------
	// GUICallbacks.
	//-------------------------------------------
	@Override
	public void onSendPrivateMessage(Client otherClient, String message) {
		PrivateChatPacket packet = new PrivateChatPacket(localClient, message);
		sendReliableTo(otherClient, packet);
	}
	
	@Override
	public void onSendMessage(String message) {
		ChatPacket packet = new ChatPacket(localClient, message);
		
		sendReliableToAll(packet);
	}

	//-------------------------------------------
	// NetworkCallbacks.
	//-------------------------------------------
	@Override
	public void onPacketReceived(Packet packet) {
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
		}
	}
	
	private void handleDisconnectPacket(DisconnectPacket packet) {
		clientCache.clientDisconnected(packet.getClient());
	}
	
	private void handleChatPacket(ChatPacket packet) {
		for(ApplicationCallbacks subscriber : callbacks) {
			subscriber.onChatMessageReceived(packet.getClient(), packet.getMessage());
		}
	}
	
	private void handlePrivateChatPacket(PrivateChatPacket packet) {
		for(ApplicationCallbacks subscriber : callbacks) {
			subscriber.onPrivateChatMessageReceived(packet.getClient(), packet.getMessage());
		}
	}
	
	private void handleRouteRequestPacket(RouteRequestPacket packet) {
		Client dest = packet.getDest();
		
		if(dest.equals(localClient)) {
			// We received a packet that was indirectly send to us.
			InetAddress originalAddress = packet.getSrc().getAddress();
			Packet originalPacket = Packet.deserialize(originalAddress, packet.getData());
			
			onPacketReceived(originalPacket);
		} else {
			// We're requested to route this packet to the next hop.
			Client cachedDest = clientCache.getCachedClient(dest);
		
			if(cachedDest == null) {
				// The client that sent this route request thinks it can reach the destination through us, but that is no longer
				// the case. Inform him of this.
				CannotRoutePacket cannotRoutePacket = new CannotRoutePacket(packet.getSrc(), localClient, packet.getDest());
				ni.sendReliable(packet.getSourceAddress(), cannotRoutePacket);
			}

			if(cachedDest.isIndirect()) {
				Client route = cachedDest.getRoute();
				ni.sendReliable(route.getAddress(), packet);
			} else {
				ni.sendReliable(cachedDest.getAddress(), packet);
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
				ni.send(route.getAddress(), packet);
			} else {
				ni.send(cachedTarget.getAddress(), packet);
			}
		}
	}

	//-------------------------------------------
	// Subscribable<ApplicationCallbacks>.
	//-------------------------------------------
	@Override
	public void subscribe(ApplicationCallbacks subscription) {
		callbacks.subscribe(subscription);
	}

	@Override
	public void unsubscribe(ApplicationCallbacks subscription) {
		callbacks.unsubscribe(subscription);
	}
}
