package project;

import java.net.InetAddress;
import java.net.UnknownHostException;

public class TCP {
	private static InetAddress myInetAddress;
	private static int myAddress;
	public static void handlePacket(Packet packet) {
		try {
			myInetAddress = InetAddress.getByName("192.168.5.3"); //TODO: get myAdress from another static function
			myAddress = Integer.parseInt(""+myInetAddress.getHostAddress().charAt(10));
		} catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		if(packet.getDestination() == myAddress) {
			if(packet.getSynFlag() && !packet.getAckFlag()) {
				//stuur SynAck
				sendSynAck(packet);
			} else if(packet.getSynFlag() && packet.getAckFlag()) {
				sendAck(packet);
			}
		}
	}
	
	public static void main(String args[]) {
		handlePacket(new Packet(1,3,1,1,1,true,true,true,1,new byte[3]));
	}
	
	public static void sendSyn(int destination) {
		Packet syn = new Packet(myAddress, destination, 0, 0, 0, true, false, false, 5, new byte[0]);
		//TODO: send packet
	}
	
	public static void sendSyn(InetAddress destination) {
		sendSyn(Integer.parseInt(""+destination.getHostAddress().charAt(10)));
	}
	
	public static void sendSynAck(Packet lastPacket) {
		Packet synAck = new Packet(myAddress, lastPacket.getSource(), 0, 0, 0, true, true, false, 5, new byte[0]);
		//TODO: send packet, ACK en SEQ goed doen
	}
	
	public static void sendAck(Packet lastPacket) {
		Packet ack = new Packet(myAddress, lastPacket.getSource(), 0, 0, 0, false, true, false, 5, new byte[0]);
		//TODO: send packet, ACK en SEQ goed doen

	}
}
