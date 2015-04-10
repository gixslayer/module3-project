package filetransfer;

import client.Client;

public final class FTHandle {
	private int transferId;
	private int requestId;
	private String fileName;
	private long fileSize;
	private boolean hasStarted;
	private Client sender;
	private Client receiver;
	private String savePath;
	
	public FTHandle() {
		
	}
	
	public int getTransferId() {
		return transferId;
	}
	
	public int getRequestId() {
		return requestId;
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
