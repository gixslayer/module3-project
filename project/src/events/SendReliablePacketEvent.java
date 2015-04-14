package events;

import network.Priority;
import protocol.Packet;
import client.Client;

public final class SendReliablePacketEvent extends Event {
	private final Client destination;
	private final Packet packet;
	private final Priority priority;
	
	public SendReliablePacketEvent(Client destination, Packet packet, Priority priority) {
		super(Event.TYPE_SEND_RELIABLE_PACKET);
		
		this.destination = destination;
		this.packet = packet;
		this.priority = priority;
	}
	
	public Client getDestination() {
		return destination;
	}
	
	public Packet getPacket() {
		return packet;
	}
	
	public Priority getPriority() {
		return priority;
	}
}
