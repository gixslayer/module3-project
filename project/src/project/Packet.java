package project;

import java.util.Arrays;

public class Packet {
	private int source, destination, type, length, seq, ack, windowSize, checksum;
	private boolean synFlag, ackFlag, finFlag;
	private byte[] packet, data;
	
	public Packet(int source, int destination, int type, int seq, int ack, boolean synFlag, boolean ackFlag, boolean finFlag, int windowSize, byte[] data) {
		this.source = source;
		this.destination = destination;
		this.type = type;
		this.seq = seq;
		this.ack = ack;
		this.synFlag = synFlag;
		this.ackFlag = ackFlag;
		this.finFlag = finFlag;
		this.windowSize = windowSize;
		this.checksum = 0;
		this.packet = new byte[0];
		this.data = data;
		fillPacket();
	}
	
	public Packet(byte[] packet) {
		this.packet = packet;
		this.data = Arrays.copyOfRange(packet, 10, packet.length);
		fillVariables();
	}
	
	private void fillPacket() {
		this.length = data.length;
		int sourceDest = (source<<4)+destination;
		System.out.println(source<<4);
		int temp = data.length;
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
		temp = calculateChecksum();
		checksum = temp;
		int checkSum1 = temp>>>8;
		int checkSum2 = temp-((temp>>>8)<<8);
		byte[] header = new byte[]{(byte)sourceDest, (byte)typeLength, (byte)length2, (byte)seq1, (byte)seq2, (byte)ack1, (byte)ack2, (byte)flags, (byte)windowSize1, (byte)windowSize2, (byte)checkSum1, (byte)checkSum2};
		packet = new byte[header.length+data.length];
		System.arraycopy(header, 0, packet, 0, header.length);
		System.arraycopy(data, 0, packet, header.length, data.length);
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
		System.out.println("Checksum: " + checksum);
		System.out.println("pakketlengte: " + packet.length);
	}
	
	public byte[] getBytes() {
		return packet;
	}
	
	private int calculateChecksum() {
		return 32000;
	}
	
	private int getUnsignedInt(int x) {
	    return x<0?256+x:x;
	}
	
	private void fillVariables() {
		source = getUnsignedInt(packet[0])>>>4;
		destination = getUnsignedInt(packet[0]) - ((getUnsignedInt(packet[0])>>>4)<<4);
		type = getUnsignedInt(packet[1])>>>4;
		int part1 = getUnsignedInt(packet[1]) - ((getUnsignedInt(packet[1])>>>4)<<4);
		length = (part1<<8) + getUnsignedInt(packet[2]);
		seq = ((getUnsignedInt(packet[3]))<<8) + getUnsignedInt(packet[4]); 
		ack = ((getUnsignedInt(packet[5]))<<8) + getUnsignedInt(packet[6]);  
		synFlag = packet[7]>>>2 == 1;
		ackFlag = (packet[7]-(packet[7]>>>2<<2)>>>1) == 1;
		finFlag = (packet[7]-packet[7]>>>1<<1) == 1;
		windowSize = ((getUnsignedInt(packet[8]))<<8) + getUnsignedInt(packet[9]); 
		checksum = (((int)packet[10])<<8) + packet[11];
		
		data = Arrays.copyOfRange(packet, 11, packet.length);
	}
}
