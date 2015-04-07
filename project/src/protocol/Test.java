package protocol;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;

import client.Client;

public class Test {
	public static void main(String[] args) throws IOException {
		InetAddress group = InetAddress.getByName("228.0.0.0");
		MulticastSocket socket = new MulticastSocket(6969);
		AnnouncePacket packet = new AnnouncePacket();
		
		//packet.getClient().setName("Henk");
		//packet.getClient().setAddress(100);
		
		for(int i = 0; i < 5; i++) {
			//Client client = new Client(String.format("Client %d", i), i);
			//packet.getKnownClients().add(client);
		}
		
		socket.joinGroup(group);
		
		byte[] serializedPacket = packet.serialize();
		DatagramPacket datagram = new DatagramPacket(serializedPacket, serializedPacket.length, group, 6969);
		socket.send(datagram);
		
		byte[] receiveBuffer = new byte[4096];
		DatagramPacket receivedDatagram = new DatagramPacket(receiveBuffer, receiveBuffer.length);
		socket.receive(receivedDatagram);
		byte[] receivedData = new byte[receivedDatagram.getLength()];
		System.arraycopy(receivedDatagram.getData(), 0, receivedData, 0, receivedData.length);
		Packet receivedPacket = Packet.deserialize(receiveBuffer);
		
		socket.leaveGroup(group);
		socket.close();
	}
}
