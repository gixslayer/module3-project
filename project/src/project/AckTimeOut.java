package project;

import java.util.TimerTask;

import protocol.Packet;
import protocol.PacketHeader;

public class AckTimeOut extends TimerTask {

	private Packet packet;
	private PacketHeader header;
	
	public AckTimeOut(Packet packet) {
		this.packet = packet;
	}
	
	public AckTimeOut(PacketHeader header) {
		this.header = header;
	}
	
	@Override
	public void run() {
		// TODO Auto-generated method stub
		if(packet != null) {
			TCP.ackTimeOut(packet);
		} else if(header != null) {
			TCP.ackTimeOut(header);
		}
		
		this.cancel();
	}
}
