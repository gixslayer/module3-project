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
import protocol.FTFailedPacket;
import protocol.FTReplyPacket;
import protocol.FTRequestPacket;
import protocol.Packet;
import network.NetworkInterface;
import client.Client;
import events.EventQueue;
import events.FTTaskCancelledEvent;
import events.FTTaskCompletedEvent;
import events.FTTaskFailedEvent;
import events.FTTaskProgressEvent;
import events.SendReliablePacketEvent;

public final class FileTransfer {
	private final FileTransferCallbacks callbacks;
	private final NetworkInterface networkInterface;
	private final EventQueue eventQueue;
	private final Map<Integer, FileTransferHandle> outgoingRequests;
	private final Map<Integer, FileTransferHandle> incomingTransfers;
	private final Map<Integer, FileTransferHandle> outgoingTransfers;
	private final Map<Integer, TransferTask> activeTasks;
	private final Client localClient;
	private int nextRequestId;
	private int nextTransferId;
	
	public FileTransfer(FileTransferCallbacks callbacks, NetworkInterface networkInterface, EventQueue eventQueue) {
		this.callbacks = callbacks;
		this.networkInterface = networkInterface;
		this.eventQueue = eventQueue;
		this.outgoingRequests = new HashMap<Integer, FileTransferHandle>();
		this.incomingTransfers = new HashMap<Integer, FileTransferHandle>();
		this.outgoingTransfers = new HashMap<Integer, FileTransferHandle>();
		this.activeTasks = new HashMap<Integer, TransferTask>();
		this.localClient = networkInterface.getLocalClient();
		this.nextRequestId = 0;
		this.nextTransferId = 0;
	}
	
	//-------------------------------------------
	// File transfer related event handlers.
	//-------------------------------------------
	public void createRequest(Client receiver, String filePath) {
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
			// If the input file could not be opened inform the sender and do not send out the request.
			FileTransferHandle handle = FileTransferHandle.createRequestHandle(0, sender, receiver, fileName, fileSize, null);
			callbacks.onFileTransferFailed(handle, String.format("Could not open input file %s -> %s", inputFile.getAbsolutePath(), e.getMessage()));
			return;
		}
		
		FileTransferHandle handle = FileTransferHandle.createRequestHandle(requestId, sender, receiver, fileName, fileSize, inputStream);
		FTRequestPacket requestPacket = new FTRequestPacket(requestId, fileName, fileSize, sender);
		
		outgoingRequests.put(requestId, handle);
		
		sendPacket(receiver, requestPacket);
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
				// We failed to open an output stream to store the to be received file content so reject the transfer.
				replyPacket = new FTReplyPacket(requestId, transferId, false);
				
