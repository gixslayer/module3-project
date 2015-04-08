package project;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.TimerTask;
import java.util.Timer;

public class TCP {
	private static InetAddress myInetAddress;
	private static int myAddress;
	private static Map<Integer, TCP.State> connections;
	private static Map<Integer, Timer> timers;
	private static Map<Integer, int[]> lastInfo;
	private static boolean constructed = false;
	
	private static void init() {
		if(!constructed) {
			try {
				myInetAddress = InetAddress.getByName("192.168.5.3"); //TODO: get myAdress from another static function
				myAddress = Integer.parseInt(""+myInetAddress.getHostAddress().charAt(10));
				constructed = true;
				connections = new HashMap<>();
				timers = new HashMap<>();
				lastInfo = new HashMap<>();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public enum State {
		CLOSED, SYNSENT, SYN_RECEIVED, ESTABLISHED, FIN_WAIT, LAST_ACK, TIME_WAIT;
	}
	
	public static void handlePacket(Packet packet) {
		init();
		if(packet.getDestination() == myAddress) {
			
			//save info
			lastInfo.put(packet.getSource(), new int[]{packet.getSeq(), packet.getAck()});
			
			int destination = packet.getSource();
			if(packet.getSynFlag() && !packet.getAckFlag() && !packet.getFinFlag()) {
				if(!connections.containsKey(destination) || connections.get(destination).equals(State.CLOSED)) {
					sendSynAck(packet);
					connections.put(destination, State.SYN_RECEIVED);
				}
			} else if(packet.getSynFlag() && packet.getAckFlag() && !packet.getFinFlag()) {
				if(connections.containsKey(destination) && connections.get(destination).equals(State.SYNSENT)) {
					sendAckForSynAck(packet);
					connections.put(destination, State.ESTABLISHED);
				}
			} else if(packet.getFinFlag() && !packet.getAckFlag()) {
				if(connections.containsKey(destination) && connections.get(destination).equals(State.ESTABLISHED)) {
					sendFinAck(packet);
					connections.put(destination, State.LAST_ACK);
				}
			} else if(packet.getFinFlag() && packet.getAckFlag()) {
				if(connections.containsKey(destination) && connections.get(destination).equals(State.FIN_WAIT)) {
					sendAckForFinAck(packet);
					connections.put(destination, State.TIME_WAIT);
					Timer timer = new Timer();
					if(timers.containsKey(destination)) {
						timers.get(destination).cancel();
					}
					timers.put(destination, timer);
					timer.schedule(new TimeOutTask(destination), 10000);
				}
			} else if(!packet.getSynFlag() && packet.getAckFlag() && !packet.getFinFlag()) {
				if(connections.containsKey(destination) && connections.get(destination).equals(State.SYN_RECEIVED)) {
					connections.put(destination, State.ESTABLISHED);
				} else if(connections.containsKey(destination) && connections.get(destination).equals(State.LAST_ACK)) {
					connections.put(destination, State.CLOSED);
				} else if(connections.containsKey(destination) && connections.get(destination).equals(State.ESTABLISHED)) {
					if(packet.getLength() == 0) {
						//no data, normal ACK
						//TODO check seqs and acks.
					} else {
						//ACK with data
						sendAck(packet);
					}
				}
			}
		}
	}
	
	public static void timeOut(int destination) {
		if(connections.containsKey(destination) && connections.get(destination).equals(State.TIME_WAIT)) {
			connections.put(destination, State.CLOSED);
			timers.remove(destination);
		}
	}
	
	private static int inetToInt(InetAddress destination) {
		return Integer.parseInt(""+destination.getHostAddress().charAt(10));
	}
	
	public static boolean openConnection(int destination) {
		init();
		if(!connections.containsKey(destination) || connections.get(destination).equals(State.CLOSED)) {
			connections.put(destination, State.SYNSENT);
			sendSyn(destination);
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean openConnection(InetAddress destination) {
		init();
		int dest = Integer.parseInt(""+destination.getHostAddress().charAt(10));
		if(!connections.containsKey(dest) || connections.get(dest).equals(State.CLOSED)) {
			connections.put(dest, State.SYNSENT);
			sendSyn(dest);
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean closeConnection(int destination) {
		//TODO: change state
		if(connections.containsKey(destination) && (connections.get(destination).equals(State.ESTABLISHED) || connections.get(destination).equals(State.SYN_RECEIVED))) {
			sendFin(destination);
			connections.put(destination, State.FIN_WAIT);
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean closeConnection(InetAddress destination) {
		//TODO: change state
		int dest = Integer.parseInt(""+destination.getHostAddress().charAt(10));
		if(connections.containsKey(dest) && (connections.get(dest).equals(State.ESTABLISHED) || connections.get(dest).equals(State.SYN_RECEIVED))) {
			sendFin(dest);
			connections.put(dest, State.FIN_WAIT);
			return true;
		} else {
			return false;
		}
	}
	
	public static void main(String args[]) {
		System.out.println(System.currentTimeMillis()/1000);
	}
	
	private static void sendSyn(int destination) {
		Packet syn = new Packet(myAddress, destination, 0, 0, 0, true, false, false, 5, new byte[0]);
		//TODO: send packet
	}
	
	private static void sendSyn(InetAddress destination) {
		sendSyn(Integer.parseInt(""+destination.getHostAddress().charAt(10)));
	}
	
	private static void sendSynAck(Packet lastPacket) {
		Packet synAck = new Packet(myAddress, lastPacket.getSource(), 0, 0, 1, true, true, false, 5, new byte[0]);
		//TODO: send packet, ACK en SEQ goed doen
	}
	
	private static void sendAckForSynAck(Packet lastPacket) {
		int newSeq = 1;
		int newAck = 1;
		Packet ack = new Packet(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, false, 5, new byte[0]);
		//TODO: send packet, ACK en SEQ goed doen

	}
	
	private static void sendAckForFinAck(Packet lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+1;
		Packet ack = new Packet(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, false, 5, new byte[0]);
		//TODO: send packet, ACK en SEQ goed doen
	}
	
	private static void sendAck(Packet lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+lastPacket.getLength();
		Packet ack = new Packet(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, false, 5, new byte[0]);
		//add to info
		lastInfo.put(lastPacket.getSource(), new int[]{newSeq, newAck});
		//TODO: send packet, ACK en SEQ goed doen
	}
	
	private static void sendFin(int destination) {
		int newSeq = lastInfo.get(destination)[1];
		int newAck = lastInfo.get(destination)[0];
		Packet fin = new Packet(myAddress, destination, 0, newSeq, newAck, false, false, true, 5, new byte[0]);
		//TODO: send packet, ACK en SEQ goed doen
	}
	
	private static void sendFinAck(Packet lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+1;
		Packet ack = new Packet(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, true, 5, new byte[0]);
		//TODO: send packet, ACK en SEQ goed doen
	}
	
	private static void sendData(int destination, byte[] data) {
		if(connections.get(destination).equals(State.ESTABLISHED)) {
			Packet toSend = new Packet(myAddress, destination, 0, lastInfo.get(destination)[0], lastInfo.get(destination)[1], false, true, false, 5, data);
			//TODO send packet
		}
	}
	
	private static void sendData(InetAddress destination, byte[] data) {
		sendData(inetToInt(destination), data);
	}
}
