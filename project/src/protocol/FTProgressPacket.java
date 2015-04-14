package protocol;

import utils.ByteUtils;

public final class FTProgressPacket extends Packet {
	private int requestId;
	private float progress;
	
	public FTProgressPacket() {
		this(0, 0.0f);
	}
	
	public FTProgressPacket(int requestId, float progress) {
		super(Packet.TYPE_FT_PROGRESS);
		
		this.requestId = requestId;
		this.progress = progress;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] buffer = new byte[8];
		int intProgress = Float.floatToIntBits(progress);
		
		ByteUtils.getIntBytes(requestId, buffer, 0);
		ByteUtils.getIntBytes(intProgress, buffer, 4);
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		requestId = ByteUtils.getIntFromBytes(buffer, offset);
		int intProgress = ByteUtils.getIntFromBytes(buffer, offset + 4);
		progress = Float.intBitsToFloat(intProgress);
	}

	public int getRequestId() {
		return requestId;
	}
	
	public float getProgress() {
		return progress;
	}
}
