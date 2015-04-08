package client;

import java.net.InetAddress;
import java.net.UnknownHostException;

import protocol.ByteUtils;

public class Client {
	
	private String name;
	private InetAddress address;
	private long lastSeen;
	private boolean indirect;
	private String route;
	
	public Client() {
		this.name = null;
		this.address = null;
		this.lastSeen = 0;
		this.indirect = false;
		this.route = null;
	}
	
	public Client(String name, InetAddress address, long lastSeen) {
		this.name = name;
		this.address = address;
		this.lastSeen = lastSeen;
		this.indirect = false;
		this.route = null;
	}
	
	public byte[] serialize() {
		byte[] nameBytes = name.getBytes(); // TOOD: Specify a charset
		byte[] addressBytes = address.getAddress();
		byte[] buffer = new byte[nameBytes.length + addressBytes.length + 16];
		
		ByteUtils.getLongBytes(lastSeen, buffer, 0);
		ByteUtils.getIntBytes(addressBytes.length, buffer, 8);
		ByteUtils.getIntBytes(nameBytes.length, buffer, 12);
		System.arraycopy(addressBytes, 0, buffer, 16, addressBytes.length);
		System.arraycopy(nameBytes, 0, buffer, 16 + addressBytes.length, nameBytes.length);

		return buffer;
	}
	
	public int deserialize(byte[] buffer, int offset) {
		int addressLength = ByteUtils.getIntFromBytes(buffer, offset + 8);
		int nameLength = ByteUtils.getIntFromBytes(buffer, offset + 12);
		byte[] addressBytes = new byte[addressLength];
		System.arraycopy(buffer, offset + 16, addressBytes, 0, addressLength);
		
		try {
			lastSeen = ByteUtils.getLongFromBytes(buffer, offset);
			address = InetAddress.getByAddress(addressBytes);
			name = new String(buffer, offset + addressLength + 16, nameLength);
			indirect = false;
			route = null;
		} catch (UnknownHostException e) {
			throw new RuntimeException(String.format("Failed to deserialize Client: %s%n", e.getMessage()));
		}
		
		return 16 + addressLength + nameLength;
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setAddress(InetAddress address) {
		this.address = address;
	}
	
	public void setLastSeen(long lastSeen) {
		this.lastSeen = lastSeen;
	}
	
	public void setDirect() {
		this.indirect = false;
		this.route = null;
	}
	
	public void setIndirect(String route) {
		this.indirect = true;
		this.route = route;
	}
	
	public void setRoute(String route) {
		this.route = route;
	}
	
	public String getName() {
		return name;
	}
	
	public InetAddress getAddress() {
		return address;
	}
	
	public long getLastSeen() {
		return lastSeen;
	}
	
	public boolean isIndirect() {
		return indirect;		
	}
	
	public String getRoute() {
		return route;
	}
	
	public boolean equals(Client other) {
		if(other == null) {
			return false;
		}
		
		return this.name.equals(other.name);
	}
	
	@Override
	public String toString() {
		return String.format("%s@%s", name, address.getHostAddress());
	}
}
