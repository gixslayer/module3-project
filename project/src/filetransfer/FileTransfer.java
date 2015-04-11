package filetransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import protocol.FTCancelPacket;
import protocol.FTDataPacket;
import protocol.FTReplyPacket;
import protocol.FTRequestPacket;
import network.NetworkInterface;
import client.Client;

public final class FileTransfer {
	private final FileTransferCallbacks callbacks;
	private final NetworkInterface networkInterface;
	private final Map<Integer, FileTransferHandle> outgoingRequests;
	private final Map<Integer, FileTransferHandle> incomingTransfers;
	private final Map<Integer, FileTransferHandle> outgoingTransfers;
	private final Map<Integer, TransferTask> activeTasks;
	private final Client localClient;
	private int nextRequestId;
	private int nextTransferId;
	
	public FileTransfer(FileTransferCallbacks callbacks, NetworkInterface networkInterface) {
		this.callbacks = callbacks;
		this.networkInterface = networkInterface;
		this.outgoingRequests = new HashMap<Integer, FileTransferHandle>();
		this.incomingTransfers = new HashMap<Integer, FileTransferHandle>();
		this.outgoingTransfers = new HashMap<Integer, FileTransferHandle>();
		this.activeTasks = new HashMap<Integer, TransferTask>();
		this.localClient = networkInterface.getLocalClient();
		this.nextRequestId = 0;
		this.nextTransferId = 0;
	}
	
	//-------------------------------------------
	// Called by GUI.
	//-------------------------------------------
	public FileTransferHandle createRequest(Client receiver, String filePath) {
		// Always processed as sender.
		int requestId = getNextRequestId();
		Client sender = localClient;
		File inputFile = new File(filePath);
		String fileName = inputFile.getName();
		long fileSize = inputFile.length();
		FileInputStream inputStream;
		
		try {
			inputStream = new FileInputStream(inputFile);
		} catch (FileNotFoundException e) {
			// The GUI should never pass a filePath that is invalid, if it does then blow up.
			throw new RuntimeException(String.format("Could not find file %s", inputFile.getAbsolutePath()), e);
		}
		
		FileTransferHandle handle = FileTransferHandle.createRequestHandle(requestId, sender, receiver, fileName, fileSize, inputStream);
		FTRequestPacket requestPacket = new FTRequestPacket(requestId, fileName, fileSize, sender);
		
		synchronized(outgoingRequests) {
			outgoingRequests.put(requestId, handle);
		}
		
		networkInterface.sendReliableTo(receiver, requestPacket);
		
		return handle;
	}
	
	public void sendReply(FileTransferHandle handle, boolean response, String savePath) {
		// Always processed as receiver;
		int transferId = response ? getNextTransferId() : -1;
		int requestId = handle.getRequestId();
		Client sender = handle.getSender();
		FTReplyPacket replyPacket = new FTReplyPacket(requestId, transferId, response);
		
		if(response) {
			FileOutputStream outputStream = null;
			
			try {
				outputStream = new FileOutputStream(savePath);
			} catch (FileNotFoundException e) {
				// We failed to open an output stream to store the to be received file content
				// so reject the transfer.
				replyPacket = new FTReplyPacket(requestId, transferId, false);
				
				// TODO: Use a different callback for this to be more explicit?
				callbacks.onFileTransferFailed(handle);
			}
			
			if(outputStream != null) {
				handle.start(transferId, outputStream);
			
				synchronized(incomingTransfers) {
					incomingTransfers.put(transferId, handle);
				}
			}
		}
		
		networkInterface.sendReliableTo(sender, replyPacket);
	}
	
	public void cancel(FileTransferHandle handle) {
		// Processed by both receiver/sender.
		boolean isReceiver = handle.isIncoming(localClient); 
		int transferId = handle.getTransferId();
		int requestId = handle.getRequestId();
		FTCancelPacket cancelPacket = new FTCancelPacket(transferId, requestId, isReceiver);
		Client destination = isReceiver ? handle.getSender() : handle.getReceiver();
		
		if(isReceiver) { /* Incoming */
			synchronized(incomingTransfers) {
				incomingTransfers.remove(transferId);
			}
			
			if(handle.hasStarted()) {
				// TODO: Remove existing file data?
			}
			
		} else { /* Outgoing */
			synchronized(outgoingTransfers) {
				outgoingTransfers.remove(requestId);
			}
			
			if(handle.hasStarted()) {
				synchronized(activeTasks) {
					activeTasks.get(requestId).cancel();
				}
			}
		}
		
		// Close any open file handles.
		handle.close();
		
		// Inform the other client we cancelled the transfer.
		networkInterface.sendReliableTo(destination, cancelPacket);
		
		callbacks.onFileTransferCancelled(handle);
	}
	
	//-------------------------------------------
	// Called when packets arrive.
	//-------------------------------------------
	public void onRequestPacketReceived(FTRequestPacket packet) {
		// Always processed as receiver.
		int requestId = packet.getRequestId();
		Client sender = packet.getSender();
		Client receiver = localClient;
		String fileName = packet.getFileName();
		long fileSize = packet.getFileSize();
		FileTransferHandle handle = FileTransferHandle.createRequestHandle(requestId, sender, receiver, fileName, fileSize);
		
		callbacks.onFileTransferRequest(handle);
	}
	
