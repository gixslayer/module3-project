package events;

import client.Client;

public final class RequestFileTransferEvent extends Event {
	private final Client client;
	private final String filePath;
	
	public RequestFileTransferEvent(Client client, String filePath) {
		super(Event.TYPE_REQUEST_FILE_TRANSFER);
		
		this.client = client;
		this.filePath = filePath;
	}
	
	public Client getClient() {
		return client;
	}
	
	public String getFilePath() {
		return filePath;
	}
}
