package events;

public final class FTTaskDataEvent extends Event {
	private final long offset;
	private final byte[] data;
	
	public FTTaskDataEvent(long offset, byte[] data) {
		super(Event.TYPE_FTTASK_DATA);
		
		this.offset = offset;
		this.data = data;
	}
	
	public long getOffset() {
		return offset;
	}
	
	public byte[] getData() {
		return data;
	}
}
