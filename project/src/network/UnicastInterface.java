package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;

import protocol.Packet;

public final class UnicastInterface {
	public static final int RECV_BUFFER_SIZE = 4096;
	
	private final DatagramSocket socket;
	private final int port;
	private final byte[] recvBuffer;
	private final UnicastCallbacks callbacks;
	
	public UnicastInterface(InetAddress localAddress, int port, UnicastCallbacks callbacks) {
		try {
			this.socket = new DatagramSocket(port);
			this.port = port;
			this.recvBuffer = new byte[RECV_BUFFER_SIZE];
			this.callbacks = callbacks;
		} catch (SocketException e) {
			throw new RuntimeException(String.format("Failed to create unicast interface: %s", e.getMessage()));
		}
	}
	
	public void start() {
		(new ReceiveThread()).start();
	}
	
	public void close() {
		socket.close();
	}

	public void send(InetAddress dest, Packet packet) {
		byte[] data = packet.serialize();
		DatagramPacket datagram = new DatagramPacket(data,  0, data.length, dest, port);
		
		try {
			socket.send(datagram);
		} catch (IOException e) {
			System.err.printf("IOException during DatagramSocket.send: %s%", e.getMessage());
		}
	}
	
	private Packet recv() {
		try {
			DatagramPacket datagram = new DatagramPacket(recvBuffer, RECV_BUFFER_SIZE);
			
			socket.receive(datagram);
			
			byte[] receivedData = new byte[datagram.getLength()];
			System.arraycopy(recvBuffer, datagram.getOffset(), receivedData, 0, receivedData.length);
			InetAddress sourceAddress = datagram.getAddress();
			Packet packet = Packet.deserialize(sourceAddress, receivedData);

			return packet;
		} catch (IOException e) {
			return null;
		}
	}
	
	class ReceiveThread extends Thread {
		@Override
		public void run() {
			setName("Unicast-recv");
			
			while(true) {
				Packet packet = recv();
				
				if(packet == null) {
					break;
				}
				
				callbacks.onUnicastPacketReceived(packet);
			}
		}
	}
}
