package protocol;

import utils.ByteUtils;
import utils.StringUtils;
import client.Client;

public final class GroupChatPacket extends Packet {
	private final Client sender;
	private String group;
	private String message;
	
	public GroupChatPacket() {
		this(new Client(), null, null);
	}
	
	public GroupChatPacket(Client sender, String group, String message) {
		super(Packet.TYPE_GROUP_CHAT);
		
		this.sender = sender;
		this.group = group;
		this.message = message;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] senderData = sender.serialize(Client.SERIALIZE_ADDRESS);
		byte[] groupData = StringUtils.getBytes(group);
		byte[] messageData = StringUtils.getBytes(message);
		byte[] buffer = new byte[8 + senderData.length + groupData.length + messageData.length];
		
		ByteUtils.getIntBytes(groupData.length, buffer, 0);
		ByteUtils.getIntBytes(messageData.length, buffer, 4);
		System.arraycopy(senderData, 0, buffer, 8, senderData.length);
		System.arraycopy(groupData, 0, buffer, 8 + senderData.length, groupData.length);
		System.arraycopy(messageData, 0, buffer, 8 + senderData.length + groupData.length, messageData.length);
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		int groupLength = ByteUtils.getIntFromBytes(buffer, offset);
		int messageLength = ByteUtils.getIntFromBytes(buffer, offset + 4);
		offset += 8;
		
		offset += sender.deserialize(buffer, offset);
		group = StringUtils.getString(buffer, offset, groupLength);
		message = StringUtils.getString(buffer, offset + groupLength, messageLength);
	}
	
	public Client getSender() {
		return sender;
	}
	
	public String getGroup() {
		return group;
	}
	
	public String getMessage() {
		return message;
	}
}
