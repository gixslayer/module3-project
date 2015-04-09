package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import project.TCP;
import protocol.Packet;
import protocol.PacketFactory;
import subscription.Subscribable;
import subscription.SubscriptionCollection;

public final class NetworkInterface implements Subscribable<NetworkCallbacks> {
	public static final int RECV_BUFFER_SIZE = 4096;
	
	private final DatagramSocket socket;
	private final int port;
	private final InetAddress myAddress;
	private final byte[] recvBuffer;
	private final SubscriptionCollection<NetworkCallbacks> callbacks;
	
	public NetworkInterface(InetAddress myAddress, int port) {
		try {
			this.socket = new DatagramSocket(port);
			this.myAddress = myAddress;
			this.port = port;
			this.recvBuffer = new byte[RECV_BUFFER_SIZE];
			this.callbacks = new SubscriptionCollection<NetworkCallbacks>();
		} catch (SocketException e) {
			throw new RuntimeException(String.format("Failed to create network interface: %s", e.getMessage()));
		}
	}
	
	public void start() {
		(new ReceiveThread()).start();
	}
	
	public void stop() {
		socket.close();
	}
	
	public void send(Packet packet, InetAddress source, InetAddress destination) {
		byte[] data = packet.serialize();
		System.out.println("Sending packet to: " + destination.getCanonicalHostName());
		TCP.sendData(this, source, destination, data);
	}
	
	public void send(InetAddress dest, byte[] data) {
		DatagramPacket datagram = new DatagramPacket(data,  0, data.length, dest, port);
		System.out.println("Sending! " + dest.getCanonicalHostName());
		
		try {
			socket.send(datagram);
		} catch (IOException e) {
			System.err.printf("IOException during DatagramSocket.send: %s", e.getMessage());
		}
	}
	
	/*public void send(Client dest, Packet packet) {
		byte[] data = packet.serialize();
		DatagramPacket datagram = new DatagramPacket(data,  0, data.length, dest.getAddress(), port);
		
		try {
			socket.send(datagram);
		} catch (IOException e) {
			System.err.printf("IOException during DatagramSocket.send: %s%n", e.getMessage());
		}
	}*/
	
	public Packet recv() {
		try {
			DatagramPacket datagram = new DatagramPacket(recvBuffer, RECV_BUFFER_SIZE);
			socket.receive(datagram);
			byte[] receivedData = new byte[datagram.getLength()];
			System.arraycopy(recvBuffer, datagram.getOffset(), receivedData, 0, receivedData.length);
			InetAddress address = datagram.getAddress();
			byte[] data = TCP.handlePacket(this, myAddress, new project.Packet(receivedData));
		
			if(data != null) {
				Packet packet = Packet.deserialize(address, data);
				return packet;
			}
			return PacketFactory.fromType(Packet.TYPE_EMPTY_PACKET);
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
