package network;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.util.Queue;

import containers.SynchronizedQueue;
import protocol.Packet;

public final class UnicastInterface {
	public static final int RECV_BUFFER_SIZE = 4096;
	
	private final DatagramSocket socket;
	private final int port;
	private final byte[] recvBuffer;
	private final UnicastCallbacks callbacks;
	private final SynchronizedQueue<DatagramPacket> packetQueue;
	private volatile boolean keepSending;
	
	public UnicastInterface(InetAddress localAddress, int port, UnicastCallbacks callbacks) {
		try {
			this.socket = new DatagramSocket(port);
			this.port = port;
			this.recvBuffer = new byte[RECV_BUFFER_SIZE];
			this.callbacks = callbacks;
			this.packetQueue = new SynchronizedQueue<DatagramPacket>();
		} catch (SocketException e) {
			throw new RuntimeException(String.format("Failed to create unicast interface: %s", e.getMessage()));
		}
	}
	
	public void start() {
		keepSending = true;
		
		(new ReceiveThread()).start();
		(new SendThread()).start();
	}
	
	public void close() {
		keepSending = false;
		
		socket.close();
	}

	public void send(InetAddress dest, Packet packet) {
		byte[] data = packet.serialize();
		DatagramPacket datagram = new DatagramPacket(data,  0, data.length, dest, port);
		
		packetQueue.enqueue(datagram);
		// For some reason the send call in the commented code below was causing huge stalls (several hundred milliseconds per call)
		// for the users running the live-USB. I didn't experience any issues on my own machine running Mint 17.1 natively. I'm not sure
		// if this is caused by the live-USB or distro used (or anything else for that matter). My best guess is that some kind of kernel
		// buffer (which the data would normally be copied in) is full and the call blocks until the queue is (partially) flushed. We could
		// somewhat work around this issue by trying to limit how much send calls we make, but as the stalls were causing massive delays in
		// our application leading to all sorts of issues we're pushing all send calls into our own queue, which is polled on another thread.
		// This way minor stalls won't be an issue. Big stalls are still problematic as the queue would develop a large backlog and the
		// effective network latency will be unacceptably large. It's also bound to cause issues in the TCP layer (due to retransmission timeouts).
		// This solution is a bit of a hack, but should work sufficiently for our purposes. We are short on time so we have to prioritize other
		// things, even if this isn't ideal sadly enough.
		
		/*try {
			socket.send(datagram);
		} catch (IOException e) {
			System.err.printf("IOException during DatagramSocket.send: %s%", e.getMessage());
		}*/
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
	
	class SendThread extends Thread {
		@Override
		public void run() {
			setName("Unicast-send");
			
			while(keepSending) {
				// Swap the buffers so we get a queue we can poll on.
				Queue<DatagramPacket> queue = packetQueue.swapBuffers();
				
				// Deplete the entire queue and send out the queued datagram packets.
				while(true) {
					DatagramPacket entry = queue.poll();
					
					if(entry == null) {
						// Queue depleted.
						break;
					}
					
					try {
						socket.send(entry);
					} catch (IOException e) {
						System.err.printf("IOException during DatagramSocket.send: %s", e.getMessage());
					}
				}
				
				// Short sleep to avoid spinning/swapping the queues when there is little to no data queued.
				try {
					Thread.sleep(5);
				} catch (InterruptedException e) { }
			}
		}
	}
}
