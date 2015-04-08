package protocol;

import java.net.InetAddress;

public abstract class Packet {
	public static final int TYPE_ANNOUNCE = 0;
	public static final int TYPE_CHAT = 1;
	public static final int TYPE_DISCONNECT = 2;
	public static final int TYPE_ROUTE_REQUEST = 3;
	public static final int TYPE_PRIVATE_CHAT = 4;
	
	private final int type;
	private InetAddress address;
	
	public Packet(int type) {
		this.type = type;
	}
	
	public byte[] serialize() {
		byte[] content = serializeContent();
		byte[] buffer = new byte[8 + content.length];
		
		ByteUtils.getIntBytes(type, buffer, 0);
		ByteUtils.getIntBytes(content.length, buffer, 4);
		System.arraycopy(content, 0, buffer, 8, content.length);
		
		return buffer;
	}
	
	public static Packet deserialize(InetAddress address, byte[] buffer) {
		int type = ByteUtils.getIntFromBytes(buffer, 0);
		int contentLength = ByteUtils.getIntFromBytes(buffer, 4);
		
		Packet packet = PacketFactory.fromType(type);
		
		packet.deserializeContent(buffer, 8, contentLength);
		packet.address = address;
		
		return packet;
	}
	
	protected abstract byte[] serializeContent();
	protected abstract void deserializeContent(byte[] buffer, int offset, int length);
	
	public int getType() {
		return type;
	}
	
	public InetAddress getAddress() {
		return address;
	}
}
