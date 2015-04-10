package filetransfer;

import client.Client;

public final class FTHandle {
	private int transferId;
	private int requestId;
	private String fileName;
	private long fileSize;
	private boolean hasStarted;
	private Client otherClient;
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
	
	public Client getOtherClient() {
		return otherClient;
	}
	
	public String getSavePath() {
		return savePath;
	}
}
