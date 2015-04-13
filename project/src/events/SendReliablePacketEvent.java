package events;

import protocol.Packet;
import client.Client;

public final class SendReliablePacketEvent extends Event {
	private final Client destination;
	private final Packet packet;
	
	public SendReliablePacketEvent(Client destination, Packet packet) {
		super(Event.TYPE_SEND_RELIABLE_PACKET);
		
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
