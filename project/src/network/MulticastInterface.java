package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import protocol.Packet;

public class MulticastInterface {
	public static final int RECV_BUFFER_SIZE = 4096;
	
	private final MulticastSocket socket;
	private final InetAddress group;
	private final int port;
	private final byte[] recvBuffer;
	private final MulticastCallbacks callbacks;
	
	public MulticastInterface(String group, int port, MulticastCallbacks callbacks) {
		try {
			this.socket = new MulticastSocket(port);
			this.group = InetAddress.getByName(group);
			this.port = port;
			this.recvBuffer = new byte[RECV_BUFFER_SIZE];
			this.callbacks = callbacks;
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to create multicast interface: %s%n", e.getMessage()));
		}
	}
	
	public void start() {
		try {
			socket.joinGroup(group);
			(new ReceiveThread()).start();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to start multicast interface: %s%n", e.getMessage()));
		}
	}
	
	public void close() {
		try {
			socket.leaveGroup(group);
		} catch (IOException e) {
		}
		
		// TODO: Verify closing the socket will end the receive thread due to throwning an exception on
		// the blocking receive call.
		socket.close();
	}
	
	public void send(Packet packet) {
		byte[] data = packet.serialize();
		DatagramPacket datagram = new DatagramPacket(data, data.length, group, port);
		
		try {
			socket.send(datagram);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public Packet recv() {
		try {
			DatagramPacket datagram = new DatagramPacket(recvBuffer, RECV_BUFFER_SIZE);
			socket.receive(datagram);
			byte[] receivedData = new byte[datagram.getLength()];
			System.arraycopy(datagram.getData(), datagram.getOffset(), receivedData, 0, receivedData.length);
			InetAddress address = datagram.getAddress();
			Packet packet = Packet.deserialize(address, receivedData);

			return packet;
		} catch(IOException e) {
			return null;
		}
	}
	
	class ReceiveThread extends Thread {
		@Override
		public void run() {
			while(true) {
				Packet packet = recv();
				
				if(packet == null) {
					break;
				}
								
				callbacks.onMulticastPacketReceived(packet);
			}
		}
	}
}
