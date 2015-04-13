package gui;

import client.Client;

public class FileLine implements ChatLine {
	private String time;
	private Client sendClient;
	private Client receiveClient;
	private String fileName;
	private String line;
	
	public FileLine(Client sendClient, Client receiveClient, String fileName, float progress) {
		this.sendClient = sendClient;
		this.receiveClient = receiveClient;
		this.fileName = fileName;
		this.line = "FILE TRANSFER " + fileName + ": " + sendClient.getName() + "*right*" + receiveClient.getName() + " | *" + (((int)progress/10)*10) + "p*";
	}
	
	public FileLine(String time, Client sendClient, Client receiveClient, String fileName, float progress) {
		this.sendClient = sendClient;
		this.receiveClient = receiveClient;
		this.fileName = fileName;
		this.line = "FILE TRANSFER " + fileName + ": " + sendClient.getName() + "*right*" + receiveClient.getName() + " | *" + (((int)progress/10)*10) + "p*";
		this.time = time;
	}
	
	@Override
	public String getTime() {
		if(time != null) 
			return time;
		else 
			return "--:--:--";
	}
	
	@Override
	public void setTime(String time) {
		this.time = time;
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

	public String getFileName() {
		return fileName;
	}
}
