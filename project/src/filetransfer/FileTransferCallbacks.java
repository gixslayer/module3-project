package filetransfer;

public interface FileTransferCallbacks {
	void onFileTransferRequest(FileTransferHandle handle);
	void onFileTransferStarted(FileTransferHandle handle);
	void onFileTransferRejected(FileTransferHandle handle);
	void onFileTransferCompleted(FileTransferHandle handle);
	void onFileTransferFailed(FileTransferHandle handle);
	void onFileTransferProgress(FileTransferHandle handle, float progress); // TODO: Only call this every X steps of progress
	void onFileTransferCancelled(FileTransferHandle handle);
}
