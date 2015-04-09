package application;

import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

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
import network.NetworkCallbacks;
import network.NetworkInterface;

public class Application implements NetworkCallbacks, MulticastCallbacks, CacheCallbacks, GUICallbacks {
	public static final String GROUP = "228.0.0.0";
	public static final int PORT = 6969;
	public static final int NI_PORT = 6970;
	public static final int ANNOUNCE_INTERVAL = 1000;
	
	private static final String[] availableAddresses = {"192.168.5.1", "192.168.5.2", "192.168.5.3", "192.168.5.4"};
	
	private final MulticastInterface mci;
	private final NetworkInterface ni;
	private final Client localClient;
	private final ClientCache clientCache;
	private final AnnounceThread announceThread;
	private final ApplicationCallbacks callbacks;

	public Application(String username, ApplicationCallbacks callbacks) {
		// TODO: Grab the correct local IP (In this case 192.168.5.X) from the network interfaces and set it in the local client field.
		// A client currently deduces the IP from incoming multicast packets, but if it tries to disconnect before receiving a multicast
		// packet it send it will not have the correct address set and will break things for other clients.
		InetAddress myAddress = null;
		for(int i=0; i<availableAddresses.length; i++) {
			try {
				if(isOwnIP(InetAddress.getByName(availableAddresses[i]))) {
					myAddress = InetAddress.getByName(availableAddresses[i]);
				break;
			}
			} catch (UnknownHostException e) { }
		}
		this.mci = new MulticastInterface(GROUP, PORT, this);
		this.ni = new NetworkInterface(myAddress, NI_PORT, this);
		this.localClient = new Client(username);
		localClient.setAddress(myAddress);
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
	
	private boolean isOwnIP(InetAddress address) {
		try {
			return java.net.NetworkInterface.getByInetAddress(address) != null;
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

	@Override
	public void onPacketReceived(Packet packet) {
		int type = packet.getType();
		InetAddress address = packet.getAddress();
		InetAddress localAddress = localClient.getAddress();
		
		System.out.println("Got a packet!");
		
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
}
