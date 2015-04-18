package filetransfer;

/**
 * Provides an interface for callback methods called by the file transfer manager.
 * @author ciske
 *
 */
public interface FileTransferCallbacks {
	/**
	 * Called when a new file transfer request has been received.
	 * @param handle The handle of this file transfer request.
	 */
	void onFileTransferRequest(FileTransferHandle handle);
	/**
	 * Called when an existing file transfer request has been accepted and has now started.
	 * @param handle The handle of the file transfer.
	 */
	void onFileTransferStarted(FileTransferHandle handle);
	/**
	 * Called when an existing file transfer request has been rejected. 
	 * @param handle The handle of the rejected file transfer. It still contains data, but is no longer considered valid by the manager.
	 */
	void onFileTransferRejected(FileTransferHandle handle);
	/**
	 * Called when an existing file transfer has been completed. This is called for both the receiver and sender.
	 * @param handle The handle of the file transfer which just completed. It still contains data, but is no longer considered valid by the manager.
	 */
	void onFileTransferCompleted(FileTransferHandle handle);
	/**
	 * Called when a file transfer request has failed. This may be called for both the receiver and sender.
	 * If a new file transfer request is made, but fails (EG the input file could not be opened for reading) this method is called for the sender and no request is send.
	 * If a reply to an existing file transfer request is processed, but fails (EG the output file not could be opened for writing) this method is called for the receiver 
	 * and the receiver will automatically send a reject to the sender.
	 * If at any point a file transfer fails (EG read/write IO exceptions) this method is called for both the receiver and sender.
	 * @param handle The handle of the failed file transfer. It still contains data, but is no longer considered valid by the manager.
	 * @param reason The reason why the file transfer failed.
	 */
	void onFileTransferFailed(FileTransferHandle handle, String reason);
	/**
	 * Called when an existing file transfer has made progress. This is called for both the receiver and sender. 
	 * @param handle The handle of the file transfer.
	 * @param progress The new progress of the file transfer as a floating point value between 0 and 100 (inclusive).
	 */
	void onFileTransferProgress(FileTransferHandle handle, float progress);
	/**
	 * Called when an existing file transfer has been cancelled by either the sender or receiver. This is called for both the receiver and sender. 
	 * @param handle The handle of the file transfer which has been cancelled. It still contains data, but is no longer considered valid by the manager.
	 */
	void onFileTransferCancelled(FileTransferHandle handle);
}
