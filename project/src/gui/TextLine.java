package gui;

import client.Client;

public class TextLine implements ChatLine {
	private String time;
	private Client client;
	private String line;
	
	public TextLine(Client client, String line) {
		this.setClient(client);
		this.setLine(line);
	}
	
	public TextLine(String time, Client client, String line) {
		this.time = time;
		this.client = client;
		this.line = line;
	}
	
	public String getTime() {
		if(time != null) 
			return time;
		else 
			return "--:--:--";
	}
	
	public void setTime(String time) {
		this.time = time;
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
