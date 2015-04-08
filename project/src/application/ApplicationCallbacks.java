package application;

import client.Client;

public interface ApplicationCallbacks {
	void onClientConnected(Client client);
	void onClientDisconnected(Client client);
	void onClientTimedOut(Client client);
	void onClientLostRoute(Client client);
	
	void onChatMessageReceived(String user, String message);
	void onPrivateChatMessageReceived(String user, String message);
}
