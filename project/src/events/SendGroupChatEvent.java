package events;

public final class SendGroupChatEvent extends Event {
	private final String group;
	private final String message;
	
	public SendGroupChatEvent(String group, String message) {
		super(Event.TYPE_SEND_GROUP_CHAT);
		
		this.group = group;
		this.message = message;
	}
	
	public String getGroup() {
		return group;
	}
	
	public String getMessage() {
		return message;
	}
}
