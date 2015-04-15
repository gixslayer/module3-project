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
	
	@Override
	public int hashCode() {
		return name.hashCode();
	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj == null) return false;
		if(!(obj instanceof Group)) return false;
		
		Group other = (Group)obj;
		
		return this.name.equals(other.name);
	}
}
