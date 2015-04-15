package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.ArrayList;
import java.util.List;

import protocol.Packet;

public class MulticastInterface {
	public static final int RECV_BUFFER_SIZE = 4096;
	
	private final MulticastSocket socket;
	private final InetAddress group;
	private final int port;
	private final byte[] recvBuffer;
	private final MulticastCallbacks callbacks;
	private final List<String> addressFilter;
	
	public MulticastInterface(InetAddress localAddress, String group, int port, MulticastCallbacks callbacks) {
		try {
			this.socket = new MulticastSocket(port);
			this.group = InetAddress.getByName(group);
			this.port = port;
			this.recvBuffer = new byte[RECV_BUFFER_SIZE];
			this.callbacks = callbacks;
			this.addressFilter = new ArrayList<String>();
		} catch (IOException e) {
			throw new RuntimeException(String.format("Failed to create multicast interface: %s", e.getMessage()));
		}
		
		addressFilter.add(localAddress.getHostAddress());
		//addressFilter.add("192.168.5.1"); // Ciske
		//addressFilter.add("192.168.5.2"); // Edwin
		//addressFilter.add("192.168.5.3"); // Roy
		//addressFilter.add("192.168.5.4"); // Kevin
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
		byte[] data = packet.serialize();
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
				} else if(addressFilter.contains(packet.getSourceAddress().getHostAddress())) {
					// Don't invoke the callback if we received our own multicast packet. 
					continue;
				}
					
				callbacks.onMulticastPacketReceived(packet);
			}
		}
	}
}
