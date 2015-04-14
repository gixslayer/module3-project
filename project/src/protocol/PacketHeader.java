package protocol;

import utils.ByteUtils;

public final class PacketHeader {
	private int seq;
	private int ack;
	private int flags;
	
	public PacketHeader() {
		this(0, 0, 0);
	}
	
	public PacketHeader(int seq, int ack, int flags) {
		this.seq = seq;
		this.ack = ack;
		this.flags = flags;
	}

	
	
	public byte[] serialize() {
		byte[] buffer = new byte[12];
			
		ByteUtils.getIntBytes(seq, buffer, 0);
		ByteUtils.getIntBytes(ack, buffer, 4);
		ByteUtils.getIntBytes(flags, buffer, 8);
			
		return buffer;
	}
	
	public int deserialize(byte[] header, int dataLength, int offset, int length) {
		this.seq = ByteUtils.getIntFromBytes(header, offset);
		this.ack = ByteUtils.getIntFromBytes(header, offset + 4);
		this.flags = ByteUtils.getIntFromBytes(header, offset + 8);
		
		return length;
	}
	
	public int getSeq() {
		return seq;
	}
	
	public int getAck() {
		return ack;
	}

	public int getFlags() {
		return flags;
	}

	public boolean hasFlags(int flags) {
		return (this.flags & flags) == flags;
	}
}
