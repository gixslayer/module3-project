package events;

import protocol.Packet;
import client.Client;

public final class SendPacketEvent extends Event {
	private final Client destination;
	private final Packet packet;
	
	public SendPacketEvent(Client destination, Packet packet) {
		super(Event.TYPE_SEND_PACKET);
		
		this.destination = destination;
		this.packet = packet;
	}
	
	public Client getDestination() {
		return destination;
	}
	
	public Packet getPacket() {
		return packet;
	}
}
