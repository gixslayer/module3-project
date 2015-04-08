package application;

import client.CacheCallbacks;
import client.Client;
import client.ClientCache;
import protocol.AnnouncePacket;
import protocol.MulticastChatPacket;
import protocol.Packet;
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
		announceThread.close();
		mci.close();
	}
	
	public void sendToAll(Packet packet) {
		mci.send(packet);
	}
	
	//-------------------------------------------
	// MulticastCallbacks.
	//-------------------------------------------
	@Override
	public void onMulticastPacketReceived(Packet packet) {
		if(packet.getType() == Packet.TYPE_ANNOUNCE) {
			handleAnnouncePacket((AnnouncePacket)packet);
		} else if(packet.getType() == Packet.TYPE_MULTICAST_CHAT) {
			handleMulticastChatPacket((MulticastChatPacket)packet);
		}
	}
	
	private void handleAnnouncePacket(AnnouncePacket packet) {
		if(packet.getSourceClient().equals(localClient)) {
			return;
		}
		
		Client source = packet.getSourceClient();
		source.setAddress(packet.getAddress());
		clientCache.updateDirect(source);
		
		for(Client client : packet.getKnownClients()) {
			clientCache.updateIndirect(source, client);
		}
	}
	
	private void handleMulticastChatPacket(MulticastChatPacket packet) {
		callbacks.onChatMessageReceived(packet.getName(), packet.getMessage());
	}

	//-------------------------------------------
	// CacheCallbacks.
	//-------------------------------------------
	@Override
	public void onClientTimedOut(Client client) {
		callbacks.onClientTimedOut(client);
		//String lastSeen = DateUtils.timestampToDateString(client.getLastSeen(), "HH:mm:ss");
		//System.out.printf("Client %s has timed out (last seen: %s)%n", client, lastSeen);
	}

	@Override
	public void onClientConnected(Client client) {
		callbacks.onClientConnected(client);
		//String lastSeen = DateUtils.timestampToDateString(client.getLastSeen(), "HH:mm:ss");
		//System.out.printf("Client %s has connected (last seen: %s)%n", client, lastSeen);	
	}

	@Override
	public void onClientDisconnected(Client client) {
		callbacks.onClientDisconnected(client);
		//String lastSeen = DateUtils.timestampToDateString(client.getLastSeen(), "HH:mm:ss");
		//System.out.printf("Client %s has disconnected (last seen: %s)%n", client, lastSeen);	
	}
	
	@Override
	public void onClientLostRoute(Client client, Client route) {
		callbacks.onClientLostRoute(client, route);
		//System.out.printf("Client %s has disconnected as its route %s has disconnected%n", client, route);
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
		MulticastChatPacket packet = new MulticastChatPacket(localClient.getName(), message);
		sendToAll(packet);
	}
}
