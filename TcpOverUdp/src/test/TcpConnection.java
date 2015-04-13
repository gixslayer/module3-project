package test;

import java.util.LinkedList;
import java.util.Queue;

enum State {
	Closed,
	SynSent,
	SynReceived,
	Established,
	FinWait,
	LastAck,
	TimeWait
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
		return System.currentTimeMillis() - lastSent >= 100;
	}
	
	public Packet getPacket() {
		return packet;
	}
}

public class TcpConnection {
	public static final int FLAG_NONE = 0;
	public static final int FLAG_SYN = 1;
	public static final int FLAG_ACK = 2;
	public static final int FLAG_FIN = 4;
	public static final int FLAG_DATA = 8;
	
	private final String name;
	private final NetworkInterface network;
	private State state;
	private volatile int currentSeq;
	private int currentAck;
	private volatile int nextSeq;
	private Queue<Packet> packetQueue;
	private Queue<RetransmissionTask> retransmissionQueue;
	private SendThread sendThread;
	
	public TcpConnection(String name, NetworkInterface network) {
		this.name = name;
		this.network = network;
		this.state = State.Closed;
		this.currentSeq = 0;
		this.currentAck = 0;
		this.nextSeq = 0;
		this.packetQueue = new LinkedList<Packet>();
		this.retransmissionQueue = new LinkedList<RetransmissionTask>();
	}
	
	public void connect() {
		state = State.SynSent;
		sendControl(currentSeq, 0, FLAG_SYN, true);
	}
	
	public void listen() {
		(new ReceiveThread()).start();
		(new RetransmissionThread()).start();
	}
	
	public void close() {
		state = State.FinWait;
		sendControl(currentSeq, currentAck, FLAG_FIN, true);
		//network.close();
	}
	
	public void send(Packet packet) {
		if(state == State.Closed) {
			connect();
		}
		
		synchronized (packetQueue) {
			packetQueue.add(packet);
		}
	}
	
	private void onDataReceived(byte[] data) {
		if(data.length == 1) {
			//close();
		} else {
			send(new Packet(new byte[data.length - 1]));
		}
		System.out.printf("%s Received data: %d%n", name, data.length);
	}
	
	private void sendControl(int seq, int ack, int flags, boolean scheduleRetransmission) {
		Packet packet = new Packet(flags, seq, ack);
		
		String flagStr = "";
		if((flags & FLAG_SYN) != 0) flagStr += "SYN ";
		if((flags & FLAG_ACK) != 0) flagStr += "ACK ";
		if((flags & FLAG_FIN) != 0) flagStr += "FIN ";
		if((flags & FLAG_DATA) != 0) flagStr += "DATA ";
		
		System.out.printf("%s Send -> seq=%-4d ack=%-4d flags=%-10s state=%s%n", name, seq, ack, flagStr, state);
		
		if(scheduleRetransmission) {
			scheduleRetransmission(packet);
		}
		
		network.send(packet);
	}
	
	private void sendPacket(Packet packet) {
		int flags = packet.getFlags();
		String flagStr = "";
		if((flags & FLAG_SYN) != 0) flagStr += "SYN ";
		if((flags & FLAG_ACK) != 0) flagStr += "ACK ";
		if((flags & FLAG_FIN) != 0) flagStr += "FIN ";
		if((flags & FLAG_DATA) != 0) flagStr += "DATA ";
		System.out.printf("%s Send -> seq=%-4d ack=%-4d flags=%-10s state=%s%n", name, packet.getSeq(), packet.getAck(), flagStr, state);
		
		network.send(packet);
	}
	
	private void onPacketReceived(Packet packet) {
		int flags = packet.getFlags();
		String flagStr = "";
		if((flags & FLAG_SYN) != 0) flagStr += "SYN ";
		if((flags & FLAG_ACK) != 0) flagStr += "ACK ";
		if((flags & FLAG_FIN) != 0) flagStr += "FIN ";
		if((flags & FLAG_DATA) != 0) flagStr += "DATA ";
		//System.out.printf("%s Recv -> seq=%-4d ack=%-4d flags=%-10s state=%s%n", name, packet.getSeq(), packet.getAck(), flagStr, state);
		
		switch (state) {
		case Closed:
			onPacketClosed(packet);
			break;
		case SynSent:
			onPacketSynSent(packet);
			break;
		case SynReceived:
			onPacketSynReceived(packet);
			break;
		case Established:
			onPacketEstablished(packet);
			break;
		case FinWait:
			onPacketFinWait(packet);
			break;
		case LastAck:
			onPacketLastAck(packet);
			break;
		case TimeWait:
			onPacketTimeWait(packet);
			break;
		}
	}
	
	private void onPacketClosed(Packet packet) {
		int flags = packet.getFlags();
		int seq = packet.getSeq();
		
		if(flags != FLAG_SYN) {
			return;
		}
		
		currentSeq = 10; // TODO: Temp hack to get a different ISN.
		currentAck = seq + 1;
		state = State.SynReceived;
		sendControl(currentSeq, currentAck, FLAG_SYN | FLAG_ACK, true);
	}
	