				callbacks.onFileTransferFailed(handle, String.format("Could not open output file %s -> %s", savePath, e.getMessage()));
			}
			
			if(outputStream != null) {
				handle.start(transferId, outputStream);			
				incomingTransfers.put(transferId, handle);
			}
		}
		
		sendPacket(sender, replyPacket);
	}
	
	public void cancel(FileTransferHandle handle) {
		// Processed by both receiver/sender.
		boolean isReceiver = handle.isIncoming(localClient); 
		int transferId = handle.getTransferId();
		int requestId = handle.getRequestId();
		FTCancelPacket cancelPacket = new FTCancelPacket(transferId, requestId, isReceiver);
		Client destination = isReceiver ? handle.getSender() : handle.getReceiver();
		
		if(isReceiver) { /* Incoming */
			incomingTransfers.remove(transferId);
			
			if(handle.hasStarted()) {
				// TODO: Remove existing file data?
			}
		} else { /* Outgoing */
			outgoingTransfers.remove(requestId);
			
			if(handle.hasStarted()) {
				activeTasks.get(requestId).cancel();
			}
		}
		
		// Close any open file handles.
		handle.close();
		
		// Inform the other client we cancelled the transfer.
		sendPacket(destination, cancelPacket);
		
		callbacks.onFileTransferCancelled(handle);
	}
	
	public void taskCancelled(FileTransferHandle handle) {
		activeTasks.remove(handle.getRequestId());
		outgoingTransfers.remove(handle.getRequestId());
	}
	
	public void taskCompleted(FileTransferHandle handle) {
		activeTasks.remove(handle.getRequestId());
		outgoingTransfers.remove(handle.getRequestId());
		
		callbacks.onFileTransferCompleted(handle);
	}
	
	public void taskFailed(FileTransferHandle handle, String reason) {
		activeTasks.remove(handle.getRequestId());
		outgoingTransfers.remove(handle.getRequestId());
		
		// Send a failed message to receiving client.
		FTFailedPacket packet = new FTFailedPacket(handle.getTransferId(), handle.getRequestId(), false);
		sendPacket(handle.getReceiver(), packet);
		
		callbacks.onFileTransferFailed(handle, reason);
	}
	
	public void taskProgress(FileTransferHandle handle, float progress) {
		callbacks.onFileTransferProgress(handle, progress);
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
		System.out.println("SDSDS: " + response);
		FileTransferHandle handle = outgoingRequests.remove(requestId);
		
		// Ignore if the request id wasn't valid.
		if(handle == null) {
			return;
		}
		
		if(response) {
			handle.start(transferId);
			TransferTask transferTask = new TransferTask(handle);
			outgoingTransfers.put(requestId, handle);
			activeTasks.put(requestId, transferTask);
			
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
		FileTransferHandle handle = incomingTransfers.get(transferId);
		
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
			// Failed file transfer as received data could not be written to output file. End the file transfer and inform the other client
			// the transfer has failed.
			FTFailedPacket failedPacket = new FTFailedPacket(transferId, handle.getRequestId(), true);
			sendPacket(handle.getSender(), failedPacket);
			
			incomingTransfers.remove(transferId);
			
			if(handle.hasStarted()) {
				// TODO: Remove existing file data?				
			}
			
			// Close file handles.
			handle.close();
			
			callbacks.onFileTransferFailed(handle, String.format("Could not write received data -> %s", e.getMessage()));
			return;
		}
		
		handle.receivedBytes(data.length);
		float progress = ((float)handle.getBytesReceived() / (float)handle.getFileSize()) * 100.0f;
		
		// TODO: Limit how often we call this (EG min 10% increase over last callback).
		callbacks.onFileTransferProgress(handle, progress);
		
		if(handle.hasCompleted()) {
			incomingTransfers.remove(transferId);
			
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
			handle = outgoingTransfers.remove(requestId);
			
			// Make sure the transfer was actually active.
			if(handle == null) {
				return;
			}
			
			if(handle.hasStarted()) {
				activeTasks.get(requestId).cancel();
			}
		} else { /* We are the receiver */
			handle = incomingTransfers.remove(transferId);
			
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
	
	public void onFailedPacketReceived(FTFailedPacket packet) {
		// Processed by both receiver/sender.
		boolean hasReceiverFailed = packet.hasReceiverFailed();
		int requestId = packet.getRequestId();
		int transferId = packet.getTransferId();
		FileTransferHandle handle;
				
		if(hasReceiverFailed) { /* We are the sender */
			handle = outgoingTransfers.remove(requestId);
					
			// Make sure the transfer was actually active.
			if(handle == null) {
				return;
			}
					
			if(handle.hasStarted()) {
				activeTasks.get(requestId).cancel();
			}
		} else { /* We are the receiver */
			handle = incomingTransfers.remove(transferId);
					
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
				
		callbacks.onFileTransferFailed(handle, "Remote user indicated failure");
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
	
	private void sendPacket(Client destination, Packet packet) {
		networkInterface.sendReliableTo(destination, packet);
	}
	
	private void sendPacketFromTask(Client destination, Packet packet) {
		// This is called from a transfer task thread so push a new event on the event queue so the backend thread processes it.
		eventQueue.enqueue(new SendReliablePacketEvent(destination, packet));
	}
	
	//-------------------------------------------
	// Transfer task class.
	//-------------------------------------------
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
			String exceptionMessage = null;
			int bytesRead = 0;
			
			while(!taskCancelled && !taskFailed) {
				// TODO: Some kind of 'rate limit' to prevent flooding TCP stack with packets which could cause a massive outgoing delay of other data.
				// Perhaps a priority queue in the TCP stack is another solution.
				try {
					bytesRead = handle.getInputStream().read(buffer);
				} catch (IOException e) {
					taskFailed = true;
					exceptionMessage = e.getMessage();
					break;
				}
				
				if(bytesRead == -1) {
					// End of stream, task completed.
					break;
				}
				
				FTDataPacket dataPacket = new FTDataPacket(transferId, offset, buffer, bytesRead);
				offset += bytesRead;
				float progress = ((float)offset / (float)size) * 100.0f;
				
				sendPacketFromTask(destination, dataPacket);
				
				// TODO: Limit how often we call this (EG min 10% increase over last callback).
				eventQueue.enqueue(new FTTaskProgressEvent(handle, progress));
			}
			
			// Close file handles.
			handle.close();
			
			if(taskFailed) {
				eventQueue.enqueue(new FTTaskFailedEvent(handle, String.format("Could not read from input file -> %s", exceptionMessage)));
			} else if(taskCancelled) {
				eventQueue.enqueue(new FTTaskCancelledEvent(handle));
			} else {
				eventQueue.enqueue(new FTTaskCompletedEvent(handle));
			}
		}
		
	}
}
