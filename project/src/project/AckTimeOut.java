package project;

import java.util.TimerTask;

import protocol.Packet;
import protocol.PacketHeader;

public class AckTimeOut extends TimerTask {

	private Packet packet;
	
	public AckTimeOut(Packet packet) {
		this.packet = packet;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		TCP.ackTimeOut(packet);
		
		this.cancel();
	}
}
