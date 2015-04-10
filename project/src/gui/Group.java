package gui;

import java.util.ArrayList;

import client.Client;

public class Group {
	private String name;
	private ArrayList<Client> clientList = new ArrayList<Client>();
	
	public Group(String name) {
		setName(name);
	}
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	
	public boolean isPartOfGroup(Client client) {
		return clientList.contains(client);
	}
	
	public void joinGroup(Client client) {
		clientList.add(client);
	}
	
	public void leaveGroup(Client client) {
		clientList.remove(client);
	}
}
