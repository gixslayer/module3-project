package protocol;

public final class FTCancelPacket extends Packet {
	private int transferId;

	public FTCancelPacket() {
		super(Packet.TYPE_FT_CANCEL);
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
