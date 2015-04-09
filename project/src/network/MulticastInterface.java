package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import project.TCP;
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
		
		socket.close();
	}
	
	public void send(Packet packet, InetAddress source, InetAddress destination) {
		byte[] data = packet.serialize();
		System.out.println("Sending packet to: " + destination.getCanonicalHostName());
		TCP.sendData(this, source, destination, data);
	}
	
	public void send(Packet packet) {
		byte[] data = packet.serialize();
		
		DatagramPacket datagram = new DatagramPacket(data, data.length, group, port);
		
		try {
			socket.send(datagram);
		} catch (IOException e) {
			System.err.printf("IOException during MulticastSocket.send: %s%n", e.getMessage());
		}
	}
	
	public void send(byte[] data) {
		
		DatagramPacket datagram = new DatagramPacket(data, data.length, group, port);
		
		try {
			socket.send(datagram);
		} catch (IOException e) {
			System.err.printf("IOException during MulticastSocket.send: %s%n", e.getMessage());
		}
	}
	
	public Packet recv() {
		try {
			DatagramPacket datagram = new DatagramPacket(recvBuffer, RECV_BUFFER_SIZE);
			socket.receive(datagram);
			byte[] receivedData = new byte[datagram.getLength()];
			System.arraycopy(datagram.getData(), datagram.getOffset(), receivedData, 0, receivedData.length);
			InetAddress address = datagram.getAddress();
			byte[] data = TCP.handlePacket(group, new project.Packet(receivedData));
			if(data != null) {
				Packet packet = Packet.deserialize(address, data);
				return packet;
			}
			else return null;
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
