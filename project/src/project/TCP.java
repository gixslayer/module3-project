package project;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

import protocol.EmptyPacket;
import protocol.Packet;
import protocol.PacketHeader;
import network.NetworkInterface;

public class TCP {
	private static int myAddress;
	private static Map<Integer, TCP.State> connections;
	private static Map<Integer, Timer> timers;
	private static Map<Integer, int[]> lastInfo;
	private static Map<Integer, ArrayList<Packet>> toSend;
	private static Map<Integer, ArrayList<Packet>> packetsInBuffer;
	private static Map<Packet, Timer> timerOfPacket;
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
	
	public static boolean handlePacket(NetworkInterface ni, InetAddress myAddr, PacketHeader packet) {
		init(inetToInt(myAddr), packet.getSource());
		int destAddress = packet.getSource();
		TCP.ni = ni;
		boolean answer = true;
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
					ArrayList<Packet> array = toSend.get(destAddress);
					if(array == null) array = new ArrayList<Packet>();
					for(Packet a: array){
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
					ArrayList<Packet> array = toSend.get(destAddress);
					if(array == null) array = new ArrayList<Packet>();
					for(Packet a: array){
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
			} else {
				answer = false;
			}
			
			
		}
		return answer;
	}
	
	private static void ackReceivedDuringHandshake(PacketHeader packet) {
		ArrayList<Packet> buffer = packetsInBuffer.get(packet.getSource());
		Packet toDelete = null;
		
		for(Packet p: buffer) {
			if(p.getHeader().getSeq()+1 == packet.getAck()) {
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
		ArrayList<Packet> buffer = packetsInBuffer.get(packet.getSource());
		Packet toDelete = null;
		System.out.println("ack RECEIVED!!! " + buffer.size());
				
		for(Packet p: buffer) {
			if(p.getHeader().getSeq()+p.getHeader().getLength() == packet.getAck()) {
				//This is packet to remove
				toDelete = p;
				break;
			}
		}
		
		
		if(toDelete != null) {
			if(timerOfPacket.get(toDelete) != null) {
				timerOfPacket.get(toDelete).cancel();
				timerOfPacket.put(toDelete, null);
			}
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
	
	public static void ackTimeOut(Packet packet) {
		Timer timer = new Timer();
		timer.schedule(new AckTimeOut(packet), 100);
		timerOfPacket.put(packet, timer);
		sendPacket(packet, packet.getHeader().getDestination());
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
		ArrayList<Packet> temp = packetsInBuffer.get(destination);
		if(temp == null) {
			temp = new ArrayList<>();
		}
		
		
		Packet toSend = new EmptyPacket();
		toSend.setHeader(syn);
		temp.add(toSend);
		packetsInBuffer.put(destination, temp);
		Timer timer = new Timer();
		timer.schedule(new AckTimeOut(toSend), 100);
		timerOfPacket.put(toSend, timer);
		sendPacket(toSend, syn.getDestination());
	}
	
	private static void sendSyn(InetAddress destination) {
		sendSyn(Integer.parseInt(""+destination.getHostAddress().charAt(10)));
	}
	
	private static void sendSynAck(PacketHeader lastPacket) {
		PacketHeader synAck = new PacketHeader(myAddress, lastPacket.getSource(), 0, 0, 1, true, true, false, 5, 0);
		System.out.println(synAck.getDestination() + ", seq: " + synAck.getSeq() +", ack: " + synAck.getAck());
		
		ArrayList<Packet> temp = packetsInBuffer.get(synAck.getDestination());
		if(temp == null) {
			temp = new ArrayList<>();
		}
		
		lastInfo.put(lastPacket.getSource(), new int[]{synAck.getSeq(), synAck.getAck()});
		
		Packet toSend = new EmptyPacket();
		toSend.setHeader(synAck);
		temp.add(toSend);
		packetsInBuffer.put(synAck.getDestination(), temp);
		Timer timer = new Timer();
		timer.schedule(new AckTimeOut(toSend), 100);
		timerOfPacket.put(toSend, timer);
		sendPacket(toSend, synAck.getDestination());
	}
	
	private static void sendAckForSynAck(PacketHeader packet) {
		int newSeq = 1;
		int newAck = 1;
		PacketHeader ack = new PacketHeader(myAddress, packet.getSource(), 0, newSeq, newAck, false, true, false, 5, 0);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		lastInfo.put(packet.getSource(), new int[]{newSeq, newAck});
		Packet toSend = new EmptyPacket();
		toSend.setHeader(ack);
		sendPacket(toSend, ack.getDestination());
	}
	
	private static void sendAckForFinAck(PacketHeader lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+1;
		PacketHeader ack = new PacketHeader(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, false, 5, 0);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		
		Packet toSend = new EmptyPacket();
		toSend.setHeader(ack);
		sendPacket(toSend, ack.getDestination());
	}
	
	private static void sendAck(PacketHeader lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+lastPacket.getLength();
		PacketHeader ack = new PacketHeader(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, false, 5, 0);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		//add to info
		lastInfo.put(lastPacket.getSource(), new int[]{newSeq, newAck});
		Packet toSend = new EmptyPacket();
		toSend.setHeader(ack);
		sendPacket(toSend, ack.getDestination());
	}
	
	private static void sendFin(int destination) {
		int newSeq = lastInfo.get(destination)[1];
		int newAck = lastInfo.get(destination)[0];
		PacketHeader fin = new PacketHeader(myAddress, destination, 0, newSeq, newAck, false, false, true, 5, 0);
		System.out.println(fin.getDestination() + ", seq: " + fin.getSeq() +", ack: " + fin.getAck());
		ArrayList<Packet> temp = packetsInBuffer.get(destination);
		if(temp == null) {
			temp = new ArrayList<>();
		}
		
		Packet toSend = new EmptyPacket();
		toSend.setHeader(fin);
		temp.add(toSend);
		packetsInBuffer.put(destination, temp);
		Timer timer = new Timer();
		timer.schedule(new AckTimeOut(toSend), 100);
		timerOfPacket.put(toSend, timer);
		sendPacket(toSend, fin.getDestination());
	}
	
	private static void sendFinAck(PacketHeader lastPacket) {
		int newSeq = lastPacket.getAck();
		int newAck = lastPacket.getSeq()+1;
		PacketHeader ack = new PacketHeader(myAddress, lastPacket.getSource(), 0, newSeq, newAck, false, true, true, 5, 0);
		System.out.println(ack.getDestination() + ", seq: " + ack.getSeq() +", ack: " + ack.getAck());
		ArrayList<Packet> temp = packetsInBuffer.get(ack.getDestination());
		if(temp == null) {
			temp = new ArrayList<>();
		}
		
		Packet toSend = new EmptyPacket();
		toSend.setHeader(ack);
		temp.add(toSend);
		packetsInBuffer.put(ack.getDestination(), temp);
		Timer timer = new Timer();
		timer.schedule(new AckTimeOut(toSend), 100);
		timerOfPacket.put(toSend, timer);
		sendPacket(toSend, ack.getDestination());
	}
	
	public static void sendData(int source, int destination, Packet packet) {
		init(source, destination);
		if(connections.containsKey(destination) && connections.get(destination).equals(State.ESTABLISHED)) {
			PacketHeader toSend = new PacketHeader(myAddress, destination, 0, lastInfo.get(destination)[0], lastInfo.get(destination)[1], false, true, false, 5, packet.getContentLength());
			lastInfo.put(destination, new int[]{toSend.getSeq()+toSend.getLength(),toSend.getAck()});
			System.out.println(toSend.getDestination() + ", seq: " + toSend.getSeq() +", ack: " + toSend.getAck());
			ArrayList<Packet> temp = packetsInBuffer.get(destination);
			if(temp == null) {
				temp = new ArrayList<>();
			}
			
			packet.setHeader(toSend);
			Timer timer = new Timer();
			timer.schedule(new AckTimeOut(packet), 100);
			temp.add(packet);
			packetsInBuffer.put(destination, temp);
			timerOfPacket.put(packet, timer);
			sendPacket(packet, destination);
		} else if(!connections.containsKey(destination) || connections.get(destination).equals(State.CLOSED)){
			openConnection(destination);
			ArrayList<Packet> array = toSend.get(destination);
			if(array == null) array = new ArrayList<Packet>();
			array.add(packet);
			toSend.put(destination, array);
		} else {
			ArrayList<Packet> array = toSend.get(destination);
			if(array == null) array = new ArrayList<Packet>();
			array.add(packet);
			toSend.put(destination, array);
		}
	}
	
	public static void sendData(NetworkInterface ni, InetAddress source, InetAddress destination, Packet packet) {
		TCP.ni = ni;
		sendData(inetToInt(source), inetToInt(destination), packet);
	}
	
	public static void sendPacket(Packet packet, int destination) {
		InetAddress destAddress = null;
		try {
			destAddress = InetAddress.getByName("192.168.5."+destination);
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		ni.send(destAddress, packet);
	}
}
