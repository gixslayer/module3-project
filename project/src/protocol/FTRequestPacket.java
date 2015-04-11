package protocol;

import utils.ByteUtils;
import utils.StringUtils;
import client.Client;

public final class FTRequestPacket extends Packet {
	private int requestId;
	private String fileName;
	private long fileSize;
	private final Client sender;
	
	public FTRequestPacket() {
		this(0, null, 0, new Client());
	}
	
	public FTRequestPacket(int requestId, String fileName, long fileSize, Client sender) {
		super(Packet.TYPE_FT_REQUEST);
		
		this.requestId = requestId;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.sender = sender;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] nameData = StringUtils.getBytes(fileName);
		byte[] senderData = sender.serialize(Client.SERIALIZE_ADDRESS);
		byte[] buffer = new byte[12 + senderData.length + nameData.length];
		
		ByteUtils.getIntBytes(requestId, buffer, 0);
		ByteUtils.getLongBytes(fileSize, buffer, 4);
		System.arraycopy(senderData, 0, buffer, 12, senderData.length);
		System.arraycopy(nameData, 0, buffer, 12 + senderData.length, nameData.length);
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		requestId = ByteUtils.getIntFromBytes(buffer, offset);
		fileSize = ByteUtils.getLongFromBytes(buffer, offset + 4);
		int senderLength = sender.deserialize(buffer, offset + 12);
		fileName = StringUtils.getString(buffer, offset + 12 + senderLength, length - (12 + senderLength));
	}

	public int getRequestId() {
		return requestId;
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public long getFileSize() {
		return fileSize;
	}
	
	public Client getSender() {
		return sender;
	}
}
