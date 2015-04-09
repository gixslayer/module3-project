package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import protocol.Packet;
import subscription.Subscribable;
import subscription.SubscriptionCollection;

public class MulticastInterface implements Subscribable<MulticastCallbacks> {
	public static final int RECV_BUFFER_SIZE = 4096;
	
	private final MulticastSocket socket;
	private final InetAddress localAddress;
	private final InetAddress group;
	private final int port;
	private final byte[] recvBuffer;
	private final SubscriptionCollection<MulticastCallbacks> callbacks;
	
	public MulticastInterface(InetAddress localAddress, String group, int port) {
		try {
			this.socket = new MulticastSocket(port);
			this.localAddress = localAddress;
			this.group = InetAddress.getByName(group);
			this.port = port;
			this.recvBuffer = new byte[RECV_BUFFER_SIZE];
			this.callbacks = new SubscriptionCollection<MulticastCallbacks>();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to create multicast interface: %s", e.getMessage()));
		}
	}
	
	public void start() {
		try {
			socket.joinGroup(group);
			(new ReceiveThread()).start();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to start multicast interface: %s", e.getMessage()));
		}
	}
	
	public void close() {
		try {
			socket.leaveGroup(group);
		} catch (IOException e) {
		}
		
		socket.close();
	}
	
	public void send(Packet packet) {
		send(packet.serialize());
	}
	
	public void send(byte[] data) {
		DatagramPacket datagram = new DatagramPacket(data, data.length, group, port);
		
		try {
			socket.send(datagram);
		} catch (IOException e) {
			System.err.printf("IOException during MulticastSocket.send: %s", e.getMessage());
		}
	}
	
	private Packet recv() {
		try {
			DatagramPacket datagram = new DatagramPacket(recvBuffer, RECV_BUFFER_SIZE);
			
			socket.receive(datagram);
			
			byte[] receivedData = new byte[datagram.getLength()];
			System.arraycopy(datagram.getData(), datagram.getOffset(), receivedData, 0, receivedData.length);
			InetAddress sourceAddress = datagram.getAddress();
			Packet packet = Packet.deserialize(sourceAddress, receivedData);
			
			return packet;
		} catch(IOException e) {
			return null;
		}
	}
	
	class ReceiveThread extends Thread {
		@Override
		public void run() {
			setName("Multicast-recv");
			
			while(true) {
				Packet packet = recv();
				
				if(packet == null) {
					break;
				} else if(packet.getSourceAddress().equals(localAddress)) {
					// Don't send packets we sent over multicast and then received to the callback subscribers. 
					continue;
				}
					
				for(MulticastCallbacks subscriber : callbacks) {
					subscriber.onMulticastPacketReceived(packet);
				}
			}
		}
	}

	@Override
	public void subscribe(MulticastCallbacks subscription) {
		callbacks.subscribe(subscription);
	}

	@Override
	public void unsubscribe(MulticastCallbacks subscription) {
		callbacks.unsubscribe(subscription);
		
	}
}
