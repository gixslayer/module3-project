package application;

import client.Client;

public interface ApplicationCallbacks {
	void onClientConnected(Client client);
	void onClientDisconnected(Client client);
	void onClientTimedOut(Client client);
	void onClientLostRoute(Client client);
	
	void onChatMessageReceived(Client client, String message);
	void onPrivateChatMessageReceived(Client client, String message);
}
