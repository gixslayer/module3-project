package protocol;

import java.util.Arrays;

import utils.ByteUtils;
import network.TcpInterface;
import network.TcpMode;

public final class PacketHeader {
	private int source, destination, type, length, seq, ack, windowSize;
	private boolean synFlag, ackFlag, finFlag;
	private byte[] header;
	private int flags;
	
	public PacketHeader() {
		
	}
	
	public PacketHeader(int seq, int ack, int flags) {
		this.seq = seq;
		this.ack = ack;
		this.flags = flags;
	}
	
	public PacketHeader(int source, int destination, int type, int seq, int ack, boolean synFlag, boolean ackFlag, boolean finFlag, int windowSize, int dataLength) {
		this.source = source;
		this.destination = destination;
		this.type = type;
		this.seq = seq;
		this.ack = ack;
		this.synFlag = synFlag;
		this.ackFlag = ackFlag;
		this.finFlag = finFlag;
		this.windowSize = windowSize;
		this.header = new byte[0];
		this.length = dataLength;
		fillPacket();
	}
	
	public PacketHeader(byte[] packet) {
		this.header = packet;
		fillVariables();
	}
	
	private void fillPacket() {
		int sourceDest = (source<<4)+destination;
		int temp = length;
		int typeLength = (type<<4)+(temp>>>8);
		int length2 = temp - ((temp>>>8)<<8);
		System.out.println(temp + " " + length2 + " " + typeLength);
		int seq1 = seq>>>8;
		int seq2 = seq-((seq>>>8)<<8);
		int ack1 = ack>>>8;
		int ack2 = ack-((ack>>>8)<<8);
		int flags = (synFlag? 1:0)*4 +(ackFlag? 1:0)*2+(finFlag? 1:0);
		int windowSize1 = windowSize>>>8;
		int windowSize2 = windowSize-((windowSize>>>8)<<8);
		byte[] header = new byte[]{(byte)sourceDest, (byte)typeLength, (byte)length2, (byte)seq1, (byte)seq2, (byte)ack1, (byte)ack2, (byte)flags, (byte)windowSize1, (byte)windowSize2};
		this.header = header;
	}
	
	public void printData() {
		System.out.println("Source: " + source);
		System.out.println("Destination: " + destination);
		System.out.println("Type: " + type);
		System.out.println("Length: " + length);
		System.out.println("Seq: " + seq);
		System.out.println("Ack: " + ack);
		System.out.println("Flags: " + synFlag + ackFlag + finFlag);
		System.out.println("WindowSize: " + windowSize);
		System.out.println("pakketlengte: " + length);
		System.out.println("");
	}
	
	public byte[] getBytes() {
		return header;
	}
		
	public int calculateChecksum(byte[] data){
	    int length = data.length;
	    int i = 0;
	    int sum = 0;
	    
	    while( length > 1 ){
	        sum += ( ((data[i] << 8) & 0xFF00) | ((data[i + 1]) & 0xFF));
	        if( (sum & 0xFFFF0000) > 0 ){
	            sum = sum & 0xFFFF;
	            sum += 1;
	        }
	        i += 2;
	        length -= 2;
	    }

	    if (length > 0 ){ 
	        sum += (data[i] << 8 & 0xFF00); 
	        if( (sum & 0xFFFF0000) > 0) {
	            sum = sum & 0xFFFF;
	            sum += 1;
	        }
	    }

	    sum = ~sum; 
	    sum = sum & 0xFFFF;
	    return sum;
	}
	
	private int getUnsignedInt(int x) {
	    return x<0?256+x:x;
	}
	
	private void fillVariables() {
		source = getUnsignedInt(header[0])>>>4;
		destination = getUnsignedInt(header[0]) - ((getUnsignedInt(header[0])>>>4)<<4);
		type = getUnsignedInt(header[1])>>>4;
		int part1 = getUnsignedInt(header[1]) - ((getUnsignedInt(header[1])>>>4)<<4);
		length = (part1<<8) + getUnsignedInt(header[2]);
		seq = ((getUnsignedInt(header[3]))<<8) + getUnsignedInt(header[4]); 
		ack = ((getUnsignedInt(header[5]))<<8) + getUnsignedInt(header[6]);  
		synFlag = header[7]>>>2 == 1;
		ackFlag = (header[7]-(header[7]>>>2<<2)>>>1) == 1;
		finFlag = (header[7]-header[7]>>>1<<1) == 1;
		windowSize = ((getUnsignedInt(header[8]))<<8) + getUnsignedInt(header[9]);
	}
	
	public int getSource() {
		return source;
	}
	
	public int getDestination() {
		return destination;
	}
	
	public int getType() {
		return type;
	}
	
	public int getLength() {
		return length;
	}
	
	public int getAck() {
		return ack;
	}
	
	public int getSeq() {
		return seq;
	}
	
	public boolean getSynFlag() {
		return synFlag;
	}
	
	public boolean getAckFlag() {
		return ackFlag;
	}
	
	public boolean getFinFlag() {
		return finFlag;
	}
	
	public int getWindowSize() {
		return windowSize;
	}
	
	public byte[] serialize() {
		if(TcpInterface.MODE == TcpMode.OldTCP) {
			return header;
		} else {
			byte[] buffer = new byte[12];
			
			ByteUtils.getIntBytes(seq, buffer, 0);
			ByteUtils.getIntBytes(ack, buffer, 4);
			ByteUtils.getIntBytes(flags, buffer, 8);
			
			return buffer;
		}
	}
	
	public int deserialize(byte[] header, int dataLength, int offset, int length) {
		if(TcpInterface.MODE == TcpMode.OldTCP) {
			this.header = Arrays.copyOfRange(header, offset, offset+length);
			this.length = dataLength;
			fillVariables();
		} else { /* NewTCP */
			this.seq = ByteUtils.getIntFromBytes(header, offset);
			this.ack = ByteUtils.getIntFromBytes(header, offset + 4);
			this.flags = ByteUtils.getIntFromBytes(header, offset + 8);
		}
		
		return length;
	}
	
	// Only used by new TCP implementation.
	public int getFlags() {
		return flags;
	}
	
	// Only used by new TCP implementation.
	public boolean hasFlags(int flags) {
		return (this.flags & flags) == flags;
	}
}
