package gui;

import client.Client;

public class TextLine implements ChatLine {
	private Client client;
	private String line;
	
	public TextLine(Client client, String line) {
		this.setClient(client);
		this.setLine(line);
	}

	public Client getClient() {
		return client;
	}

	public void setClient(Client client) {
		this.client = client;
	}

	public String getLine() {
		return line;
	}

	public void setLine(String line) {
		this.line = line;
	}
}
