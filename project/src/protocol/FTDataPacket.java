package protocol;

public final class FTDataPacket extends Packet {
	private long offset;
	private int length;
	private byte[] data;
	
	public FTDataPacket() {
		super(Packet.TYPE_FT_DATA);
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
