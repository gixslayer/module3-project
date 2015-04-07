package application;

import java.io.IOException;
import java.net.InetAddress;
import java.text.SimpleDateFormat;
import java.util.Date;

import client.CacheCallbacks;
import client.Client;
import client.ClientCache;
import protocol.AnnouncePacket;
import protocol.MulticastChatPacket;
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
		args = new String[] { "Henk" };
		
		String username = args[0];
		InetAddress address = InetAddress.getLocalHost();
		
		new Application(username, address).start();
	}
	
	public Application(String username, InetAddress address) {
		this.mci = new MulticastInterface(GROUP, PORT, this);
		this.localClient = new Client(username, address, 0);
		this.clientCache = new ClientCache(localClient, this);
		this.announceThread = new AnnounceThread(mci, clientCache, ANNOUNCE_INTERVAL);
	}
	
	public void start() {
		long now = System.currentTimeMillis();
		InetAddress address = localClient.getAddress();
		clientCache.updateDirect(new Client("Jantje69", address, now));
		clientCache.updateDirect(new Client("Paashaas", address, now));
		clientCache.updateDirect(new Client("Kabouter", address, now));
		
		mci.start();
		announceThread.start();
		
		// TODO: Start gui here on this thread.
		System.out.println("Press any key to exit");
		try {
			System.in.read();
		} catch (IOException e) { }
		System.out.println("Closing");
		
		announceThread.close();
		mci.close();
	}

	@Override
	public void onMulticastPacketReceived(Packet packet, InetAddress address) {
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
		clientCache.updateDirect(source);
		
		//System.out.println("[Announcement packet]");
		//System.out.printf("Source: %s%n", packet.getSourceClient());
		//System.out.println("Knows about clients:");
		
		for(Client client : packet.getKnownClients()) {
			//System.out.println(client);
			clientCache.updateIndirect(source, client);
		}
	}
	
	private void handleMulticastChatPacket(MulticastChatPacket packet) {
		System.out.printf("Received a multicast chat message from %s: %s%n", packet.getName(), packet.getMessage());
	}

	@Override
	public void onClientTimedOut(Client client) {
		String lastSeen = DateUtils.timestampToDateString(client.getLastSeen(), "HH:mm:ss");
		System.out.printf("Client %s has timed out (last seen: %s)%n", client, lastSeen);
	}

	@Override
	public void onClientConnected(Client client) {
		String lastSeen = DateUtils.timestampToDateString(client.getLastSeen(), "HH:mm:ss");
		System.out.printf("Client %s has connected (last seen: %s)%n", client, lastSeen);	
	}

	@Override
	public void onClientDisconnected(Client client) {
		String lastSeen = DateUtils.timestampToDateString(client.getLastSeen(), "HH:mm:ss");
		System.out.printf("Client %s has disconnected (last seen: %s)%n", client, lastSeen);	
	}
}
