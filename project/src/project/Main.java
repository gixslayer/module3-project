package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Main {
	static final int DEVICE = 4;
	static final int PORT = 6969;
	static final String GROUP = "228.9.10.11";
	
	public Main(String name) throws IOException {
		InetAddress group = InetAddress.getByName(GROUP);
		MulticastSocket socket = new MulticastSocket(PORT);
		socket.joinGroup(group);
		while (true) {
			sendBroadcast(name, socket, group);
			
			DatagramPacket recv = receivePacket(socket, group);
			String s = new String(recv.getData(), 0, recv.getLength());
			System.out.println("Received some data: " + s);
			
			try {
				Thread.sleep(1000);
			}
			catch (InterruptedException e) { }
		}
	}
	
	public DatagramPacket receivePacket(MulticastSocket socket, InetAddress group) throws IOException {
		byte[] buf = new byte[1024];
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		socket.receive(recv);
		return recv;
	}
	
	public void sendBroadcast(String name, MulticastSocket socket, InetAddress group) throws IOException {
		DatagramPacket packet = createPacket("[BC] " + name, group);
		socket.send(packet);
		System.out.println("Send a packet!");
	}
	
	public DatagramPacket createPacket(String data, InetAddress group) {
		return new DatagramPacket(data.getBytes(), data.length(), group, PORT);
	}
	
	public static void main(String[] args) throws IOException {
		InetAddress group = InetAddress.getByName(GROUP);
		MulticastSocket socket = new MulticastSocket(PORT);
		String data = "Test";
		byte[] buf = new byte[1024];
		
		Packet p1 = new Packet(3,2,4,33024,3900,true,true,false,100, new byte[4000]);
		p1.printData();
		Packet p2 = new Packet(p1.getBytes());
		p2.printData();
		
		socket.joinGroup(group);		
		
		DatagramPacket send = new DatagramPacket(data.getBytes(), data.length(), group, PORT);
		socket.send(send);
		
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		socket.receive(recv);
		String s = new String(recv.getData(), 0, recv.getLength());
		
		socket.leaveGroup(group);
		socket.close();
		
		//System.out.println("Received some data: " + s);
	}
}
