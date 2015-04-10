package project;

import java.util.TimerTask;

import protocol.PacketHeader;

public class AckTimeOut extends TimerTask {

	private PacketHeader packet;
	
	public AckTimeOut(PacketHeader packet) {
		this.packet = packet;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		TCP.ackTimeOut(packet);
		
		this.cancel();
	}
}
