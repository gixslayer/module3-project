package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import client.Client;
import project.TCP;
import protocol.AnnouncePacket;
import protocol.Packet;

public final class NetworkInterface {
	public static final int RECV_BUFFER_SIZE = 4096;
	
	private final DatagramSocket socket;
	private final int port;
	private final InetAddress myAddress;
	private final byte[] recvBuffer;
	private final NetworkCallbacks callbacks;
	
	public NetworkInterface(InetAddress myAddress, int port, NetworkCallbacks callbacks) {
		try {
			this.socket = new DatagramSocket(port);
			this.myAddress = myAddress;
			this.port = port;
			this.recvBuffer = new byte[RECV_BUFFER_SIZE];
			this.callbacks = callbacks;
		} catch (SocketException e) {
			throw new RuntimeException(String.format("Failed to create network interface: %s%n", e.getMessage()));
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
			System.err.printf("IOException during DatagramSocket.send: %s%n", e.getMessage());
		}
	}
	
	public void send(Client dest, Packet packet) {
		// TODO: Perhaps specify the remote port explicitly?
		byte[] data = packet.serialize();
		DatagramPacket datagram = new DatagramPacket(data,  0, data.length, dest.getAddress(), port);
		
		try {
			socket.send(datagram);
		} catch (IOException e) {
			System.err.printf("IOException during DatagramSocket.send: %s%n", e.getMessage());
		}
	}
	
	public Packet recv() {
		try {
			DatagramPacket datagram = new DatagramPacket(recvBuffer, RECV_BUFFER_SIZE);
			socket.receive(datagram);
			byte[] receivedData = new byte[datagram.getLength()];
			System.arraycopy(recvBuffer, datagram.getOffset(), receivedData, 0, receivedData.length);
			InetAddress address = datagram.getAddress();
			byte[] data = TCP.handlePacket(myAddress, new project.Packet(receivedData));
		
			if(data != null) {
				Packet packet = Packet.deserialize(address, data);
				return packet;
			}
			return null;
		} catch (IOException e) {
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
				
				callbacks.onPacketReceived(packet);
			}
		}
	}
}
