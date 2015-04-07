package project;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

public class Main {
	static final int PORT = 6969;
	static final String GROUP = "228.9.10.11";
	
	public static void main(String[] args) throws IOException {
		InetAddress group = InetAddress.getByName(GROUP);
		MulticastSocket socket = new MulticastSocket(PORT);
		String data = "Test";
		byte[] buf = new byte[1024];
		
		socket.joinGroup(group);		
		
		DatagramPacket send = new DatagramPacket(data.getBytes(), data.length(), group, PORT);
		socket.send(send);
		
		DatagramPacket recv = new DatagramPacket(buf, buf.length);
		socket.receive(recv);
		String s = new String(recv.getData(), 0, recv.getLength());
		
		socket.leaveGroup(group);
		socket.close();
		
		System.out.println("Received some data: " + s);
	}
}
