package filetransfer;

import java.util.HashMap;
import java.util.Map;

import network.NetworkInterface;
import client.Client;

public final class FileTransfer {
	private final FileTransferCallbacks callbacks;
	private final NetworkInterface networkInterface;
	private final Map<Integer, FTHandle> openRequests;
	private final Map<Integer, FTHandle> activeTransfers;
	private int nextRequestId;
	
	public FileTransfer(FileTransferCallbacks callbacks, NetworkInterface networkInterface) {
		this.callbacks = callbacks;
		this.networkInterface = networkInterface;
		this.openRequests = new HashMap<Integer, FTHandle>();
		this.activeTransfers = new HashMap<Integer, FTHandle>();
		this.nextRequestId = 0;
	}
	
	//-------------------------------------------
	// Called by GUI.
	//-------------------------------------------
	public FTHandle createRequest(Client dest, String filePath) {
		return null;
	}
	
	public void sendReply(FTHandle handle, boolean response, String savePath) {
		
	}
	
	public void cancel(FTHandle handle) {
		
	}
	
	private int getNextRequestId() {
		int result = nextRequestId;
		
		if(nextRequestId == Integer.MAX_VALUE) {
			nextRequestId = 0;
		} else {
			nextRequestId++;
		}
		
		return result;
	}
}
