package protocol;

public class EmptyPacket extends Packet {

	public EmptyPacket() {
		super(Packet.TYPE_EMPTY);
	}
	
	public EmptyPacket(int type) {
		super(type);
	}

	@Override
	protected byte[] serializeContent() {
		return new byte[0];
	}

	@Override
	protected void deserializeContent(byte[] buffer, int offset, int length) {
	}

}
