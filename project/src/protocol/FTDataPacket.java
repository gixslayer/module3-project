package protocol;

import utils.ByteUtils;

public final class FTDataPacket extends Packet {
	private int transferId;
	private long offset;
	private byte[] data;
	private final int dataLength;
	
	public FTDataPacket() {
		this(-1, 0, null, 0);
	}
	
	public FTDataPacket(int transferId, long offset, byte[] data, int dataLength) {
		super(Packet.TYPE_FT_DATA);
		
		this.transferId = transferId;
		this.offset = offset;
		this.data = data;
		this.dataLength = dataLength;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] buffer = new byte[12 + dataLength];
		
		ByteUtils.getIntBytes(transferId, buffer, 0);
		ByteUtils.getLongBytes(offset, buffer, 4);
		System.arraycopy(data, 0, buffer, 12, dataLength);
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		transferId = ByteUtils.getIntFromBytes(buffer, offset);
		this.offset = ByteUtils.getLongFromBytes(buffer, offset + 4);
		data = new byte[length - 12];
		System.arraycopy(buffer, offset + 12, data, 0, data.length);
	}

	public int getTransferId() {
		return transferId;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public byte[] getData() {
		return data;
	}
}
