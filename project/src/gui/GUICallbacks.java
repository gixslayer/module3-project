package gui;

import client.Client;

public interface GUICallbacks {
	void onSendPrivateMessage(Client otherClient, String message);
	void onSendMessage(String message);
}
