package project;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import protocol.PacketHeader;

import network.NetworkInterface;

public class TCP {
	private static int myAddress;
	private static Map<Integer, TCP.State> connections;
	private static Map<Integer, Timer> timers;
	private static Map<Integer, int[]> lastInfo;
	private static Map<Integer, ArrayList<byte[]>> toSend;
	private static Map<Integer, ArrayList<PacketHeader>> packetsInBuffer;
	private static Map<PacketHeader, Timer> timerOfPacket;
	private static boolean constructed = false;
	private static NetworkInterface ni;
	
	private static void init(int source, int dest) {
		if(!constructed) {
			myAddress = source;
			constructed = true;
			connections = new HashMap<>();
			toSend = new HashMap<>();
			timers = new HashMap<>();
			lastInfo = new HashMap<>();
			packetsInBuffer = new HashMap<>();
			timerOfPacket = new HashMap<>();
		}
	}
	
	public enum State {
		CLOSED, SYNSENT, SYN_RECEIVED, ESTABLISHED, FIN_WAIT, LAST_ACK, TIME_WAIT;
	}
	
	public static void handlePacket(NetworkInterface ni, InetAddress myAddr, PacketHeader packet) {
		init(inetToInt(myAddr), packet.getSource());
		int destAddress = packet.getSource();
		TCP.ni = ni;
		if(packet.getDestination() == myAddress) {
			System.out.println("From:" + packet.getSource() + ", seq: " + packet.getSeq() +", ack: " + packet.getAck());
			if(packetsInBuffer.get(destAddress) != null) {
				System.out.println("SEQACK INFO FROM: " + destAddress + " :" + packetsInBuffer.get(destAddress).size());
			}
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
					if(array == null) array = new ArrayList<byte[]>();
					for(byte[] a: array){
						sendData(inetToInt(myAddr), destAddress, a);
					}
					toSend.remove(destAddress);
					
					ackReceivedDuringHandshake(packet);
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
					ackReceived(packet);
				}
			} else if(!packet.getSynFlag() && packet.getAckFlag() && !packet.getFinFlag()) {
				if(connections.containsKey(destAddress) && connections.get(destAddress).equals(State.SYN_RECEIVED)) {
					connections.put(destAddress, State.ESTABLISHED);
					ArrayList<byte[]> array = toSend.get(destAddress);
					if(array == null) array = new ArrayList<byte[]>();
					for(byte[] a: array){
						sendData(inetToInt(myAddr), destAddress, a);
					}
					toSend.remove(destAddress);
					ackReceivedDuringHandshake(packet);
				} else if(connections.containsKey(destAddress) && connections.get(destAddress).equals(State.LAST_ACK)) {
					connections.put(destAddress, State.CLOSED);
					ackReceived(packet);
				} else if(connections.containsKey(destAddress) && connections.get(destAddress).equals(State.ESTABLISHED)) {
					if(packet.getLength() == 0) {
						//no data, normal ACK
						ackReceived(packet);
						//TODO check seqs and acks.
					} else {
						sendAck(packet);
					}
				}
			}
		}
	}
	
	private static void ackReceivedDuringHandshake(PacketHeader packet) {
		ArrayList<PacketHeader> buffer = packetsInBuffer.get(packet.getSource());
		PacketHeader toDelete = null;
		
		for(PacketHeader p: buffer) {
			if(p.getSeq()+1 == packet.getAck()) {
				//This is packet to remove
				toDelete = p;
			}
		}
		
		
		if(toDelete != null) {
			timerOfPacket.get(toDelete).cancel();
			timerOfPacket.remove(toDelete);
			buffer.remove(toDelete);
			packetsInBuffer.put(packet.getSource(), buffer);
		}
	}
	
	private static void ackReceived(PacketHeader packet) {
		ArrayList<PacketHeader> buffer = packetsInBuffer.get(packet.getSource());
		PacketHeader toDelete = null;
		System.out.println("ack RECEIVED!!! " + buffer.size());
				
		for(PacketHeader p: buffer) {
			if(p.getSeq()+p.getLength() == packet.getAck()) {
				//This is packet to remove
				toDelete = p;
				break;
			}
		}
		
		
		if(toDelete != null) {
			timerOfPacket.get(toDelete).cancel();
			timerOfPacket.remove(toDelete);
			System.out.println("DELETING PACKET");
			buffer.remove(toDelete);
			packetsInBuffer.put(packet.getSource(), buffer);
		}
	}

	public static void timeOut(int destination) {
		if(connections.containsKey(destination) && connections.get(destination).equals(State.TIME_WAIT)) {
			connections.put(destination, State.CLOSED);
			timers.remove(destination);
		}
	}
	
	public static void ackTimeOut(PacketHeader packet) {
		Timer timer = new Timer();
		timer.schedule(new AckTimeOut(packet), 100);
		timerOfPacket.put(packet, timer);
		sendPacket(packet, packet.getDestination());
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
		connections.put(destination, State.CLOSED);
		for(Timer i: timers.values()){
			i.cancel();
		}
		timers.remove(destination);
		lastInfo.remove(destination);
		toSend.remove(destination);
		return true;
	}
	
	public static boolean closeConnection(InetAddress destination) {
		int dest = Integer.parseInt(""+destination.getHostAddress().charAt(10));
		return closeConnection(dest);
	}
	
	public static void stopConnections(){
		if(constructed) {
			connections.clear();
			for(Timer i: timers.values()){
				i.cancel();
			}
			timers.clear();
			lastInfo.clear();
			toSend.clear();
		}
	}
	
	private static void sendSyn(int destination) {
		PacketHeader syn = new PacketHeader(myAddress, destination, 0, 0, 0, true, false, false, 5, 0);
		System.out.println(syn.getDestination() + ", seq: " + syn.getSeq() +", ack: " + syn.getAck());
		ArrayList<PacketHeader> temp = packetsInBuffer.get(destination);
		if(temp == null) {
			temp = new ArrayList<>();
		}
		
		temp.add(syn);
		packetsInBuffer.put(destination, temp);
		Timer timer = new Timer();
		timer.schedule(new AckTimeOut(syn), 100);
		timerOfPacket.put(syn, timer);
		
		sendPacket(syn, destination);
	}
	
	private static void sendSyn(InetAddress destination) {
		sendSyn(Integer.parseInt(""+destination.getHostAddress().charAt(10)));
	}
	
	private static void sendSynAck(PacketHeader lastPacket) {
		PacketHeader synAck = new PacketHeader(myAddress, lastPacket.getSource(), 0, 0, 1, true, true, false, 5, 0);
		System.out.println(synAck.getDestination() + ", seq: " + synAck.getSeq() +", ack: " + synAck.getAck());
		
		ArrayList<PacketHeader> temp = packetsInBuffer.get(synAck.getDestination());
		if(temp == null) {
			temp = new ArrayList<>();
		}
		
		lastInfo.put(lastPacket.getSource(), new int[]{synAck.getSeq(), synAck.getAck()});
		
		temp.add(synAck);
		packetsInBuffer.put(synAck.getDestination(), temp);
		Timer timer = new Timer();
		timer.schedule(new AckTimeOut(synAck), 100);
		timerOfPacket.put(synAck, timer);
		
		sendPacket(synAck, synAck.getDestination());
	}
	
	private static void sendAckForSynAck(PacketHeader packet) {
		int newSeq = 1;
		int newAck = 1;
		PacketHeader ack = new PacketHeader(myAddress, packet.getSource(), 0, newSeq, newAck, false, true, false, 5, 0);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		lastInfo.put(packet.getSource(), new int[]{newSeq, newAck});
		sendPacket(ack, ack.getDestination());

	}
	
	private static void sendAckForFinAck(PacketHeader lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+1;
		PacketHeader ack = new PacketHeader(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, false, 5, 0);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		sendPacket(ack, ack.getDestination());
	}
	
	private static void sendAck(PacketHeader lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+lastPacket.getLength();
		PacketHeader ack = new PacketHeader(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, false, 5, 0);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		//add to info
		lastInfo.put(lastPacket.getSource(), new int[]{newSeq, newAck});
		sendPacket(ack, ack.getDestination());
	}
	
	private static void sendFin(int destination) {
		int newSeq = lastInfo.get(destination)[1];
		int newAck = lastInfo.get(destination)[0];
		PacketHeader fin = new PacketHeader(myAddress, destination, 0, newSeq, newAck, false, false, true, 5, 0);
		System.out.println(fin.getDestination() + ", seq: " + fin.getSeq() +", ack: " + fin.getAck());
		ArrayList<PacketHeader> temp = packetsInBuffer.get(destination);
		if(temp == null) {
			temp = new ArrayList<>();
		}
		
		temp.add(fin);
		packetsInBuffer.put(destination, temp);
		Timer timer = new Timer();
		timer.schedule(new AckTimeOut(fin), 100);
		timerOfPacket.put(fin, timer);
		sendPacket(fin, destination);
	}
	
	private static void sendFinAck(PacketHeader lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+1;
		PacketHeader ack = new PacketHeader(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, true, 5, 0);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		ArrayList<PacketHeader> temp = packetsInBuffer.get(ack.getDestination());
		if(temp == null) {
			temp = new ArrayList<>();
		}
		
		temp.add(ack);
		packetsInBuffer.put(ack.getDestination(), temp);
		Timer timer = new Timer();
		timer.schedule(new AckTimeOut(ack), 100);
		timerOfPacket.put(ack, timer);
		sendPacket(ack, ack.getDestination());
	}
	
	public static void sendData(int source, int destination, byte[] data) {
		init(source, destination);
		if(connections.containsKey(destination) && connections.get(destination).equals(State.ESTABLISHED)) {
			PacketHeader toSend = new PacketHeader(myAddress, destination, 0, lastInfo.get(destination)[0], lastInfo.get(destination)[1], false, true, false, 5, data.length);
			lastInfo.put(destination, new int[]{toSend.getSeq()+toSend.getLength(),toSend.getAck()});
			System.out.println(toSend.getDestination() + ", seq: " + toSend.getSeq() +", ack: " + toSend.getAck());
			ArrayList<PacketHeader> temp = packetsInBuffer.get(destination);
			if(temp == null) {
				temp = new ArrayList<>();
			}
			
			temp.add(toSend);
			packetsInBuffer.put(destination, temp);
			Timer timer = new Timer();
			timer.schedule(new AckTimeOut(toSend), 100);
			timerOfPacket.put(toSend, timer);
			sendPacket(toSend, destination);
		} else if(!connections.containsKey(destination) || connections.get(destination).equals(State.CLOSED)){
			openConnection(destination);
			ArrayList<byte[]> array = toSend.get(destination);
			if(array == null) array = new ArrayList<byte[]>();
			array.add(data);
			toSend.put(destination, array);
		} else {
			ArrayList<byte[]> array = toSend.get(destination);
			if(array == null) array = new ArrayList<byte[]>();
			array.add(data);
			toSend.put(destination, array);
		}
	}
	
	public static void sendData(NetworkInterface ni, InetAddress source, InetAddress destination, byte[] data) {
		TCP.ni = ni;
		sendData(inetToInt(source), inetToInt(destination), data);
	}
	
	public static void sendPacket(PacketHeader packet, int destination) {
		System.out.println("I AM ACKING WITH: ACK=" + packet.getAck() + " SEQ=" + packet.getSeq());
		InetAddress destAddress = null;
		try {
			destAddress = InetAddress.getByName("192.168.5."+destination);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ni.send(destAddress, packet.getBytes());
	}
}
