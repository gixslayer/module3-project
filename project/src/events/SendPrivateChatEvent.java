package events;

import client.Client;

public final class SendPrivateChatEvent extends Event {
	private final Client client;
	private final String message;
	
	public SendPrivateChatEvent(Client client, String message) {
		super(Event.TYPE_SEND_PRIVATE_CHAT);
		
		this.client = client;
		this.message = message;
	}
	
	public Client getClient() {
		return client;
	}
	
	public String getMessage() {
		return message;
	}
}
