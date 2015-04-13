package protocol;

import utils.ByteUtils;

public final class FTReplyPacket extends Packet {
	private int requestId;
	private int transferId;
	private boolean response;
	
	public FTReplyPacket() {
		super(Packet.TYPE_FT_REPLY);
	}
	
	public FTReplyPacket(int requestId, int transferId, boolean response) {
		super(Packet.TYPE_FT_REPLY);
		
		System.out.println("SDSDSDSDS: " + response);
		
		this.requestId = requestId;
		this.transferId = transferId;
		this.response = response;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] buffer = new byte[9];
		
		ByteUtils.getIntBytes(transferId, buffer, 0);
		ByteUtils.getIntBytes(requestId, buffer, 4);
		buffer[8] = response ? (byte)0xff : (byte)0x0;
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		transferId = ByteUtils.getIntFromBytes(buffer, offset);
		requestId = ByteUtils.getIntFromBytes(buffer, offset + 4);
		response = (buffer[offset + 8] & 0xFF) == 0xff;
		
	}

	public int getRequestId() {
		return requestId;
	}
	
	public int getTransferId() {
		return transferId;
	}
	
	public boolean getResponse() {
		System.out.println("SDSDSDSDSDSDSDSDSDS:  " + response);
		return response;
	}
}
