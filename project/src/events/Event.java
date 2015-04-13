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
	public static final int TYPE_SEND_PACKET = 9;
	public static final int TYPE_SEND_RELIABLE_PACKET = 10;
	public static final int TYPE_FTTASK_FAILED = 11;
	public static final int TYPE_FTTASK_CANCELLED = 12;
	public static final int TYPE_FTTASK_COMPLETED = 13;
	public static final int TYPE_FTTASK_PROGRESS = 14;
	public static final int TYPE_FTTASK_DATA = 15;
	
	private final int type;
	
	public Event(int type) {
		this.type = type;
	}
	
	public int getType() {
		return type;
	}
}
