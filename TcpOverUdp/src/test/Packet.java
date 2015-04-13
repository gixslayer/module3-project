package test;

public class Packet {
	private int flags;
	private int seq;
	private int ack;
	private byte[] data;
	
	public Packet() {
		
	}
	
	public Packet(byte[] data) {
		this(0, 0, 0, data);
	}
	
	public Packet(int flags, int seq, int ack) {
		this(flags, seq, ack, new byte[0]);
	}
	
	public Packet(int flags, int seq, int ack, byte[] data) {
		this.flags = flags;
		this.seq = seq;
		this.ack = ack;
		this.data = data;
	}
	
	public byte[] serialize() {
		byte[] buffer = new byte[16 + data.length];
		
		getIntBytes(flags, buffer, 0);
		getIntBytes(seq, buffer, 4);
		getIntBytes(ack, buffer, 8);
		getIntBytes(data.length, buffer, 12);
		System.arraycopy(data, 0, buffer, 16, data.length);
		
		return buffer;
	}
	
	public void deserialize(byte[] buffer) {
		flags = getIntFromBytes(buffer, 0);
		seq = getIntFromBytes(buffer, 4);
		ack = getIntFromBytes(buffer, 8);
		int dataLength = getIntFromBytes(buffer, 12);
		data = new byte[dataLength];
		System.arraycopy(buffer, 16, data, 0, dataLength);
	}
	
	public void setFlags(int flags) {
		this.flags = flags;
	}
	
	public void setSeq(int seq) {
		this.seq = seq;
	}
	
	public void setAck(int ack) {
		this.ack = ack;
	}
	
	public int getFlags() {
		return flags;
	}
	
	public int getSeq() {
		return seq;
	}
	
	public int getAck() {
		return ack;
	}
	
	public int getContentLength() {
		return data.length;
	}
	
	public byte[] getData() {
		return data;
	}
	
	private static void getIntBytes(int value, byte[] dest, int offset) {
		dest[offset++] = (byte)(value & 0xff);
		dest[offset++] = (byte)((value >> 8) & 0xff);
		dest[offset++] = (byte)((value >> 16) & 0xff);
		dest[offset++] = (byte)(value >> 24);
	}
	
	private static int getIntFromBytes(byte[] buffer, int offset) {
		int result = 0;
		
		result |= (buffer[offset++] & 0xff);
		result |= ((buffer[offset++] & 0xff) << 8);
		result |= ((buffer[offset++] & 0xff) << 16);
		result |= ((buffer[offset++] & 0xff) << 24);
		
		return result;
	}
}
