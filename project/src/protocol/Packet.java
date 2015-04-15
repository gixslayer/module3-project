package protocol;

import java.net.InetAddress;

import utils.ByteUtils;

public abstract class Packet {
	public static final int TYPE_ANNOUNCE = 0;
	public static final int TYPE_DISCONNECT = 1;
	public static final int TYPE_CHAT = 2;
	public static final int TYPE_PRIVATE_CHAT = 3;
	public static final int TYPE_ROUTE_REQUEST = 4;
	public static final int TYPE_CANNOT_ROUTE = 5;
	public static final int TYPE_EMPTY = 6;
	public static final int TYPE_GROUP_CHAT = 7;
	public static final int TYPE_FT_REQUEST = 8;
	public static final int TYPE_FT_REPLY = 9;
	public static final int TYPE_FT_DATA = 10;
	public static final int TYPE_FT_CANCEL = 11;
	public static final int TYPE_FT_FAILED = 12;
	public static final int TYPE_FT_PROGRESS = 13;
	public static final int TYPE_FT_COMPLETED = 14;
	public static final int TYPE_POKE = 15;
	public static final int TYPE_ENCRYPTION = 16;
	
	private final int type;
	private InetAddress sourceAddress;
	private boolean hasHeader;
	private PacketHeader header;
	private byte[] serializedContent;
	
	public Packet(int type) {
		this.type = type;
		this.hasHeader = false;
		this.header = null;
		this.serializedContent = null;
	}
	
	public byte[] serialize() {
		byte[] content = getContent();
		byte[] headerData = hasHeader ? header.serialize() : null;
		int contentLength = content.length;
		int headerLength = hasHeader ? headerData.length : 0;
		int length = hasHeader ? 13 + contentLength + headerLength : 9 + content.length;
		byte[] buffer = new byte[length];
		int offset = 5;
		
		ByteUtils.getIntBytes(type, buffer, 0);
		buffer[4] = hasHeader ? (byte)0x1 : (byte)0x0;
		
		ByteUtils.getIntBytes(contentLength, buffer, offset);
		System.arraycopy(content, 0, buffer, offset + 4, contentLength);
		offset += 4 + contentLength;
		
		if(hasHeader) {
			ByteUtils.getIntBytes(headerLength, buffer, offset);
			System.arraycopy(headerData, 0, buffer, offset + 4, headerLength);
		}
		
		return buffer;
	}
	
	public static Packet deserialize(InetAddress sourceAddress, byte[] buffer) {
		int type = ByteUtils.getIntFromBytes(buffer, 0);
		Packet packet = PacketFactory.fromType(type);
		PacketHeader header = null;
		boolean hasHeader = buffer[4] == 0x1;
		int offset = 5;
		
		int contentLength = ByteUtils.getIntFromBytes(buffer, offset);
		packet.deserializeContent(buffer, offset + 4, contentLength);
		offset += 4 + contentLength;
		
		if(hasHeader) {
			header = new PacketHeader();
			int headerLength = ByteUtils.getIntFromBytes(buffer, offset);
			header.deserialize(buffer, contentLength, offset + 4, headerLength);
		}
		
		packet.sourceAddress = sourceAddress;
		packet.hasHeader = hasHeader;
		packet.header = header;
		
		return packet;
	}
	
	protected abstract byte[] serializeContent();
	protected abstract void deserializeContent(byte[] buffer, int offset, int length);
	
	public void clearHeader() {
		this.hasHeader = false;
		this.header = null;
	}
	
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
	
	public byte[] getContent() {
		if(serializedContent == null) {
			serializedContent = serializeContent();
		}
		
		return serializedContent;
	}
	
	public int getContentLength() {
		return getContent().length;
	}
}
