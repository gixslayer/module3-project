package protocol;

import client.Client;
import utils.ByteUtils;
import utils.StringUtils;

public final class PrivateChatPacket extends Packet {
	private Client client;
	private String message;
	
	public PrivateChatPacket() {
		this(new Client(), null);
	}
	
	public PrivateChatPacket(Client client, String message) {
		super(Packet.TYPE_PRIVATE_CHAT);
		
		this.client = client;
		this.message = message;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] clientBytes = client.serialize(Client.SERIALIZE_ADDRESS);
		byte[] messageBytes = StringUtils.getBytes(message);
		byte[] buffer = new byte[clientBytes.length + messageBytes.length + 8];
		
		ByteUtils.getIntBytes(clientBytes.length, buffer, 0);
		ByteUtils.getIntBytes(messageBytes.length, buffer, 4);
		System.arraycopy(clientBytes, 0, buffer, 8, clientBytes.length);
		System.arraycopy(messageBytes, 0, buffer, 8 + clientBytes.length, messageBytes.length);
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		int clientLength = ByteUtils.getIntFromBytes(buffer, offset);
		int messageLength = ByteUtils.getIntFromBytes(buffer, offset + 4);
		
		client.deserialize(buffer, offset + 8);
		message = StringUtils.getString(buffer, offset + 8 + clientLength, messageLength);
	}
	
	public void setClient(Client client) {
		this.client = client;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public Client getClient() {
		return client;
	}
	
	public String getMessage() {
		return message;
	}
}
