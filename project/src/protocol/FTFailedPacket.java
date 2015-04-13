package protocol;

import utils.ByteUtils;

public final class FTFailedPacket extends Packet {
	private int transferId;
	private int requestId;
	private boolean receiverFailed;

	public FTFailedPacket() {
		this(-1, -1, false);
	}
	
	public FTFailedPacket(int transferId, int requestId, boolean receiverFailed) {
		super(Packet.TYPE_FT_FAILED);
		
		this.transferId = transferId;
		this.requestId = requestId;
		this.receiverFailed = receiverFailed;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] buffer = new byte[9];
		
		ByteUtils.getIntBytes(transferId, buffer, 0);
		ByteUtils.getIntBytes(requestId, buffer, 4);
		buffer[8] = receiverFailed ? (byte)0xff : (byte)0x0;
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		transferId = ByteUtils.getIntFromBytes(buffer, offset);
		requestId = ByteUtils.getIntFromBytes(buffer, offset + 4);
		receiverFailed = buffer[offset + 8] == 0xff;
		
	}

	public int getTransferId() {
		return transferId;
	}
	
	public int getRequestId() {
		return requestId;
	}

	public boolean hasReceiverFailed() {
		return receiverFailed;
	}
}
