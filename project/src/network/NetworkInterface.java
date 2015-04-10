package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import protocol.Packet;
import subscription.Subscribable;
import subscription.SubscriptionCollection;

public final class NetworkInterface implements Subscribable<NetworkCallbacks> {
	public static final int RECV_BUFFER_SIZE = 4096;
	
	private final DatagramSocket socket;
	private final int port;
	private final byte[] recvBuffer;
	private final SubscriptionCollection<NetworkCallbacks> callbacks;
	private final ReliableLayer reliableLayer;
	
	public NetworkInterface(InetAddress localAddress, int port) {
		try {
			this.socket = new DatagramSocket(port);
			this.port = port;
			this.recvBuffer = new byte[RECV_BUFFER_SIZE];
			this.callbacks = new SubscriptionCollection<NetworkCallbacks>();
			this.reliableLayer = new ReliableLayer(localAddress, this);
		} catch (SocketException e) {
			throw new RuntimeException(String.format("Failed to create network interface: %s", e.getMessage()));
		}
	}
	
	public void start() {
		(new ReceiveThread()).start();
	}
	
	public void stop() {
		reliableLayer.close();
		socket.close();
	}
	
	public void send(InetAddress dest, Packet packet) {
		byte[] data = packet.serialize();
		DatagramPacket datagram = new DatagramPacket(data,  0, data.length, dest, port);
		
		try {
			socket.send(datagram);
		} catch (IOException e) {
			//System.err.printf("IOException during DatagramSocket.send: %s%n", e.getMessage());
			throw new RuntimeException(e);
		}
	}
	
	public void sendReliable(InetAddress dest, Packet packet) {
		reliableLayer.send(dest, packet);
	}
	
	private Packet recv() {
		try {
			DatagramPacket datagram = new DatagramPacket(recvBuffer, RECV_BUFFER_SIZE);
			
			socket.receive(datagram);
			
			byte[] receivedData = new byte[datagram.getLength()];
			System.arraycopy(recvBuffer, datagram.getOffset(), receivedData, 0, receivedData.length);
			InetAddress sourceAddress = datagram.getAddress();
			Packet packet = Packet.deserialize(sourceAddress, receivedData);
			
			if(packet.hasHeader()) {
				reliableLayer.onPacketReceived(packet);
			}
			
			return packet;
		} catch (IOException e) {
			return null;
		}
	}
	
	class ReceiveThread extends Thread {
		@Override
		public void run() {
			setName("UDP-recv");
			
			while(true) {
				Packet packet = recv();
				
				if(packet == null) {
					break;
				} else if(packet.getType() == Packet.TYPE_EMPTY) {
					// Don't send empty packets to the callback subscribers (they should only be used by the reliableLayer). 
					continue;
				}
				
				for(NetworkCallbacks subscriber : callbacks) {
					subscriber.onPacketReceived(packet);
				}
			}
		}
	}

	@Override
	public void subscribe(NetworkCallbacks subscription) {
		callbacks.subscribe(subscription);
	}

	@Override
	public void unsubscribe(NetworkCallbacks subscription) {
		callbacks.unsubscribe(subscription);
		
	}
}
