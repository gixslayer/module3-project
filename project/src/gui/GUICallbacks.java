package gui;

import client.Client;
import filetransfer.FileTransferHandle;

public interface GUICallbacks {
	void onSendGroupMessage(String groupName, String message);
	void onSendPrivateMessage(Client otherClient, String message);
	void onSendMessage(String message);
	
	FileTransferHandle onRequestFileTransfer(Client dest, String filePath); // TODO: filePath must be full path to file.
	void onReplyToFileTransfer(FileTransferHandle handle, boolean response, String savePath);
	void onCancelFileTransfer(FileTransferHandle handle);

	void onSendPoke(Client client);
}
