package network;

import java.net.InetAddress;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Queue;

import protocol.EmptyPacket;
import protocol.Packet;
import protocol.PacketHeader;

enum State {
	Closed,
	SynSent,
	SynReceived,
	Established
}

class RetransmissionTask {
	private final Packet packet;
	private long lastSent;
	
	public RetransmissionTask(Packet packet) {
		this.packet = packet;
		this.lastSent = System.currentTimeMillis();
	}
	
	public void updateLastSent() {
		lastSent = System.currentTimeMillis();
	}
	
	public boolean requiresResend() {
		return System.currentTimeMillis() - lastSent >= TcpConnection.RETRANSMISSION_TIMEOUT;
	}
	
	public Packet getPacket() {
		return packet;
	}
}

class SequenceNumber {
	// Could really be anything, but don't go super low (to prevent ISN predictability and wrapping issues).
	public static final int MAX = Integer.MAX_VALUE; 
	
	private int value;
	
	public SequenceNumber(int value) {
		this.value = value;
	}
	
	public void increment() {
		if(value == MAX) {
			value = 0;
		} else {
			value++;
		}
	}
	
	public void incrementFrom(int value) {
		if(value == MAX) {
			this.value = 0;
		} else {
			this.value = value + 1;
		}
	}

	public boolean isAfter(int value) {
		// Assumes this.value and value are reasonably 'close' (either a small difference, or a near MAX difference).
		if(this.value == value) {
			return false;
		}
		
		int difference = this.value - value;
		difference = difference < 0 ? -difference : difference; // Simple integer abs.
		
		if(difference > MAX / 2) {
			// Possibly very messy, but now assumed one point is just before the wrap point and the other is just over.
			
			// If we are before the wrap point we cannot be after the other point.
			return this.value >= MAX / 2;
		} else {
			// No need to account for wrapped, direct comparison possible.
			return this.value > value;
		}
	}
	
	public boolean equals(int value) {
		return this.value == value;
	}
	
	public boolean isAfterOrEqual(int value) {
		return equals(value) || isAfter(value);
	}
	
	public int getValue() {
		return value;
	}
}

public final class TcpConnection {
	public static final int MAX_RETRANSMISSION_QUEUE_SIZE = 10;
	public static final int RETRANSMISSION_TIMEOUT = 100;
	public static final boolean DEBUG_PRINTS = false;
	private static final int FLAG_SYN = 1;
	private static final int FLAG_ACK = 2;
	private static final int FLAG_DATA = 4;
	
	private final UnicastInterface unicastInterface;
	private final InetAddress remoteAddress;
	private final TcpCallbacks callbacks;
	private final Queue<RetransmissionTask> retransmissionQueue;
	private final PriorityQueue<Packet> packetQueue;
	private State state;
	private SequenceNumber localSeq;
	private SequenceNumber localAck;

	public TcpConnection(UnicastInterface unicastInterface, InetAddress remoteAddress, TcpCallbacks callbacks) {
		this.unicastInterface = unicastInterface;
		this.remoteAddress = remoteAddress;
		this.callbacks = callbacks;
		this.retransmissionQueue = new LinkedList<RetransmissionTask>();
		this.packetQueue = new PriorityQueue<Packet>();
		this.state = State.Closed;
		 // TODO: Random value in [0, SEQ_MAX] range once confirmed to work. Should probably leave some room and stay a bit under SEQ_MAX to start out.
		this.localSeq = new SequenceNumber(0);
		this.localAck = new SequenceNumber(0);
	}
	
	public void queuePacket(Packet packet, Priority priority) {
		packetQueue.add(packet, priority);
		
		if(state == State.Closed) {
			connect();
		}
	}

	public void process() {
		processRetransmissionQueue();
		processPacketQueue();
	}
	
	public void onPacketReceived(Packet packet) {
		if(DEBUG_PRINTS) {
			int seq = packet.getHeader().getSeq();
			int ack = packet.getHeader().getAck();
			int flags = packet.getHeader().getFlags();
			System.out.printf("TCP IN   [%s] seq=%-11d ack=%-11d flags=%s%n", remoteAddress.getHostAddress(), seq, ack, flagsToString(flags));
		}
		
		if(state == State.Closed) {
			packetReceivedOnClosed(packet);
		} else if(state == State.SynReceived) {
			packetReceivedOnSynReceived(packet);
		} else if(state == State.SynSent) {
			packetReceivedOnSynSent(packet);
		} else if(state == State.Established) {
			packetReceivedOnEstablished(packet);
		}
	}
	
	private void packetReceivedOnClosed(Packet packet) {
		PacketHeader header = packet.getHeader();
		int flags = header.getFlags();
		int seq = header.getSeq();
		
		if(flags != FLAG_SYN) {
			return;
		}
		
		localAck.incrementFrom(seq);
			
		sendControl(FLAG_SYN | FLAG_ACK, true);
		state = State.SynReceived;
	}
	
	private void packetReceivedOnSynSent(Packet packet) {
		PacketHeader header = packet.getHeader();
		int flags = header.getFlags();
		int seq = header.getSeq();
		int ack = header.getAck();
		
		if(flags != (FLAG_SYN | FLAG_ACK)) {
			return;
		} else if(!localSeq.equals(ack)) {
			return;
		}
		
		localAck.incrementFrom(seq);
		state = State.Established;
		cancelRetransmissionsUpTo(ack);
		
		// As we are the 'connecting' side we are called when at least one packet is available in the queue.
		// Use that packet to carry the ack.
		Packet replyPacket = packetQueue.poll();
		sendPacket(FLAG_ACK | FLAG_DATA, replyPacket);
	}
	