	public void onReplyPacketReceived(FTReplyPacket packet) {
		// Always processed as sender.
		int requestId = packet.getRequestId();
		int transferId = packet.getTransferId();
		boolean response = packet.getResponse();
		FileTransferHandle handle;
		
		synchronized(outgoingRequests) {
			handle = outgoingRequests.remove(requestId);
		}
		
		// Ignore if the request id wasn't valid.
		if(handle == null) {
			return;
		}
		
		if(response) {
			handle.start(transferId);

			synchronized(outgoingTransfers) {
				outgoingTransfers.put(requestId, handle);
			}
			
			TransferTask transferTask = new TransferTask(handle);
			
			synchronized(activeTasks) {
				activeTasks.put(requestId, transferTask);
			}
			
			transferTask.start();
			
			callbacks.onFileTransferStarted(handle);
		} else {
			// Close file handles.
			handle.close();
			
			callbacks.onFileTransferRejected(handle);
		}
	}
	
	public void onDataPacketReceived(FTDataPacket packet) {
		// Always processed as receiver.
		int transferId = packet.getTransferId();
		long offset = packet.getOffset();
		byte[] data = packet.getData();
		FileTransferHandle handle;
		
		synchronized(incomingTransfers) {
			handle = incomingTransfers.get(transferId);
		}
		
		// Ignore if the transfer id wasn't valid.
		if(handle == null) {
			return;
		}
		
		try {
			FileOutputStream outputStream = handle.getOutputStream();
			
			outputStream.getChannel().position(offset);
			outputStream.write(data);
			outputStream.flush();
			
		} catch (IOException e) {
			// TODO: Send a failed message to sending client.
			
			synchronized(incomingTransfers) {
				incomingTransfers.remove(transferId);
			}
			
			// Close file handles.
			handle.close();
			
			callbacks.onFileTransferFailed(handle);
		}
		
		handle.receivedBytes(data.length);
		float progress = ((float)handle.getBytesReceived() / (float)handle.getFileSize()) * 100.0f;
		
		// TODO: Limit how often we call this (EG min 10% increase over last callback).
		callbacks.onFileTransferProgress(handle, progress);
		
		if(handle.hasCompleted()) {
			synchronized(incomingTransfers) {
				incomingTransfers.remove(transferId);
			}
			
			// Close file handles.
			handle.close();
			
			callbacks.onFileTransferCompleted(handle);
		}
	}
	
	public void onCancelPacketReceived(FTCancelPacket packet) {
		// Processed by both receiver/sender.
		boolean hasReceiverCancelled = packet.hasReceiverCancelled();
		int requestId = packet.getRequestId();
		int transferId = packet.getTransferId();
		FileTransferHandle handle;
		
		if(hasReceiverCancelled) { /* We are the sender */
			synchronized(outgoingTransfers) {
				handle = outgoingTransfers.remove(requestId);
			}
			
			// Make sure the transfer was actually active.
			if(handle == null) {
				return;
			}
			
			if(handle.hasStarted()) {
				synchronized(activeTasks) {
					activeTasks.get(requestId).cancel();
				}
			}
		} else { /* We are the receiver */
			synchronized(incomingTransfers) {
				handle = incomingTransfers.remove(transferId);
			}
			
			// Make sure the transfer was actually active.
			if(handle == null) {
				return;
			}
			
			if(handle.hasStarted()) {
				// TODO: Remove existing file data?
			}
		}
		
		// Close file handles.
		handle.close();
		
		callbacks.onFileTransferCancelled(handle);
	}
	
	//-------------------------------------------
	// Private helper methods.
	//-------------------------------------------
	private int getNextRequestId() {
		int result = nextRequestId;
		
		if(nextRequestId == Integer.MAX_VALUE) {
			nextRequestId = 0;
		} else {
			nextRequestId++;
		}
		
		return result;
	}
	
	private int getNextTransferId() {
		int result = nextTransferId;
		
		if(nextTransferId == Integer.MAX_VALUE) {
			nextTransferId = 0;
		} else {
			nextTransferId++;
		}
		
		return result;
	}
	
	class TransferTask extends Thread {
		private volatile boolean taskCancelled;
		private final FileTransferHandle handle;
		
		TransferTask(FileTransferHandle handle) {
			this.taskCancelled = false;
			this.handle = handle;
		}
		
		void cancel() {
			taskCancelled = true;
			try {
				join();
			} catch (InterruptedException e) { }
		}
		
		@Override
		public void run() {
			long offset = 0;
			byte[] buffer = new byte[1024]; // TODO: Determine a proper buffer size.
			int transferId = handle.getTransferId();
			Client destination = handle.getReceiver();
			long size = handle.getFileSize();
			boolean taskFailed = false;
			int bytesRead = 0;
			
			while(!taskCancelled && !taskFailed) {
				try {
					bytesRead = handle.getInputStream().read(buffer);
				} catch (IOException e) {
					taskFailed = true;
					break;
				}
				
				if(bytesRead == -1) {
					// End of stream, task completed.
					break;
				}
				
				FTDataPacket dataPacket = new FTDataPacket(transferId, offset, buffer, bytesRead);
				offset += bytesRead;
				float progress = ((float)offset / (float)size) * 100.0f;
				
				networkInterface.sendReliableTo(destination, dataPacket);
				
				// TODO: Limit how often we call this (EG min 10% increase over last callback).
				callbacks.onFileTransferProgress(handle, progress);
			}
			
			activeTasks.remove(handle.getRequestId());
			
			// Close file handles.
			handle.close();
			
			if(!taskCancelled && !taskFailed) {
				callbacks.onFileTransferCompleted(handle);
			}
			
			if(taskFailed) {
				// TODO: Send a failed message to receiving client.
				
				synchronized(outgoingTransfers) {
					outgoingTransfers.remove(handle.getRequestId());
				}
				
				callbacks.onFileTransferFailed(handle);
			}
		}
		
	}
}
