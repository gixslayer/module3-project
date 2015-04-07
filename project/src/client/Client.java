package client;

import protocol.ByteUtils;

public class Client {
	private String name;
	private int address;
	private long lastSeen;
	
	public Client() {
		this.name = null;
		this.address = 0;
		this.lastSeen = 0;
	}
	
	public Client(String name, int address, long lastSeen) {
		this.name = name;
		this.address = address;
		this.lastSeen = lastSeen;
	}
	
	public byte[] serialize() {
		byte[] nameBytes = name.getBytes(); // TOOD: Specify a charset
		byte[] buffer = new byte[nameBytes.length + 16];
		
		ByteUtils.getLongBytes(lastSeen, buffer, 0);
		ByteUtils.getIntBytes(address, buffer, 8);
		ByteUtils.getIntBytes(nameBytes.length, buffer, 12);
		System.arraycopy(nameBytes, 0, buffer, 16, nameBytes.length);

		return buffer;
	}
	
	public int deserialize(byte[] buffer, int offset) {
		lastSeen = ByteUtils.getLongFromBytes(buffer, offset);
		address = ByteUtils.getIntFromBytes(buffer, offset + 8);
		int nameLength = ByteUtils.getIntFromBytes(buffer, offset + 12);
		name = new String(buffer, offset + 16, nameLength);
		
		return 16 + nameLength;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setAddress(int address) {
		this.address = address;
	}
	
	public void setLastSeen(long lastSeen) {
		this.lastSeen = lastSeen;
	}
	
	public String getName() {
		return name;
	}
	
	public int getAddress() {
		return address;
	}
	
	public long getLastSeen() {
		return lastSeen;
	}
	
	public boolean equals(Client other) {
		if(other == null) {
			return false;
		}
		
		return this.name.equals(other.name);
	}
	
	@Override
	public String toString() {
		return String.format("%s@%d", name, address);
	}
}
