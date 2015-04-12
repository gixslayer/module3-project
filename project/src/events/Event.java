package events;

public abstract class Event {
	public static final int TYPE_UNICAST_PACKET_RECEIVED = 0;
	public static final int TYPE_MULTICAST_PACKET_RECEIVED = 1;
	
	private final int type;
	
	public Event(int type) {
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
}
