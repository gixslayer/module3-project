package project;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import network.MulticastInterface;
import network.NetworkInterface;

public class TCP {
	private static InetAddress myInetAddress;
	private static int myAddress;
	private static Map<Integer, TCP.State> connections;
	private static Map<Integer, Timer> timers;
	private static Map<Integer, int[]> lastInfo;
	private static Map<Integer, ArrayList<byte[]>> toSend;
	private static boolean constructed = false;
	private static NetworkInterface ni;
	private static InetAddress destInetAddress;
	private static int destAddress;
	
	private static void init(int source, int dest) {
		if(!constructed) {
			try {
				myInetAddress = InetAddress.getByName("192.168.5." + source); //TODO: get myAdress from another static function
				myAddress = source;
				constructed = true;
				connections = new HashMap<>();
				toSend = new HashMap<>();
				timers = new HashMap<>();
				lastInfo = new HashMap<>();
				destInetAddress = InetAddress.getByName("192.168.5." + dest);
				destAddress = dest;
			} catch (UnknownHostException e) {
				e.printStackTrace();
			}
		}
	}
	
	public enum State {
		CLOSED, SYNSENT, SYN_RECEIVED, ESTABLISHED, FIN_WAIT, LAST_ACK, TIME_WAIT;
	}
	
	public static byte[] handlePacket(NetworkInterface ni, InetAddress myAddr, Packet packet) {
		init(inetToInt(myAddr), packet.getSource());
		TCP.ni = ni;
		if(packet.getDestination() == myAddress/* && checksumCheck(packet)*/) {
			System.out.println("From:" + packet.getSource() + ", seq: " + packet.getSeq() +", ack: " + packet.getAck());
			//save info
			lastInfo.put(packet.getSource(), new int[]{packet.getSeq(), packet.getAck()});
			
			if(packet.getSynFlag() && !packet.getAckFlag() && !packet.getFinFlag()) {
				if(!connections.containsKey(destAddress) || connections.get(destAddress).equals(State.CLOSED)) {
					sendSynAck(packet);
					connections.put(destAddress, State.SYN_RECEIVED);
				}
			} else if(packet.getSynFlag() && packet.getAckFlag() && !packet.getFinFlag()) {
				if(connections.containsKey(destAddress) && connections.get(destAddress).equals(State.SYNSENT)) {
					sendAckForSynAck(packet);
					connections.put(destAddress, State.ESTABLISHED);
					ArrayList<byte[]> array = toSend.get(destAddress);
					for(byte[] a: array){
						sendData(inetToInt(myAddr), destAddress, a);
					}
					toSend.remove(destAddress);
				}
			} else if(packet.getFinFlag() && !packet.getAckFlag()) {
				if(connections.containsKey(destAddress) && connections.get(destAddress).equals(State.ESTABLISHED)) {
					sendFinAck(packet);
					connections.put(destAddress, State.LAST_ACK);
				}
			} else if(packet.getFinFlag() && packet.getAckFlag()) {
				if(connections.containsKey(destAddress) && connections.get(destAddress).equals(State.FIN_WAIT)) {
					sendAckForFinAck(packet);
					connections.put(destAddress, State.TIME_WAIT);
					Timer timer = new Timer();
					if(timers.containsKey(destAddress)) {
						timers.get(destAddress).cancel();
					}
					timers.put(destAddress, timer);
					timer.schedule(new TimeOutTask(destAddress), 10000);
				}
			} else if(!packet.getSynFlag() && packet.getAckFlag() && !packet.getFinFlag()) {
				if(connections.containsKey(destAddress) && connections.get(destAddress).equals(State.SYN_RECEIVED)) {
					connections.put(destAddress, State.ESTABLISHED);
					ArrayList<byte[]> array = toSend.get(destAddress);
					for(byte[] a: array){
						sendData(inetToInt(myAddr), destAddress, a);
					}
					toSend.remove(destAddress);
				} else if(connections.containsKey(destAddress) && connections.get(destAddress).equals(State.LAST_ACK)) {
					connections.put(destAddress, State.CLOSED);
				} else if(connections.containsKey(destAddress) && connections.get(destAddress).equals(State.ESTABLISHED)) {
					if(packet.getLength() == 0) {
						//no data, normal ACK
						//TODO check seqs and acks.
					} else {
						sendAck(packet);
						return packet.getData();
					}
				}
			}
		}
		return null;
	}
	
	private static boolean checksumCheck(Packet packet) {
		return packet.getCheckSum() == packet.calculateChecksum(packet.getData());
	}

