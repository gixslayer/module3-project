package protocol;

public final class PacketFactory {
	public static Packet fromType(int type) {
		switch(type) {
		case Packet.TYPE_ANNOUNCE:
			return new AnnouncePacket();
		case Packet.TYPE_MULTICAST_CHAT:
			return new MulticastChatPacket();
		}
		
		throw new IllegalArgumentException("Invalid type");
	}
}
