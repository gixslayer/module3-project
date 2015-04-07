package network;

import java.net.InetAddress;

import protocol.Packet;

public final class ReceivedPacket {
	private final Packet packet;
	private final InetAddress address;
	
	public ReceivedPacket(Packet packet, InetAddress address) {
		this.packet = packet;
		this.address = address;
	}
	
	public Packet getPacket() {
		return packet;
	}
	
	public InetAddress getAddress() {
		return address;
	}
}
