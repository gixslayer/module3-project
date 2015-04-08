package protocol;

public final class PacketFactory {
	public static Packet fromType(int type) {
		switch(type) {
		case Packet.TYPE_ANNOUNCE:
			return new AnnouncePacket();
		case Packet.TYPE_CHAT:
			return new ChatPacket();
		case Packet.TYPE_DISCONNECT:
			return new DisconnectPacket();
		case Packet.TYPE_ROUTE_REQUEST:
			return new RouteRequestPacket();
		case Packet.TYPE_PRIVATE_CHAT:
			return new PrivateChatPacket();
		}
		
		throw new IllegalArgumentException("Invalid type");
	}
}
