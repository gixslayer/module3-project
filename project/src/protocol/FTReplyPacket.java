package protocol;

public final class FTReplyPacket extends Packet {
	private int requestId; // This reply responds to a request with this id.
	private boolean response; // Accept/reject request.
	private int transferId; // Id used for the transfer, 0 if response = false.
	
	public FTReplyPacket() {
		super(Packet.TYPE_FT_REPLY);
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
