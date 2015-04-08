package client;

import java.net.InetAddress;
import java.net.UnknownHostException;

import utils.ByteUtils;
import utils.StringUtils;

public class Client {
	public static final int SERIALIZE_NAME_ONLY = 0x0;
	public static final int SERIALIZE_ADDRESS = 0x1;
	public static final int SERIALIZE_LASTSEEN = 0x2;
	
	private String name;
	private InetAddress address;
	private long lastSeen;
	private boolean indirect;
	private Client route;
	
	public Client() {
		this(null);
	}
	
	public Client(String name) {
		this.name = name;
		this.address = null;
		this.lastSeen = 0;
		this.indirect = false;
		this.route = null;
	}

	//-------------------------------------------
	// Serialization.
	//-------------------------------------------	
	public byte[] serialize(int flags) {
		boolean serializeAddress = (flags & SERIALIZE_ADDRESS) == SERIALIZE_ADDRESS;
		boolean serializeLastSeen = (flags & SERIALIZE_LASTSEEN) == SERIALIZE_LASTSEEN;

		byte[] nameBytes = StringUtils.getBytes(name);
		byte[] addressBytes = serializeAddress ? address.getAddress() : null;
		int nameLength = nameBytes.length;
		int addressLength = serializeAddress ? addressBytes.length : 0;
		int totalLength = 8 + nameLength;
		int offset = 8 + nameLength;
		
		if(serializeAddress) totalLength += 4 + addressBytes.length;
		if(serializeLastSeen) totalLength += 8;
		
		byte[] buffer = new byte[totalLength];
		
		ByteUtils.getIntBytes(flags, buffer, 0);
		ByteUtils.getIntBytes(nameLength, buffer, 4);
		System.arraycopy(nameBytes, 0, buffer, 8, nameLength);
		
		if(serializeAddress) {
			ByteUtils.getIntBytes(addressLength, buffer, offset);
			System.arraycopy(addressBytes, 0, buffer, offset + 4, addressLength);
			offset += 4 + addressLength;
		}
		
		if(serializeLastSeen) {
			ByteUtils.getLongBytes(lastSeen, buffer, offset);
		}
		
		return buffer;
	}
	
	public int deserialize(byte[] buffer, int offset) {
		int flags = ByteUtils.getIntFromBytes(buffer, offset);
		int nameLength = ByteUtils.getIntFromBytes(buffer, offset + 4);
		String name = StringUtils.getString(buffer, offset + 8, nameLength);
		InetAddress address = null;
		long lastSeen = 0;
		int bytesRead = 8 + nameLength;

		boolean serializedAddress = (flags & SERIALIZE_ADDRESS) == SERIALIZE_ADDRESS;
		boolean serializedLastSeen = (flags & SERIALIZE_LASTSEEN) == SERIALIZE_LASTSEEN;
		
		if(serializedAddress) {
			int addressLength = ByteUtils.getIntFromBytes(buffer, offset + bytesRead);
			byte[] addressBytes = new byte[addressLength];
			System.arraycopy(buffer, offset + bytesRead + 4, addressBytes, 0, addressLength);
			bytesRead += 4 + addressLength;

			try {
				address = InetAddress.getByAddress(addressBytes);
			} catch(UnknownHostException e) {
				throw new RuntimeException(String.format("Failed to deserialize Client: %s%n", e.getMessage()));				
			}
		}
		
		if(serializedLastSeen) {
			lastSeen = ByteUtils.getLongFromBytes(buffer, offset + bytesRead);
			bytesRead += 8;
		}
		
		this.name = name;
		this.address = address;
		this.lastSeen = lastSeen;
		this.indirect = false;
		this.route = null;
		
		return bytesRead;
	}
	
	//-------------------------------------------
	// Setters.
	//-------------------------------------------
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
	
	public void setIndirect(Client route) {
		this.indirect = true;
		this.route = route;
	}
	
	public void setRoute(Client route) {
		this.route = route;
	}
	
	//-------------------------------------------
	// Getters.
	//-------------------------------------------
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
	
	public Client getRoute() {
		return route;
	}
	
	//-------------------------------------------
	// Object overrides.
	//-------------------------------------------
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(!(obj instanceof Client)) return false;
		
		Client other = (Client)obj;
		
		if(!this.name.equals(other.name)) return false;
		if(!this.address.equals(other.address)) return false;
		
		return true;
	}
	
	@Override
	public int hashCode() {
		return name.hashCode() ^ address.hashCode();
	}
	
	@Override
	public String toString() {
		return String.format("%s@%s", name, address.getHostAddress());
	}
	
	//-------------------------------------------
	// !!!TEMP HACK METHOD!!!
	//-------------------------------------------
	public static Client fromString(String str) {
		return null;
	}
}
