package protocol;

import java.net.InetAddress;

import utils.ByteUtils;

public abstract class Packet {
	public static final int TYPE_ANNOUNCE = 0;
	public static final int TYPE_CHAT = 1;
	public static final int TYPE_DISCONNECT = 2;
	public static final int TYPE_ROUTE_REQUEST = 3;
	public static final int TYPE_PRIVATE_CHAT = 4;
	public static final int TYPE_EMPTY_PACKET = 5;
	
	private final int type;
	private InetAddress sourceAddress;
	private boolean hasHeader;
	private PacketHeader header;
	
	public Packet(int type) {
		this.type = type;
		this.hasHeader = false;
		this.header = null;
	}
	
	public byte[] serialize() {
		byte[] content = serializeContent();
		byte[] headerData = hasHeader ? header.serialize() : null;
		int contentLength = content.length;
		int headerLength = hasHeader ? headerData.length : 0;
		int length = hasHeader ? 13 + headerLength + contentLength : 9 + content.length;
		byte[] buffer = new byte[length];
		int offset = 5;
		
		ByteUtils.getIntBytes(type, buffer, 0);
		buffer[4] = hasHeader ? (byte)0x1 : (byte)0x0;
		
		if(hasHeader) {
			ByteUtils.getIntBytes(headerLength, buffer, offset);
			System.arraycopy(headerData, 0, buffer, offset + 4, headerLength);
			offset += 4 + headerLength;
		}
		
		ByteUtils.getIntBytes(contentLength, buffer, offset);
		System.arraycopy(content, 0, buffer, offset + 4, contentLength);
		
		return buffer;
	}
	
	public static Packet deserialize(InetAddress sourceAddress, byte[] buffer) {
		int type = ByteUtils.getIntFromBytes(buffer, 0);
		Packet packet = PacketFactory.fromType(type);
		PacketHeader header = null;
		boolean hasHeader = buffer[4] == 0x1;
		int offset = 5;
		
		if(hasHeader) {
			header = new PacketHeader();
			int headerLength = ByteUtils.getIntFromBytes(buffer, offset);
			offset += 4 + header.deserialize(buffer, offset + 4, headerLength);
		}
		
		int contentLength = ByteUtils.getIntFromBytes(buffer, offset);
		
		packet.deserializeContent(buffer, offset + 4, contentLength);
		packet.sourceAddress = sourceAddress;
		packet.hasHeader = hasHeader;
		packet.header = header;
		
		return packet;
	}
	
	protected abstract byte[] serializeContent();
	protected abstract void deserializeContent(byte[] buffer, int offset, int length);
	
	public void setHeader(PacketHeader header) {
		this.hasHeader = true;
		this.header = header;
	}
	
	public int getType() {
		return type;
	}
	
	public InetAddress getSourceAddress() {
		return sourceAddress;
	}
	
	public boolean hasHeader() {
		return hasHeader;
	}
	
	public PacketHeader getHeader() {
		return header;
	}
}
