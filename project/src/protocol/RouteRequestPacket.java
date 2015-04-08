package protocol;

public final class RouteRequestPacket extends Packet {
	private String target;
	private byte[] data;
	
	public RouteRequestPacket() {
		this(null, null);
	}
	
	public RouteRequestPacket(String target, byte[] data) {
		super(Packet.TYPE_ROUTE_REQUEST);
		
		this.target = target;
		this.data = data;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] targetBytes = target.getBytes();
		byte[] buffer = new byte[targetBytes.length + data.length + 8];
		
		ByteUtils.getIntBytes(targetBytes.length, buffer, 0);
		ByteUtils.getIntBytes(buffer.length, buffer, 4);
		System.arraycopy(targetBytes, 0, buffer, 8, targetBytes.length);
		System.arraycopy(data, 0, buffer, 8 + targetBytes.length, data.length);
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		int targetLength = ByteUtils.getIntFromBytes(buffer, offset);
		int dataLength = ByteUtils.getIntFromBytes(buffer, offset + 4);
		
		target = new String(buffer, offset + 8, targetLength);
		data = new byte[dataLength];
		
		System.arraycopy(buffer, offset + 8 + targetLength, data, 0, dataLength);
	}
	
	public void setTarget(String target) {
		this.target = target;
	}
	
	public void setData(byte[] data) {
		this.data = data;
	}
	
	public String getTarget() {
		return target;
	}
	
	public byte[] getData() {
		return data;
	}
}
