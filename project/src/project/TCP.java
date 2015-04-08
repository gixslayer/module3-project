package project;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.HashMap;
import java.util.Map;

public class TCP {
	private static InetAddress myInetAddress;
	private static int myAddress;
	private static Map<Integer, TCP.State> connections;
	private static boolean constructed = false;
	
	private static void init() {
		if(!constructed) {
			try {
				myInetAddress = InetAddress.getByName("192.168.5.3"); //TODO: get myAdress from another static function
				myAddress = Integer.parseInt(""+myInetAddress.getHostAddress().charAt(10));
				constructed = true;
				connections = new HashMap<>();
			} catch (UnknownHostException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}
	
	public enum State {
		CLOSED, SYNSENT, SYNRECEIVED, ESTABLISHED, FIN_WAIT, LAST_ACK, TIME_WAIT;
	}
	
	public static void handlePacket(Packet packet) {
		init();
		if(packet.getDestination() == myAddress) {
			if(packet.getSynFlag() && !packet.getAckFlag() && !packet.getFinFlag()) {
				//stuur SynAck
				
				sendSynAck(packet);
			} else if(packet.getSynFlag() && packet.getAckFlag() && !packet.getFinFlag()) {
				sendAck(packet);
			} else if(packet.getFinFlag() && !packet.getAckFlag()) {
				sendFinAck(packet);
			} else if(packet.getFinFlag() && packet.getAckFlag()) {
				sendAck(packet);
			}
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
		if(connections.containsKey(destination) && (connections.get(destination).equals(State.ESTABLISHED) || connections.get(destination).equals(State.SYNRECEIVED))) {
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
		if(connections.containsKey(dest) && (connections.get(dest).equals(State.ESTABLISHED) || connections.get(dest).equals(State.SYNRECEIVED))) {
			sendFin(dest);
			connections.put(dest, State.FIN_WAIT);
			return true;
		} else {
			return false;
		}
	}
	
	public static void main(String args[]) {
		handlePacket(new Packet(1,3,1,1,1,true,true,true,1,new byte[3]));
	}
	
	private static void sendSyn(int destination) {
		Packet syn = new Packet(myAddress, destination, 0, 0, 0, true, false, false, 5, new byte[0]);
		//TODO: send packet
	}
	
	private static void sendSyn(InetAddress destination) {
		sendSyn(Integer.parseInt(""+destination.getHostAddress().charAt(10)));
	}
	
	private static void sendSynAck(Packet lastPacket) {
		Packet synAck = new Packet(myAddress, lastPacket.getSource(), 0, 0, 0, true, true, false, 5, new byte[0]);
		//TODO: send packet, ACK en SEQ goed doen
	}
	
	private static void sendAck(Packet lastPacket) {
		Packet ack = new Packet(myAddress, lastPacket.getSource(), 0, 0, 0, false, true, false, 5, new byte[0]);
		//TODO: send packet, ACK en SEQ goed doen

	}
	
	private static void sendFin(int destination) {
		Packet fin = new Packet(myAddress, destination, 0, 0, 0, false, false, true, 5, new byte[0]);
		//TODO: send packet, ACK en SEQ goed doen
	}
	
	private static void sendFinAck(Packet lastPacket) {
		Packet ack = new Packet(myAddress, lastPacket.getSource(), 0, 0, 0, false, true, true, 5, new byte[0]);
		//TODO: send packet, ACK en SEQ goed doen
	}
}
