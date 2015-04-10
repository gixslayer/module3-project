package gui;

import client.Client;

public interface GUICallbacks {
	void onSendGroupMessage(String groupName, String message);
	void onSendPrivateMessage(Client otherClient, String message);
	void onSendMessage(String message);
}
