package events;

import protocol.Packet;

public final class UnicastPacketReceivedEvent extends Event {
	private final Packet packet;
	
	public UnicastPacketReceivedEvent(Packet packet) {
		super(Event.TYPE_UNICAST_PACKET_RECEIVED);
		
		this.packet = packet;
	}
	
	public Packet getPacket() {
		return packet;
	}
}
