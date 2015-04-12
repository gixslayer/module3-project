package backend;

import client.Client;
import filetransfer.FileTransferCallbacks;

public interface BackendCallbacks extends FileTransferCallbacks {
	void onClientConnected(Client client);
	void onClientDisconnected(Client client);
	void onClientTimedOut(Client client);
	void onClientLostRoute(Client client);
	
	void onChatMessageReceived(Client client, String message);
	void onPrivateChatMessageReceived(Client client, String message);
	void onGroupChatMessageReceived(Client client, String groupName, String message);
	void onPokePacketReceived(Client client);
}
