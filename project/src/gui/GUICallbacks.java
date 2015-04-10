package gui;

import client.Client;
import filetransfer.FTHandle;

public interface GUICallbacks {
	void onSendGroupMessage(String groupName, String message);
	void onSendPrivateMessage(Client otherClient, String message);
	void onSendMessage(String message);
	
	FTHandle onRequestFileTransfer(Client dest, String filePath); // TODO: filePath must be full path to file.
	void onReplyToFileTransfer(FTHandle handle, boolean response, String savePath);
	void onCancelFileTransfer(FTHandle handle);
}
