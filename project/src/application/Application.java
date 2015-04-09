package application;

import gui.GUICallbacks;

import java.net.InetAddress;

import client.CacheCallbacks;
import client.Client;
import client.ClientCache;
import project.TCP;
import protocol.AnnouncePacket;
import protocol.ChatPacket;
import protocol.DisconnectPacket;
import protocol.Packet;
import protocol.PrivateChatPacket;
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

		this.mci = new MulticastInterface(GROUP, MC_PORT);
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
		sendToAll(new DisconnectPacket(localClient));
		
		announceThread.close();
		mci.close();
		ni.stop();
	}
	
	public void sendToAll(Packet packet) {
		mci.send(packet);
		
		Client[] clients = clientCache.getClients();
		
		for(Client client : clients) {
			if(!client.isIndirect()) {
				continue;
			}
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
		InetAddress address = packet.getSourceAddress();
		InetAddress localAddress = localClient.getAddress();
		
		if(localAddress.equals(address)) {
			// Don't do anything if we receive a multicast packet we sent.
			return;
		}
		
		if(type == Packet.TYPE_ANNOUNCE) {
			handleAnnouncePacket((AnnouncePacket)packet);
		} else if(type == Packet.TYPE_DISCONNECT) {
			handleDisconnectPacket((DisconnectPacket)packet);
		} else if(type == Packet.TYPE_CHAT) {
			handleChatPacket((ChatPacket)packet);
		} else if(type == Packet.TYPE_PRIVATE_CHAT) {
			handlePrivateChatPacket((PrivateChatPacket)packet);
		} else if(type == Packet.TYPE_EMPTY_PACKET) {
			return;
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
			subscriber.onChatMessageReceived(packet.getClient(), packet.getMessage());
		}
	}
	
	//-------------------------------------------
	// CacheCallbacks.
	//-------------------------------------------
	@Override
	public void onClientTimedOut(Client client) {
		for(ApplicationCallbacks subscriber : callbacks) {
			subscriber.onClientTimedOut(client);
		}
		
		// TODO: Implement using subscription.
		TCP.closeConnection(client.getAddress());
	}

	@Override
	public void onClientConnected(Client client) {
		for(ApplicationCallbacks subscriber : callbacks) {
			subscriber.onClientConnected(client);
		}
	}

	@Override
	public void onClientDisconnected(Client client) {
		for(ApplicationCallbacks subscriber : callbacks) {
			subscriber.onClientDisconnected(client);
		}
	}
	
	@Override
	public void onClientLostRoute(Client client) {
		for(ApplicationCallbacks subscriber : callbacks) {
			subscriber.onClientLostRoute(client);
		}
	}
	
	//-------------------------------------------
	// GUICallbacks.
	//-------------------------------------------
	@Override
	public void onSendPrivateMessage(Client client, String message, String otherName) {
		PrivateChatPacket packet = new PrivateChatPacket(client, message);
		Client otherClient = clientCache.getClientFromName(otherName);
		ni.send(packet, client.getAddress(), otherClient.getAddress());
	}
	
	@Override
	public void onSendMessage(String message) {
		ChatPacket packet = new ChatPacket(localClient, message);
		sendToAll(packet);
	}

	//-------------------------------------------
	// NetworkCallbacks.
	//-------------------------------------------
	@Override
	public void onPacketReceived(Packet packet) {
		int type = packet.getType();
		
		System.out.println("Got a packet! " + type);
		
		if(type == Packet.TYPE_ANNOUNCE) {
			handleAnnouncePacket((AnnouncePacket)packet);
		} else if(type == Packet.TYPE_DISCONNECT) {
			handleDisconnectPacket((DisconnectPacket)packet);
		} else if(type == Packet.TYPE_CHAT) {
			handleChatPacket((ChatPacket)packet);
		} else if(type == Packet.TYPE_PRIVATE_CHAT) {
			handlePrivateChatPacket((PrivateChatPacket)packet);
		} else if(type == Packet.TYPE_EMPTY_PACKET) {
			return;
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
