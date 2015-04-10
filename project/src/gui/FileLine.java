package gui;

import client.Client;

public class FileLine implements ChatLine {
	private Client sendClient;
	private Client receiveClient;
	private String fileName;
	private String line;
	private float progress;
	
	public FileLine(Client sendClient, Client receiveClient, String fileName, float progress) {
		this.sendClient = sendClient;
		this.receiveClient = receiveClient;
		this.fileName = fileName;
		this.progress = progress;
		int prog = ((int)progress/10)*10;
		this.line = "FILE TRANSFER " + fileName + ": " + sendClient.getName() + "*right*" + receiveClient.getName() + " | *" + prog + "p*";
	
	}
	
	@Override
	public Client getClient() {
		return sendClient;
	}

	@Override
	public void setClient(Client client) {
		this.sendClient = client;
	}

	@Override
	public String getLine() {
		return line;
	}

	@Override
	public void setLine(String line) {
		this.line = line;
	}

}
