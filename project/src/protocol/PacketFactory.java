package protocol;

public final class PacketFactory {
	public static Packet fromType(int type) {
		switch(type) {
		case Packet.TYPE_ANNOUNCE:
			return new AnnouncePacket();
		}
		
		throw new IllegalArgumentException("Invalid type");
	}
}
