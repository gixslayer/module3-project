package application;

import java.io.IOException;

import client.CacheCallbacks;
import client.Client;
import client.ClientCache;
import protocol.AnnouncePacket;
import protocol.Packet;
import network.AnnounceThread;
import network.MulticastCallbacks;
import network.MulticastInterface;

public class Application implements MulticastCallbacks, CacheCallbacks {
	public static final String GROUP = "228.0.0.0";
	public static final int PORT = 6969;
	public static final int ANNOUNCE_INTERVAL = 1000;
	
	private final MulticastInterface mci;
	private final Client localClient;
	private final ClientCache clientCache;
	private final AnnounceThread announceThread;
	
	public static void main(String[] args) throws IOException {
		args = new String[] { "Henk", "1" };
		
		String username = args[0];
		int address = Integer.parseInt(args[1]);
		
		new Application(username, address).start();
	}
	
	public Application(String username, int address) {
		this.mci = new MulticastInterface(GROUP, PORT, this);
		this.localClient = new Client(username, address, 0);
		this.clientCache = new ClientCache(localClient, this);
		this.announceThread = new AnnounceThread(mci, clientCache, ANNOUNCE_INTERVAL);
	}
	
	public void start() {
		clientCache.update(new Client("Jantje", 2, 0));
		clientCache.update(new Client("Paashaas", 3, 0));
		clientCache.update(new Client("Kabouter", 4, 0));
		
		mci.start();
		announceThread.start();
		
		System.out.println("Press any key to exit");
		try {
			System.in.read();
		} catch (IOException e) { }
		System.out.println("Closing");
		
		announceThread.close();
		mci.close();
	}

	@Override
	public void onPacketReceived(Packet packet) {
		if(packet.getType() == Packet.TYPE_ANNOUNCE) {
			handleAnnouncePacket((AnnouncePacket)packet);
		}
	}
	
	private void handleAnnouncePacket(AnnouncePacket packet) {
		if(packet.getSourceClient().equals(localClient)) {
			return;
		}
		
		clientCache.update(packet.getSourceClient());
		
		System.out.println("[Announcement packet]");
		System.out.printf("Source: %s%n", packet.getSourceClient());
		System.out.println("Knows about clients:");
		
		for(Client client : packet.getKnownClients()) {
			System.out.println(client);
			clientCache.update(client);
		}
	}

	@Override
	public void onClientTimedOut(Client client) {
		System.out.printf("Client %s has timed out%n", client);
	}

	@Override
	public void onClientConnected(Client client) {
		System.out.printf("Client %s has connected%n", client);	
	}

	@Override
	public void onClientDisconnected(Client client) {
		System.out.printf("Client %s has disconnected%n", client);	
	}
}
