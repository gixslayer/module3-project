package test;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Arrays;
import java.util.Random;

public class NetworkInterface {
	private final int localPort;
	private final int remotePort;
	private final DatagramSocket socket;
	private Random random = new Random();
	
	public NetworkInterface(int localPort, int remotePort) throws SocketException {
		this.localPort = localPort;
		this.remotePort = remotePort;
		this.socket = new DatagramSocket(localPort);
	}
	
	public void close() {
		socket.close();
	}
	
	public void send(Packet packet) {
		if(random.nextInt(100) < 0) {
			System.out.println("DROPPED");
			return;
		}
		
		byte[] content = packet.serialize();
		DatagramPacket datagram = new DatagramPacket(content, content.length, InetAddress.getLoopbackAddress(), remotePort);
		
		try {
			socket.send(datagram);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Packet recv() {
		byte[] buffer = new byte[8192];
		DatagramPacket datagram = new DatagramPacket(buffer, buffer.length);
		
		try {
			socket.receive(datagram);
			
			byte[] content = Arrays.copyOfRange(datagram.getData(), datagram.getOffset(), datagram.getOffset() + datagram.getLength());
			Packet packet = new Packet();
			packet.deserialize(content);
			
			return packet;
		} catch (IOException e) {
			return null;
		}
	}
}
