package protocol;

public final class ChatPacket extends Packet {
	private String name;
	private String message;
	
	public ChatPacket() {
		this(null, null);
	}
	
	public ChatPacket(String name, String message) {
		super(Packet.TYPE_CHAT);
		
		this.name = name;
		this.message = message;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] nameBytes = name.getBytes();
		byte[] messageBytes = message.getBytes();
		byte[] buffer = new byte[nameBytes.length + messageBytes.length + 8];
		
		ByteUtils.getIntBytes(nameBytes.length, buffer, 0);
		ByteUtils.getIntBytes(messageBytes.length, buffer, 4);
		System.arraycopy(nameBytes, 0, buffer, 8, nameBytes.length);
		System.arraycopy(messageBytes, 0, buffer, 8 + nameBytes.length, messageBytes.length);
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		int nameLength = ByteUtils.getIntFromBytes(buffer, offset + 0);
		int messageLength = ByteUtils.getIntFromBytes(buffer, offset + 4);
		
		name = new String(buffer, offset + 8, nameLength);
		message = new String(buffer, offset + 8 + nameLength, messageLength);
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public void setMessage(String message) {
		this.message = message;
	}
	
	public String getName() {
		return name;
	}
	
	public String getMessage() {
		return message;
	}
}
