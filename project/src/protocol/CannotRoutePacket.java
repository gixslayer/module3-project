package protocol;

import client.Client;

public final class CannotRoutePacket extends Packet {
	private final Client source;
	private Client hop;
	private final Client destination;
	
	public CannotRoutePacket() {
		this(new Client(), new Client(), new Client());
	}
	
	public CannotRoutePacket(Client source, Client hop, Client destination) {
		super(Packet.TYPE_CANNOT_ROUTE);
		
		this.source = source;
		this.hop = hop;
		this.destination = destination;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] sourceBytes = source.serialize(Client.SERIALIZE_ADDRESS);
		byte[] hopBytes = hop.serialize(Client.SERIALIZE_ADDRESS);
		byte[] destinationBytes = destination.serialize(Client.SERIALIZE_ADDRESS);
		byte[] buffer = new byte[sourceBytes.length + hopBytes.length + destinationBytes.length];
		
		System.arraycopy(sourceBytes, 0, buffer, 0, sourceBytes.length);
		System.arraycopy(hopBytes, 0, buffer, sourceBytes.length, hopBytes.length);
		System.arraycopy(destinationBytes, 0, buffer, sourceBytes.length + hopBytes.length, destinationBytes.length);
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		offset += source.deserialize(buffer, offset);
		offset += hop.deserialize(buffer, offset);
		offset += destination.deserialize(buffer, offset);
	}
	
	public void setHop(Client hop) {
		this.hop = hop;
	}
	
	public Client getSource() {
		return source;
	}
	
	public Client getHop() {
		return hop;
	}
	
	public Client getDestination() {
		return destination;
	}
}
