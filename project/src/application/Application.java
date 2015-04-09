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
import utils.NetworkUtils;
import network.AnnounceThread;
import network.MulticastCallbacks;
import network.MulticastInterface;
import network.NetworkCallbacks;
import network.NetworkInterface;

public class Application implements NetworkCallbacks, MulticastCallbacks, CacheCallbacks, GUICallbacks {
	public static final String GROUP = "228.0.0.0";
	public static final int MC_PORT = 6969;
	public static final int UDP_PORT = 6970;
	public static final int ANNOUNCE_INTERVAL = 1000;
	
	private final MulticastInterface mci;
	private final NetworkInterface ni;
	private final Client localClient;
	private final ClientCache clientCache;
	private final AnnounceThread announceThread;
	private final ApplicationCallbacks callbacks;

	public Application(String username, ApplicationCallbacks callbacks) {
		InetAddress localAddress = NetworkUtils.getLocalAddress();

		this.mci = new MulticastInterface(GROUP, MC_PORT, this);
		this.ni = new NetworkInterface(localAddress, UDP_PORT, this);
		this.localClient = new Client(username, localAddress);
		this.clientCache = new ClientCache(localClient, this);
		this.announceThread = new AnnounceThread(mci, clientCache, ANNOUNCE_INTERVAL);
		this.callbacks = callbacks;
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
		InetAddress address = packet.getAddress();
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
		source.setAddress(packet.getAddress());
		clientCache.updateDirect(source);
		
		for(Client client : packet.getKnownClients()) {
			clientCache.updateIndirect(source, client);
		}
	}
	
	private void handleDisconnectPacket(DisconnectPacket packet) {
		clientCache.clientDisconnected(packet.getClient());
	}
	
	private void handleChatPacket(ChatPacket packet) {
		callbacks.onChatMessageReceived(packet.getClient(), packet.getMessage());
	}
	
	private void handlePrivateChatPacket(PrivateChatPacket packet) {
		callbacks.onPrivateChatMessageReceived(packet.getClient(), packet.getMessage());
	}
	
	//-------------------------------------------
	// CacheCallbacks.
	//-------------------------------------------
	@Override
	public void onClientTimedOut(Client client) {
		callbacks.onClientTimedOut(client);
		TCP.closeConnection(client.getAddress());
	}

	@Override
	public void onClientConnected(Client client) {
		callbacks.onClientConnected(client);
	}

	@Override
	public void onClientDisconnected(Client client) {
		callbacks.onClientDisconnected(client);
	}
	
	@Override
	public void onClientLostRoute(Client client) {
		callbacks.onClientLostRoute(client);
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
}