	private void onPacketSynSent(Packet packet) {
		int flags = packet.getFlags();
		int seq = packet.getSeq();
		int ack = packet.getAck();
		
		if(flags != (FLAG_SYN | FLAG_ACK)) {
			return;
		}
		if(ack - 1 != currentSeq) {
			return;
		}
		
		currentSeq = ack;
		nextSeq = ack + 1;
		currentAck = seq + 1;
		state = State.Established;
		cancelRetransmissionsUpTo(ack);

		Packet queuedPacket;
		synchronized(packetQueue) {
			 queuedPacket = packetQueue.remove();
		}

		queuedPacket.setSeq(currentSeq);
		queuedPacket.setAck(currentAck);
		queuedPacket.setFlags(FLAG_ACK | FLAG_DATA);
			
		scheduleRetransmission(queuedPacket);
		sendPacket(queuedPacket);
		
		// TODO: Figure out the right thing to do for this.
		(new SendThread()).start();
	}

	private void onPacketSynReceived(Packet packet) {
		int flags = packet.getFlags();
		int seq = packet.getSeq();
		int ack = packet.getAck();
		
		if(flags != (FLAG_ACK | FLAG_DATA)) {
			return;
		}
		if(seq != currentAck) {
			return;
		}
		
		currentSeq = ack;
		currentAck = seq + 1;
		nextSeq = ack;
		state = State.Established;
		cancelRetransmissionsUpTo(ack);
		sendControl(currentSeq, currentAck, FLAG_ACK, false);
		(new SendThread()).start();
		
		onDataReceived(packet.getData());
	}
	
	private void onPacketEstablished(Packet packet) {
		int flags = packet.getFlags();
		int seq = packet.getSeq();
		int ack = packet.getAck();
		boolean hasAckFlag = (flags & FLAG_ACK) == FLAG_ACK;
		boolean hasDataFlag = (flags & FLAG_DATA) == FLAG_DATA;
		
		if(flags == (FLAG_SYN | FLAG_ACK)) {
			return;
		}
		
		if(flags == FLAG_FIN) {
			state = State.LastAck;
			currentSeq = ack;
			currentAck = seq + 1;
			sendControl(currentSeq, currentAck, FLAG_FIN | FLAG_ACK, true);
			return;
		}
		
		if(seq != currentAck) {
			if(hasDataFlag) {
				//System.out.println("resending ack");
				sendControl(currentSeq, currentAck, FLAG_ACK, false);
			}
			
			//System.out.printf("dropped on established: ack=%d curSeq=%d%n", ack, currentSeq);
			// Already saw this packet.
			return;
		} else if(hasAckFlag && ack <= currentSeq) {
			//System.out.printf("Dropped old ack: ack=%d curSeq=%sd%n", ack, currentSeq);
			return;
		}
		
		currentAck = hasDataFlag ? seq + 1 : seq;
		
		if(hasAckFlag) {
			//System.out.printf("%s Updating curSeq to %d%n", name, ack);
			currentSeq = ack;
			cancelRetransmissionsUpTo(ack);
		}
		
		if(hasDataFlag) {
			sendControl(currentSeq, currentAck, FLAG_ACK, false);
			onDataReceived(packet.getData());
		}
	}
	
	private void onPacketFinWait(Packet packet) {
		int flags = packet.getFlags();
		int ack = packet.getAck();
		int seq = packet.getSeq();
		
		if(flags == (FLAG_FIN | FLAG_ACK)) {
			// Cancel all retransmission timers.
			
			state = State.TimeWait;
			currentSeq = ack;
			currentAck = seq + 1;
			sendControl(currentSeq, currentAck, FLAG_ACK, true);
			
			// Begin close timer.
		}
	}
	
	private void onPacketLastAck(Packet packet) {
		int flags = packet.getFlags();
		
		if(flags == FLAG_ACK) {
			state = State.Closed;
		}
	}
	
	private void onPacketTimeWait(Packet packet) {
		
	}
	
	private void scheduleRetransmission(Packet packet) {
		synchronized (retransmissionQueue) {
			retransmissionQueue.add(new RetransmissionTask(packet));
		}
	}
	
	private void cancelRetransmissionsUpTo(int ack) {
		synchronized(retransmissionQueue) {
			while(true) {
				RetransmissionTask task = retransmissionQueue.peek();
				
				if(task == null) {
					break; // Queue empty.
				}
				
				Packet packet = task.getPacket();
				
				if(packet.getSeq() < ack) {
					retransmissionQueue.remove();
				} else {
					// Work done. TODO: Wrapping stuff
					break;
				}
			}
		}
	}
	
	class RetransmissionThread extends Thread {
		@Override
		public void run() {
			while(true) {
				synchronized(retransmissionQueue) {
					for(RetransmissionTask task : retransmissionQueue) {
						if(task.requiresResend()) {
							task.updateLastSent();
							sendPacket(task.getPacket());
						}
					}
				}
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) { }
			}
		}
	}
	
	class SendThread extends Thread {
		@Override
		public void run() {
			while(true) {
				Packet packet;
				
				synchronized (packetQueue) {
					packet = packetQueue.poll();
				}
				
				if(packet == null) {
					continue;
				}
				
				//System.out.printf("%s Entering send stall for seq=%d%n", name, nextSeq);
				
				while(currentSeq != nextSeq) {
					
				}
				
				//System.out.printf("%s Sending packet from queue: seq=%d ack=%d%n", name, currentSeq, currentAck);
				
				packet.setSeq(currentSeq);
				packet.setAck(currentAck);
				packet.setFlags(FLAG_DATA);
				nextSeq += 1; // TODO: Figure this out.

				scheduleRetransmission(packet);				
				sendPacket(packet);
			}
		}
	}
	
	class ReceiveThread extends Thread {
		@Override
		public void run() {
			while(true) {
				Packet packet = network.recv();
				
				if(packet == null) {
					break;
				}
				
				onPacketReceived(packet);
			}
		}
	}
}
