package application;

import client.Client;
import filetransfer.FTHandle;

public interface ApplicationCallbacks {
	void onClientConnected(Client client);
	void onClientDisconnected(Client client);
	void onClientTimedOut(Client client);
	void onClientLostRoute(Client client);
	
	void onChatMessageReceived(Client client, String message);
	void onPrivateChatMessageReceived(Client client, String message);
	void onGroupChatMessageReceived(Client client, String groupName, String message);
	
	void onFileTransferRequest(FTHandle handle);
	void onFileTransferStarted(FTHandle handle);
	void onFileTransferRejected(FTHandle handle);
	void onFileTransferCompleted(FTHandle handle);
	void onFileTransferFailed(FTHandle handle);
	void onFileTransferProgress(FTHandle handle, float progress); // TODO: Only call this every X steps of progress
	void onFileTransferCancelled(FTHandle handle);
}
