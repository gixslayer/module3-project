package protocol;

import utils.ByteUtils;

public final class FTCompletedPacket extends Packet {
	private int requestId;
	
	public FTCompletedPacket() {
		this(0);
	}
	
	public FTCompletedPacket(int requestId) {
		super(Packet.TYPE_FT_COMPLETED);
		
		this.requestId = requestId;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] buffer = new byte[4];
		
		ByteUtils.getIntBytes(requestId, buffer, 0);
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		requestId = ByteUtils.getIntFromBytes(buffer, offset);		
	}

	public int getRequestId() {
		return requestId;
	}
}