	private void packetReceivedOnSynReceived(Packet packet) {
		PacketHeader header = packet.getHeader();
		int flags = header.getFlags();
		int seq = header.getSeq();
		int ack = header.getAck();
		
		if(flags != (FLAG_ACK | FLAG_DATA)) {
			return;
		} else if(!localAck.equals(seq)) {
			return;
		}
		
		localAck.incrementFrom(seq);
		state = State.Established;
		cancelRetransmissionsUpTo(ack);
		
		sendControl(FLAG_ACK, false);
		
		callbacks.onTcpPacketReceived(packet);
	}
	
	private void packetReceivedOnEstablished(Packet packet) {
		PacketHeader header = packet.getHeader();
		int flags = header.getFlags();
		int seq = header.getSeq();
		int ack = header.getAck();
		boolean ackFlag = header.hasFlags(FLAG_ACK);
		boolean dataFlag = header.hasFlags(FLAG_DATA);
		
		if(localAck.isAfter(seq)) {
			// Already saw the packet so drop it.
			
			if(flags != FLAG_ACK) {
				// If its not a pure ack (such as ACK|DATA / DATA) send an ack back to confirm the arrival as our previous ack to
				// this packet might have been lost and we triggered a retransmission timeout on the other client.
				sendControl(FLAG_ACK, false);
			}
			
			return;
		}
		
		if(ackFlag) {
			// If we receive a packet with an ack flag we know everything up to that number (not including) has been confirmed
			// to have arrived. Cancel retransmission timers for the packets confirmed to have arrived.
			cancelRetransmissionsUpTo(ack);
		}
		
		// If the packet carries a data flag make sure its the next packet we expected. We already ensured its not an 'old' packet
		// we already saw, but this might be a 'newer' packet than we expect (as the actual packet we expected perhaps got dropped 
		// somewhere along the way).
		if(dataFlag && localAck.equals(seq)) {
			localAck.incrementFrom(seq);
			
			callbacks.onTcpPacketReceived(packet);
			
			// TODO: We could implement piggy-backing here by checking if we have a queued packet and the size of the retransmission
			// queue is acceptable. If yes we could send an ACK|DATA using that packet instead of a pure ack like this. 
			sendControl(FLAG_ACK, false);
		}
	}
	
	private void processRetransmissionQueue() {
		for(RetransmissionTask task : retransmissionQueue) {
			if(task.requiresResend()) {
				task.updateLastSent();
				send(task.getPacket());
			}
		}
	}
	
	private void processPacketQueue() {
		if(state != State.Established) {
			// Only begin processing the packet queue once the connection is established.
			return;
		}
		
		while(retransmissionQueue.size() < MAX_RETRANSMISSION_QUEUE_SIZE) {
			Packet packet = packetQueue.poll();
			
			if(packet == null) {
				// Queue depleted, break out of loop.
				break;
			}
			
			// Send the next packet with a data flag, this will also add it to the retransmission queue.
			sendPacket(FLAG_DATA, packet);
		}
	}
	
	private void cancelRetransmissionsUpTo(int ack) {
		SequenceNumber ackedSeq = new SequenceNumber(ack);
		Iterator<RetransmissionTask> it = retransmissionQueue.iterator();
		while(it.hasNext()) {
			RetransmissionTask task = it.next();
			int seq = task.getPacket().getHeader().getSeq();
			
			if(ackedSeq.isAfter(seq)) {
				if(DEBUG_PRINTS) {
					System.out.printf("TCP INFO [%s] removed seq=%-11d from retransmission queue (recv ack=%d)%n", remoteAddress.getHostAddress(), seq, ack);
				}
				
				it.remove();
			}
		}
	}
	
	private void connect() {
		sendControl(FLAG_SYN, true);
		state = State.SynSent;
	}
	
	private void sendPacket(int flags, Packet packet) {
		PacketHeader header = new PacketHeader(localSeq.getValue(), localAck.getValue(), flags);
		packet.setHeader(header);
		
		retransmissionQueue.add(new RetransmissionTask(packet));
		localSeq.increment();

		send(packet);
	}
	
	private void sendControl(int flags, boolean scheduleRetransmission) {
		EmptyPacket packet = new EmptyPacket();
		PacketHeader header = new PacketHeader(localSeq.getValue(), localAck.getValue(), flags);
		packet.setHeader(header);
		
		if(scheduleRetransmission) {
			retransmissionQueue.add(new RetransmissionTask(packet));
		}
		
		if(flags != FLAG_ACK) {
			// If its not a pure ack increment the local sequence number.
			localSeq.increment();
		}
		
		send(packet);
	}
	
	private void send(Packet packet) {
		if(DEBUG_PRINTS) {
			int seq = packet.getHeader().getSeq();
			int ack = packet.getHeader().getAck();
			int flags = packet.getHeader().getFlags();
			System.out.printf("TCP OUT  [%s] seq=%-11d ack=%-11d flags=%s%n", remoteAddress.getHostAddress(), seq, ack, flagsToString(flags));
		}
		
		unicastInterface.send(remoteAddress, packet);
	}
	
	private static String flagsToString(int flags) {
		// The string processing in this method is awful, but only used to debug TCP.
		String result = "";
		
		if((flags & FLAG_SYN) != 0) result += "SYN ";
		if((flags & FLAG_ACK) != 0) result += "ACK ";
		if((flags & FLAG_DATA) != 0) result += "DATA ";
		
		return result;
	}
}
