package protocol;

public final class FTRequestPacket extends Packet {
	private String fileName; // File name (no extra path, just name.ext)
	private long fileSize; // File size in bytes
	private int requestId; // Unique id used to reply to this request
	
	public FTRequestPacket() {
		super(Packet.TYPE_FT_REQUEST);
	}

	@Override
	protected byte[] serializeContent() {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		// TODO Auto-generated method stub
		
	}

}
