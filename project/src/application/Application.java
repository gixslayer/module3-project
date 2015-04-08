package application;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.net.SocketException;

import client.CacheCallbacks;
import client.Client;
import client.ClientCache;
import protocol.AnnouncePacket;
import protocol.ChatPacket;
import protocol.DisconnectPacket;
import protocol.Packet;
import protocol.PrivateChatPacket;
import network.AnnounceThread;
import network.MulticastCallbacks;
import network.MulticastInterface;

public class Application implements MulticastCallbacks, CacheCallbacks, GUICallbacks {
	public static final String GROUP = "228.0.0.0";
	public static final int PORT = 6969;
	public static final int ANNOUNCE_INTERVAL = 1000;
	
	private final MulticastInterface mci;
	private final Client localClient;
	private final ClientCache clientCache;
	private final AnnounceThread announceThread;
	private final ApplicationCallbacks callbacks;

	public Application(String username, ApplicationCallbacks callbacks) {
		this.mci = new MulticastInterface(GROUP, PORT, this);
		this.localClient = new Client(username);
		this.clientCache = new ClientCache(localClient, this);
		this.announceThread = new AnnounceThread(mci, clientCache, ANNOUNCE_INTERVAL);
		this.callbacks = callbacks;
	}
	
	public void start() {
		mci.start();
		announceThread.start();
	}

	public void stop() {
		sendToAll(new DisconnectPacket(localClient.getName()));
		
		announceThread.close();
		mci.close();
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
	
	private boolean isOwnIP(InetAddress address) {
		try {
			return NetworkInterface.getByInetAddress(address) != null;
		} catch (SocketException e) {
			throw new RuntimeException(String.format("Failed to get network interfaces by address: %s%n", e.getMessage()));
		}
	}
	
	//-------------------------------------------
	// MulticastCallbacks.
	//-------------------------------------------
	@Override
	public void onMulticastPacketReceived(Packet packet) {
		int type = packet.getType();
		InetAddress address = packet.getAddress();
		InetAddress localAddress = localClient.getAddress();
		
		if(localAddress == null) {
			if(isOwnIP(address)) {
				localClient.setAddress(address);
				return;
			}
		} else if(localAddress.equals(address)) {
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
		clientCache.clientDisconnected(packet.getName());
	}
	
	private void handleChatPacket(ChatPacket packet) {
		callbacks.onChatMessageReceived(packet.getName(), packet.getMessage());
	}
	
	private void handlePrivateChatPacket(PrivateChatPacket packet) {
		callbacks.onPrivateChatMessageReceived(packet.getName(), packet.getMessage());
	}
	
	//-------------------------------------------
	// CacheCallbacks.
	//-------------------------------------------
	@Override
	public void onClientTimedOut(Client client) {
		callbacks.onClientTimedOut(client);
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
	public void onClientLostRoute(Client client, Client route) {
		callbacks.onClientLostRoute(client, route);
	}
	
	//-------------------------------------------
	// GUICallbacks.
	//-------------------------------------------
	@Override
	public void onSendPrivateMessage(String user, String message) {
		System.out.printf("Send priv message callback: %s -> %s%n", user, message);
	}
	
	@Override
	public void onSendMessage(String message) {
		ChatPacket packet = new ChatPacket(localClient.getName(), message);
		sendToAll(packet);
	}
}
