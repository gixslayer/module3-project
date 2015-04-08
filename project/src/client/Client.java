package client;

import java.net.InetAddress;
import java.net.UnknownHostException;

import protocol.ByteUtils;

public class Client {
	public static final int SERIALIZE_DEFAULT = 0x0;
	public static final int SERIALIZE_ADDRESS = 0x1;
	
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
	
	public Client(String name) {
		this.name = name;
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
	
	public byte[] serialize(int flags) {
		boolean serializeAddress = (flags & SERIALIZE_ADDRESS) == SERIALIZE_ADDRESS;
		byte[] nameBytes = name.getBytes(); // TOOD: Specify a charset
		byte[] addressBytes = serializeAddress ? address.getAddress() : null;
		int nameLength = nameBytes.length;
		int addressLength = serializeAddress ? addressBytes.length : 0;
		int totalLength = nameLength + addressLength + 16;
		byte[] buffer = serializeAddress ? new byte[totalLength + 4] : new byte[totalLength];
		
		ByteUtils.getLongBytes(lastSeen, buffer, 0);
		ByteUtils.getIntBytes(flags, buffer, 8);
		ByteUtils.getIntBytes(nameLength, buffer, 12);
		System.arraycopy(nameBytes, 0, buffer, 16, nameLength);
		if(serializeAddress) {
			ByteUtils.getIntBytes(addressLength, buffer, 16 + nameLength);
			System.arraycopy(addressBytes, 0, buffer, 20 + nameLength, addressLength);
		}
		
		return buffer;
	}
	
	public int deserialize(byte[] buffer, int offset) {
		long lastSeen = ByteUtils.getLongFromBytes(buffer, offset);
		int flags = ByteUtils.getIntFromBytes(buffer, offset + 8);
		int nameLength = ByteUtils.getIntFromBytes(buffer, offset + 12);
		String name = new String(buffer, offset + 16, nameLength);
		InetAddress address = null;
		int bytesRead = 16 + nameLength;

		boolean serializedAddress = (flags & SERIALIZE_ADDRESS) == SERIALIZE_ADDRESS;
		
		if(serializedAddress) {
			int addressLength = ByteUtils.getIntFromBytes(buffer, offset + 16 + nameLength);
			byte[] addressBytes = new byte[addressLength];
			System.arraycopy(buffer, offset + 20 + nameLength, addressBytes, 0, addressLength);
			bytesRead += 4 + addressLength;

			try {
				address = InetAddress.getByAddress(addressBytes);
			} catch(UnknownHostException e) {
				throw new RuntimeException(String.format("Failed to deserialize Client: %s%n", e.getMessage()));				
			}
		}
		
		this.lastSeen = lastSeen;
		this.address = address;
		this.name = name;
		indirect = false;
		route = null;
		
		return bytesRead;
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
