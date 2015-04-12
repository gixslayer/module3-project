package events;

import protocol.Packet;

public final class MulticastPacketReceivedEvent extends Event {
	private final Packet packet;
	
	public MulticastPacketReceivedEvent(Packet packet) {
		super(Event.TYPE_MULTICAST_PACKET_RECEIVED);
		
		this.packet = packet;
	}
	
	public Packet getPacket() {
		return packet;
	}
}
