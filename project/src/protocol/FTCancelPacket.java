package protocol;

import utils.ByteUtils;

public final class FTCancelPacket extends Packet {
	private int transferId;
	private int requestId;
	private boolean receiverCancelled;

	public FTCancelPacket() {
		this(-1, -1, false);
	}
	
	public FTCancelPacket(int transferId, int requestId, boolean receiverCancelled) {
		super(Packet.TYPE_FT_CANCEL);
		
		this.transferId = transferId;
		this.requestId = requestId;
		this.receiverCancelled = receiverCancelled;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] buffer = new byte[9];
		
		ByteUtils.getIntBytes(transferId, buffer, 0);
		ByteUtils.getIntBytes(requestId, buffer, 4);
		buffer[8] = receiverCancelled ? (byte)0xff : (byte)0x0;
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		transferId = ByteUtils.getIntFromBytes(buffer, offset);
		requestId = ByteUtils.getIntFromBytes(buffer, offset + 4);
		receiverCancelled = buffer[offset + 8] == 0xff;
	}
	
	public int getTransferId() {
		return transferId;
	}
	
	public int getRequestId() {
		return requestId;
	}

	public boolean hasReceiverCancelled() {
		return receiverCancelled;
	}
}
