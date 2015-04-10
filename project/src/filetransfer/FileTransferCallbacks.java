package filetransfer;

public interface FileTransferCallbacks {
	void onFileTransferRequest(FTHandle handle);
	void onFileTransferStarted(FTHandle handle);
	void onFileTransferRejected(FTHandle handle);
	void onFileTransferCompleted(FTHandle handle);
	void onFileTransferFailed(FTHandle handle);
	void onFileTransferProgress(FTHandle handle, float progress); // TODO: Only call this every X steps of progress
	void onFileTransferCancelled(FTHandle handle);
}
