package filetransfer;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import client.Client;

public final class FileTransferHandle {
	private int requestId;
	private int transferId;
	private Client sender;
	private Client receiver;
	private String fileName;
	private long fileSize;
	private FileInputStream inputStream;
	private FileOutputStream outputStream;
	private boolean hasStarted;
	private String savePath;
	private long bytesReceived;
	
	private FileTransferHandle(int requestId, int transferId, Client sender, Client receiver, String fileName, long fileSize, 
			FileInputStream inputStream, FileOutputStream outputStream, boolean hasStarted, String savePath, long bytesReceived) {
		this.requestId = requestId;
		this.transferId = transferId;
		this.sender = sender;
		this.receiver = receiver;
		this.fileName = fileName;
		this.fileSize = fileSize;
		this.inputStream = inputStream;
		this.outputStream = outputStream;
		this.hasStarted = hasStarted;		
		this.savePath = savePath;
		this.bytesReceived = bytesReceived;
	}
	
	static FileTransferHandle createRequestHandle(int requestId, Client sender, Client receiver, String fileName, long fileSize, FileInputStream inputStream) {
		return new FileTransferHandle(requestId, -1, sender, receiver, fileName, fileSize, inputStream, null, false, null, 0);
	}
	
	static FileTransferHandle createRequestHandle(int requestId, Client sender, Client receiver, String fileName, long fileSize) {
		return new FileTransferHandle(requestId, -1, sender, receiver, fileName, fileSize, null, null, false, null, 0);
	}
	
	void close() {
		hasStarted = false;
		
		try {
			if(inputStream != null) {
				inputStream.close();
				inputStream = null;
			} else if(outputStream != null) {
				outputStream.close();
				outputStream = null;
			}
		} catch (IOException e) { }
	}
	
	void start(int transferId) {
		this.transferId = transferId;
		this.hasStarted = true;
	}
	
	void start(int transferId, FileOutputStream outputStream) {
		this.transferId = transferId;
		this.outputStream = outputStream;
		this.hasStarted = true;
		this.bytesReceived = 0;
	}
	
	void receivedBytes(int amount) {
		bytesReceived += amount;
	}
	
	int getTransferId() {
		return transferId;
	}
	
	int getRequestId() {
		return requestId;
	}
	
	FileInputStream getInputStream() {
		return inputStream;
	}
	
	FileOutputStream getOutputStream() {
		return outputStream;
	}
	
	long getBytesReceived() {
		return bytesReceived;
	}
	
	boolean hasCompleted() {
		return bytesReceived == fileSize;
	}
	
	boolean isIncoming(Client localClient) {
		return receiver.equals(localClient);
	}
	
	boolean isOutgoing(Client localClient) {
		return sender.equals(localClient);
	}
	
	public String getFileName() {
		return fileName;
	}
	
	public long getFileSize() {
		return fileSize;
	}
	
	public boolean hasStarted() {
		return hasStarted;
	}
	
	public Client getSender() {
		return sender;
	}
	
	public Client getReceiver() {
		return receiver;
	}
	
	public String getSavePath() {
		return savePath;
	}
}
