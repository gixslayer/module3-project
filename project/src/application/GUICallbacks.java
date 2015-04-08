package application;

import client.Client;

public interface GUICallbacks {
	void onSendPrivateMessage(Client client, String message);
	void onSendMessage(String message);
}
