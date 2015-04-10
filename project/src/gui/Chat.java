package gui;

import client.Client;

public interface Chat {
	public abstract void init();
	public void addToScreen(Client client, String str);
	public void sendText();
	public void receiveText(String str, Client client);
	public boolean isPrivate();
}