	public static void timeOut(int destination) {
		if(connections.containsKey(destination) && connections.get(destination).equals(State.TIME_WAIT)) {
			connections.put(destination, State.CLOSED);
			timers.remove(destination);
		}
	}
	
	private static int inetToInt(InetAddress destination) {
		return destination.getAddress()[3];
	}
	
	public static boolean openConnection(int destination) {
		if(!connections.containsKey(destination) || connections.get(destination).equals(State.CLOSED)) {
			connections.put(destination, State.SYNSENT);
			sendSyn(destination);
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean openConnection(InetAddress destination) {
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
		
		if(connections.containsKey(destination) && (connections.get(destination).equals(State.ESTABLISHED) || connections.get(destination).equals(State.SYN_RECEIVED))) {
			sendFin(destination);
			connections.put(destination, State.FIN_WAIT);
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean closeConnection(InetAddress destination) {
		int dest = Integer.parseInt(""+destination.getHostAddress().charAt(10));
		if(connections.containsKey(dest) && (connections.get(dest).equals(State.ESTABLISHED) || connections.get(dest).equals(State.SYN_RECEIVED))) {
			sendFin(dest);
			connections.put(dest, State.FIN_WAIT);
			return true;
		} else {
			return false;
		}
	}
	
	private static void sendSyn(int destination) {
		Packet syn = new Packet(myAddress, destination, 0, 0, 0, true, false, false, 5, new byte[0]);
		System.out.println(syn.getDestination() + ", seq: " + syn.getSeq() +", ack: " + syn.getAck());
		
		sendPacket(syn);
	}
	
	private static void sendSyn(InetAddress destination) {
		sendSyn(Integer.parseInt(""+destination.getHostAddress().charAt(10)));
	}
	
	private static void sendSynAck(Packet lastPacket) {
		Packet synAck = new Packet(myAddress, lastPacket.getSource(), 0, 0, 1, true, true, false, 5, new byte[0]);
		System.out.println(synAck.getDestination() + ", seq: " + synAck.getSeq() +", ack: " + synAck.getAck());
		
		sendPacket(synAck);
	}
	
	private static void sendAckForSynAck(Packet lastPacket) {
		int newSeq = 1;
		int newAck = 1;
		Packet ack = new Packet(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, false, 5, new byte[0]);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		
		sendPacket(ack);

	}
	
	private static void sendAckForFinAck(Packet lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+1;
		Packet ack = new Packet(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, false, 5, new byte[0]);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		sendPacket(ack);
	}
	
	private static void sendAck(Packet lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+lastPacket.getLength();
		Packet ack = new Packet(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, false, 5, new byte[0]);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		//add to info
		lastInfo.put(lastPacket.getSource(), new int[]{newSeq, newAck});
		sendPacket(ack);
	}
	
	private static void sendFin(int destination) {
		int newSeq = lastInfo.get(destination)[1];
		int newAck = lastInfo.get(destination)[0];
		Packet fin = new Packet(myAddress, destination, 0, newSeq, newAck, false, false, true, 5, new byte[0]);
		System.out.println(fin.getDestination() + ", seq: " + fin.getSeq() +", ack: " + fin.getAck());
		sendPacket(fin);
	}
	
	private static void sendFinAck(Packet lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+1;
		Packet ack = new Packet(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, true, 5, new byte[0]);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		sendPacket(ack);
	}
	
	public static void sendData(int source, int destination, byte[] data) {
		init(source, destination);
		if(connections.containsKey(destination) && connections.get(destination).equals(State.ESTABLISHED)) {
			Packet toSend = new Packet(myAddress, destination, 0, lastInfo.get(destination)[0], lastInfo.get(destination)[1], false, true, false, 5, data);
			lastInfo.put(destination, new int[]{toSend.getSeq()+toSend.getLength(),toSend.getAck()});
			System.out.println(toSend.getDestination() + ", seq: " + toSend.getSeq() +", ack: " + toSend.getAck());
			sendPacket(toSend);
		} else if(!connections.containsKey(destination) || connections.get(destination).equals(State.CLOSED)){
			openConnection(destination);
			ArrayList<byte[]> array = toSend.get(destination);
			if(array == null) array = new ArrayList<byte[]>();
			array.add(data);
			toSend.put(destination, array);
		} else {
			ArrayList<byte[]> array = toSend.get(destination);
			array.add(data);
			toSend.put(destination, array);
		}
	}
	
	public static void sendData(NetworkInterface ni, InetAddress source, InetAddress destination, byte[] data) {
		TCP.ni = ni;
		sendData(inetToInt(source), inetToInt(destination), data);
	}
	
	public static void sendPacket(Packet packet) {
		ni.send(destInetAddress, packet.getBytes());
	}
}
