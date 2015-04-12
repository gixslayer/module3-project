package events;

public abstract class Event {
	public static final int TYPE_UNICAST_PACKET_RECEIVED = 0;
	public static final int TYPE_MULTICAST_PACKET_RECEIVED = 1;
	public static final int TYPE_SEND_GROUP_CHAT = 2;
	public static final int TYPE_SEND_PRIVATE_CHAT = 3;
	public static final int TYPE_SEND_CHAT = 4;
	public static final int TYPE_SEND_POKE = 5;
	public static final int TYPE_REQUEST_FILE_TRANSFER = 6;
	public static final int TYPE_REPLY_TO_FILE_TRANSFER = 7;
	public static final int TYPE_CANCEL_FILE_TRANSFER = 8;
	
	private final int type;
	
	public Event(int type) {
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
}
