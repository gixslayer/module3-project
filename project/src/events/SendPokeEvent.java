package events;

import client.Client;

public final class SendPokeEvent extends Event {
	private final Client client;
	
	public SendPokeEvent(Client client) {
		super(Event.TYPE_SEND_POKE);
		
		this.client = client;
	}
	
	public Client getClient() {
		return client;
	}
}
