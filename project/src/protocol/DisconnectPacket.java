package protocol;

import client.Client;

public final class DisconnectPacket extends Packet {
	private final Client client;
	
	public DisconnectPacket() {
		this(new Client());
	}
	
	public DisconnectPacket(Client client) {
		super(Packet.TYPE_DISCONNECT);
		
		this.client = client;
	}

	@Override
	protected byte[] serializeContent() {
		return client.serialize(Client.SERIALIZE_ADDRESS);
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		client.deserialize(buffer, offset);
	}
	
	public Client getClient() {
		return client;
	}
}
