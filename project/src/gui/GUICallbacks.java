package gui;

import client.Client;
import filetransfer.FileTransferHandle;

/**
 * Provides an interface for callback methods called by the GUI.
 * @author ciske
 *
 */
public interface GUICallbacks {
	/**
	 * Send a new group chat message to all connected clients. Clients will only display this chat message if they are currently in this group.
	 * @param group The name of the group.
	 * @param message The message to send.
	 */
	void onSendGroupMessage(String group, String message);
	/**
	 * Send a new private chat message to the specified client.
	 * @param client The client to which this private chat message is send.
	 * @param message The message to send.
	 */
	void onSendPrivateMessage(Client client, String message);
	/**
	 * Send a new chat message to all connected clients.
	 * @param message The message to send.
	 */
	void onSendMessage(String message);
	/**
	 * Send a poke to the specified client.
	 * @param client The client to which this poke is send.
	 */
	void onSendPoke(Client client);
	
	/**
	 * Sends a new file transfer request to transfer the specified file to the specified client.
	 * @param client The client which will receive the request to receive the file.
	 * @param filePath The full path of the file to transfer to the client.
	 */
	void onRequestFileTransfer(Client client, String filePath);
	/**
	 * Respond to a file transfer request.
	 * @param handle The handle of the current file transfer.
	 * @param response The response to give. True will accept the request to receive the file while False will reject the request.
	 * @param savePath The full local path at which the file that is received will be stored. If response is False this parameter is ignored and null can be passed.
	 */
	void onReplyToFileTransfer(FileTransferHandle handle, boolean response, String savePath);
	/**
	 * Cancel an active file transfer. Can be called as both sender and receiver.
	 * @param handle The handle of the file transfer to cancel.
	 */
	void onCancelFileTransfer(FileTransferHandle handle);
}
