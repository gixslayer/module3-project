package gui;

import client.Client;

public interface GUICallbacks {
	void onSendPrivateMessage(Client client, String message, String otherName);
	void onSendMessage(String message);
}
