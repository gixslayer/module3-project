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
		case Packet.TYPE_EMPTY:
			return new EmptyPacket();
		case Packet.TYPE_CANNOT_ROUTE:
			return new CannotRoutePacket();
		case Packet.TYPE_GROUP_CHAT:
			return new GroupChatPacket();
		case Packet.TYPE_FT_DATA:
			return new FTDataPacket();
		case Packet.TYPE_FT_REPLY:
			return new FTReplyPacket();
		case Packet.TYPE_FT_REQUEST:
			return new FTRequestPacket();
		}
		
		throw new IllegalArgumentException("Invalid type");
	}
}
