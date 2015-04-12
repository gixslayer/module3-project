package events;

public final class SendChatEvent extends Event {
	private final String message;
	
	public SendChatEvent(String message) {
		super(Event.TYPE_SEND_CHAT);
		
		this.message = message;
	}
	
	public String getMessage() {
		return message;
	}
}
