package protocol;

public final class DisconnectPacket extends Packet {
	private String name;
	
	public DisconnectPacket() {
		this(null);
	}
	
	public DisconnectPacket(String name) {
		super(Packet.TYPE_DISCONNECT);
		
		this.name = name;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] nameBytes = name.getBytes();
		byte[] buffer = new byte[nameBytes.length + 4];
		
		ByteUtils.getIntBytes(nameBytes.length, buffer, 0);
		System.arraycopy(nameBytes, 0, buffer, 4, nameBytes.length);
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		int nameLength = ByteUtils.getIntFromBytes(buffer, offset);
		name = new String(buffer, offset + 4, nameLength);
	}
	
	public void setName(String name) {
		this.name = name;
	}
	
	public String getName() {
		return name;
	}
}
