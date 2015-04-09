package protocol;

import utils.ByteUtils;
import client.Client;

public final class RouteRequestPacket extends Packet {
	private Client src;
	private Client dest;
	private byte[] data;
	
	public RouteRequestPacket() {
		this(new Client(), new Client(), null);
	}
	
	public RouteRequestPacket(Client src, Client dest, byte[] data) {
		super(Packet.TYPE_ROUTE_REQUEST);

		this.src = src;
		this.dest = dest;
		this.data = data;
	}

	@Override
	protected byte[] serializeContent() {
		byte[] srcBytes = src.serialize(Client.SERIALIZE_ADDRESS);
		byte[] destBytes = dest.serialize(Client.SERIALIZE_ADDRESS);
		byte[] buffer = new byte[4 + srcBytes.length + destBytes.length + data.length];
		
		ByteUtils.getIntBytes(data.length, buffer, 0);
		System.arraycopy(srcBytes, 0, buffer, 4, srcBytes.length);
		System.arraycopy(destBytes, 0, buffer, 4 + srcBytes.length, destBytes.length);
		System.arraycopy(data, 0, buffer, 4 + srcBytes.length + destBytes.length, data.length);
		
		return buffer;
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
		int dataLength = ByteUtils.getIntFromBytes(buffer, offset);
		
		offset += 4;
		offset += src.deserialize(buffer, offset);
		offset += dest.deserialize(buffer, offset);
		data = new byte[dataLength];
		
		System.arraycopy(buffer, offset, data, 0, dataLength);
	}
	
	public Client getSrc() {
		return src;
	}
	
	public Client getDest() {
		return dest;
	}
	
	public byte[] getData() {
		return data;
	}
}
