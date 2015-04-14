package filetransfer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;

import containers.Priority;
import containers.SynchronizedQueue;
import protocol.FTCancelPacket;
import protocol.FTCompletedPacket;
import protocol.FTDataPacket;
import protocol.FTFailedPacket;
import protocol.FTProgressPacket;
import protocol.FTReplyPacket;
import protocol.FTRequestPacket;
import protocol.Packet;
import network.NetworkInterface;
import network.UnicastInterface;
import client.Client;
import events.Event;
import events.FTTaskCancelledEvent;
import events.FTTaskCompletedEvent;
import events.FTTaskFailedEvent;
import events.SendReliablePacketEvent;

public final class FileTransfer {
	private final FileTransferCallbacks callbacks;
	private final NetworkInterface networkInterface;
	private final SynchronizedQueue<Event> eventQueue;
	private final Map<Integer, FileTransferHandle> outgoingRequests;
	private final Map<Integer, FileTransferHandle> incomingTransfers;
	private final Map<Integer, FileTransferHandle> outgoingTransfers;
	private final Map<Integer, TransferTask> activeTasks;
	private final Map<Integer, ReceiveTask> receiveTasks;
	private final Client localClient;
	private int nextRequestId;
	private int nextTransferId;
	
	public FileTransfer(FileTransferCallbacks callbacks, NetworkInterface networkInterface, SynchronizedQueue<Event> eventQueue) {
		this.callbacks = callbacks;
		this.networkInterface = networkInterface;
		this.eventQueue = eventQueue;
		this.outgoingRequests = new HashMap<Integer, FileTransferHandle>();
		this.incomingTransfers = new HashMap<Integer, FileTransferHandle>();
		this.outgoingTransfers = new HashMap<Integer, FileTransferHandle>();
		this.activeTasks = new HashMap<Integer, TransferTask>();
		this.receiveTasks = new HashMap<Integer, ReceiveTask>();
		this.localClient = networkInterface.getLocalClient();
		this.nextRequestId = 0;
		this.nextTransferId = 0;
	}
	
	public void close() {
		for(TransferTask task : activeTasks.values()) {
			task.cancel();
		}
		
		for(ReceiveTask task : receiveTasks.values()) {
			task.cancel();
		}
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
		
		sendPacket(receiver, requestPacket, Priority.Normal);
	}
	
	public void sendReply(FileTransferHandle handle, boolean response, String savePath) {
		// Always processed as receiver;
		int transferId = response ? getNextTransferId() : -1;
		int requestId = handle.getRequestId();
		Client sender = handle.getSender();
		FTReplyPacket replyPacket = new FTReplyPacket(requestId, transferId, response);
		
		if(response) {
			RandomAccessFile outputStream = null;
			
			try {
				outputStream = new RandomAccessFile(savePath, "rw");
			} catch (FileNotFoundException e) {
				// We failed to open an output stream to store the to be received file content so reject the transfer.
				replyPacket = new FTReplyPacket(requestId, transferId, false);
				
				callbacks.onFileTransferFailed(handle, String.format("Could not open output file %s -> %s", savePath, e.getMessage()));
			}
			
			if(outputStream != null) {
				handle.start(transferId, outputStream);			
				ReceiveTask task = new ReceiveTask(handle);
				incomingTransfers.put(transferId, handle);
				receiveTasks.put(transferId, task);
				
				task.start();
				
				callbacks.onFileTransferStarted(handle);
			}
		}
		
		sendPacket(sender, replyPacket, Priority.Normal);
	}
	
	public void cancel(final FileTransferHandle handle) {
		// Processed by both receiver/sender.
		boolean isReceiver = handle.isIncoming(localClient); 
		int transferId = handle.getTransferId();
		int requestId = handle.getRequestId();
		FTCancelPacket cancelPacket = new FTCancelPacket(transferId, requestId, isReceiver);
		Client destination = isReceiver ? handle.getSender() : handle.getReceiver();
		
		if(isReceiver) { /* Incoming */
			incomingTransfers.remove(transferId);
			
			if(handle.hasStarted()) {
				receiveTasks.get(transferId).cancel();
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
		sendPacket(destination, cancelPacket, Priority.High);
		
		(new Thread() {
			public void run() {
				callbacks.onFileTransferCancelled(handle);
			}
		}).start();
	}
	
	public void taskCancelled(FileTransferHandle handle) {
		if(handle.isOutgoing(localClient)) {
			activeTasks.remove(handle.getRequestId());
			outgoingTransfers.remove(handle.getRequestId());
		} else {
			receiveTasks.remove(handle.getTransferId());
			incomingTransfers.remove(handle.getTransferId());
		}
	}
	
	public void taskCompleted(final FileTransferHandle handle) {
		if(handle.isOutgoing(localClient)) {
			activeTasks.remove(handle.getRequestId());
		} else {
			receiveTasks.remove(handle.getTransferId());
			incomingTransfers.remove(handle.getTransferId());
			
			(new Thread() {
				public void run() {
					callbacks.onFileTransferCompleted(handle);				
				}
			}).start();
		}
	}
	
	public void taskFailed(final FileTransferHandle handle, final String reason) {
		if(handle.isOutgoing(localClient)) {
			activeTasks.remove(handle.getRequestId());
			outgoingTransfers.remove(handle.getRequestId());
		} else {
			receiveTasks.remove(handle.getTransferId());
			incomingTransfers.remove(handle.getTransferId());
		}
		
		// Send a failed message to other client.
		boolean isReceiver = handle.isIncoming(localClient);
		Client destination = isReceiver ? handle.getSender() : handle.getReceiver();
		FTFailedPacket packet = new FTFailedPacket(handle.getTransferId(), handle.getRequestId(), isReceiver);
		sendPacket(destination, packet, Priority.High);
		
		(new Thread() {
			public void run() {
				callbacks.onFileTransferFailed(handle, reason);
			}
		}).start();
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
		final FileTransferHandle handle = FileTransferHandle.createRequestHandle(requestId, sender, receiver, fileName, fileSize);
		
		(new Thread() {
			public void run() {
				callbacks.onFileTransferRequest(handle);							
			};
		}).start();
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
		ReceiveTask task = receiveTasks.get(transferId);
		
		// Ignore if the transfer id wasn't valid.
		if(task == null) {
			return;
		}
	
		task.queueData(offset, data);
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
				receiveTasks.get(transferId).cancel();
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
				receiveTasks.get(transferId).cancel();
			}
		}
				
		// Close file handles.
		handle.close();
				
		callbacks.onFileTransferFailed(handle, "Remote user indicated failure");
	}
	
	public void onProgressPacketReceived(FTProgressPacket packet) {
		final FileTransferHandle handle = outgoingTransfers.get(packet.getRequestId());
		
		if(handle == null) {
			return;
		}
		
		callbacks.onFileTransferProgress(handle, packet.getProgress());
	}
	
	public void onCompletedPacketReceived(FTCompletedPacket packet) {
		final FileTransferHandle handle = outgoingTransfers.get(packet.getRequestId());
		
		if(handle == null) {
			return;
		}
		
		outgoingTransfers.remove(handle.getRequestId());

		(new Thread() {
			public void run() {
				callbacks.onFileTransferCompleted(handle);
			}
		}).start();
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
	
	private void sendPacket(Client destination, Packet packet, Priority priority) {
		networkInterface.sendReliableTo(destination, packet, priority);
	}
	
	private void sendPacketFromTask(Client destination, Packet packet, Priority priority) {
		// This is called from a transfer task thread so push a new event on the event queue so the backend thread processes it.
		eventQueue.enqueue(new SendReliablePacketEvent(destination, packet, priority));
	}
	
	//-------------------------------------------
	// Transfer task class.
	//-------------------------------------------
	class TransferTask extends Thread {
		private final FileTransferHandle handle;
		private volatile boolean taskCancelled;
		private volatile boolean taskFailed;
		private volatile String failMessage;
		
		TransferTask(FileTransferHandle handle) {
			this.handle = handle;
			this.taskCancelled = false;
			this.taskFailed = false;
			this.failMessage = null;
		}
		
		void cancel() {
			taskCancelled = true;
			try {
				join();
			} catch (InterruptedException e) { }
		}
		
		void fail(String failMessage) {
			this.failMessage = failMessage;
			this.taskFailed = true;
			
			try {
				join();
			} catch(InterruptedException e) { }
		}
		
		@Override
		public void run() {
			setName(String.format("TransferTask-%d", handle.getRequestId()));
			
			long offset = 0;
			byte[] buffer = new byte[UnicastInterface.RECV_BUFFER_SIZE - 1024];
			int transferId = handle.getTransferId();
			Client destination = handle.getReceiver();
			int bytesRead = 0;
			
			while(!taskCancelled && !taskFailed) {
				try {
					bytesRead = handle.getInputStream().read(buffer);
				} catch (IOException e) {
					taskFailed = true;
					failMessage = String.format("Could not read from input file -> %s", e.getMessage());
					break;
				}
				
				if(bytesRead == -1) {
					// End of stream, task completed.
					break;
				}
				
				FTDataPacket dataPacket = new FTDataPacket(transferId, offset, buffer, bytesRead);
				offset += bytesRead;
				
				sendPacketFromTask(destination, dataPacket, Priority.Low);
			}
			
			// Close file handles.
			handle.close();
			
			if(taskFailed) {
				eventQueue.enqueue(new FTTaskFailedEvent(handle, failMessage));
			} else if(taskCancelled) {
				eventQueue.enqueue(new FTTaskCancelledEvent(handle));
			} else {
				eventQueue.enqueue(new FTTaskCompletedEvent(handle));
			}
		}
		
	}
	
	//-------------------------------------------
	// Receive task class.
	//-------------------------------------------
	class ReceiveTask extends Thread {
		private final SynchronizedQueue<DataFragment> dataQueue;
		private final FileTransferHandle handle;
		private final RandomAccessFile outputStream;
		private volatile boolean taskCancelled;
		private volatile boolean taskFailed;
		private volatile String failMessage;
		private long bytesReceived;
		private float lastProgressUpdate;
		
		public ReceiveTask(FileTransferHandle handle) {
			this.dataQueue = new SynchronizedQueue<DataFragment>();
			this.handle = handle;
			this.outputStream = handle.getOutputStream();
			this.taskCancelled = false;
			this.taskFailed = false;
			this.failMessage = null;
			this.bytesReceived = 0;
			this.lastProgressUpdate = 0.0f;
		}
		
		void cancel() {
			taskCancelled = true;
			try {
				join();
			} catch (InterruptedException e) { }
		}
		
		void fail(String failMessage) {
			this.failMessage = failMessage;
			this.taskFailed = true;
			
			try {
				join();
			} catch(InterruptedException e) { }
		}
		
		@Override
		public void run() {
			setName(String.format("ReceiveTask-%d", handle.getTransferId()));			
			byte[] buffer = new byte[65536];
			
			while(!taskCancelled && !taskFailed) {
				Queue<DataFragment> queue = dataQueue.swapBuffers();
				long currentOffset = 0;
				long expectedOffset = 0;
				int bufferSpaceUsed = 0;
				
				while(true) {
					DataFragment fragment = queue.poll();
					
					if(fragment == null) {
						break;
					}
					
					long offset = fragment.getOffset();
					byte[] data = fragment.getData();
					
					if(offset != expectedOffset) {
						// We're skipping one or more data fragments from writing in a sequential order. Flush any existing data to the file
						// and begin constructing a new buffer.
						if(bufferSpaceUsed != 0) {
							if(!writeToFile(currentOffset, buffer, bufferSpaceUsed)) {
								taskFailed = true;
								break;			
							}
						}
						
						currentOffset = offset;
						expectedOffset = offset + data.length;
						bufferSpaceUsed = 0;
						System.arraycopy(data, 0, buffer, bufferSpaceUsed, data.length);
						bufferSpaceUsed = data.length;
					} else if(bufferSpaceUsed + data.length > buffer.length) {
						// We received a sequential data fragment, but the data cannot fit inside the buffer. Flush any existing data to the
						// file and begin constructing a new buffer.
						if(!writeToFile(currentOffset, buffer, bufferSpaceUsed)) {
							taskFailed = true;
							break;			
						}
						
						currentOffset = offset;
						expectedOffset = offset + data.length;
						bufferSpaceUsed = 0;
						System.arraycopy(data, 0, buffer, bufferSpaceUsed, data.length);
						bufferSpaceUsed = data.length;
					} else {
						// We received a sequential data fragment and we can append it to the current buffer. No need to perform and I/O operations.
						currentOffset = expectedOffset;
						expectedOffset += data.length;
						System.arraycopy(data, 0, buffer, bufferSpaceUsed, data.length);
						bufferSpaceUsed += data.length;
					}
				}
				
				// If we have buffered the final data fragments required to complete the file write flush them to the file now so this task finishes.
				if(handle.getMissingBytes() == bufferSpaceUsed) {
					if(!writeToFile(currentOffset, buffer, bufferSpaceUsed)) {
						taskFailed = true;
						break;
					}
				}
				
				if(handle.hasCompleted()) {
					break;
				}
				
				try {
					Thread.sleep(10);
				} catch (InterruptedException e) { } 
			}
			
			// Close file handles.
			handle.close();
			
			if(taskFailed) {
				eventQueue.enqueue(new FTTaskFailedEvent(handle, failMessage));
			} else if(taskCancelled) {
				eventQueue.enqueue(new FTTaskCancelledEvent(handle));
			} else {
				eventQueue.enqueue(new FTTaskCompletedEvent(handle));
			}
		}
		
		public void queueData(long offset, byte[] data) {
			bytesReceived += data.length;
			float progress = ((float)bytesReceived / (float)handle.getFileSize()) * 100.0f;
			
			if(progress - lastProgressUpdate >= 1.0f) {
				callbacks.onFileTransferProgress(handle, progress);
				sendProgressUpdate(progress);
				lastProgressUpdate = progress;
			}
			
			if(bytesReceived == handle.getFileSize()) {
				// All file data has been received, but not necessarily flushed to disk.
				// Send completed packet to other client to inform all data has been received.
				FTCompletedPacket packet = new FTCompletedPacket(handle.getRequestId());
				sendPacket(handle.getSender(), packet, Priority.Normal);
				
				// TODO: Might want to inform the receiver all data is received and is being flushed to disk.
			}
			
			dataQueue.enqueue(new DataFragment(offset, data));
		}
		
		private boolean writeToFile(long offset, byte[] data, int length) {
			try {
				outputStream.seek(offset);
				outputStream.write(data, 0, length);
				
				handle.receivedBytes(length);
				
				return true;
			} catch (IOException e) {
				failMessage = String.format("Could not write to output file -> %s", e.getMessage());
				return false;
			}
		}
		
		private void sendProgressUpdate(float progress) {
			FTProgressPacket packet = new FTProgressPacket(handle.getRequestId(), progress);
			sendPacketFromTask(handle.getSender(), packet, Priority.Normal);
		}
	}
	
	//-------------------------------------------
	// Data fragment class.
	//-------------------------------------------
	class DataFragment {
		private final long offset;
		private final byte[] data;
		
		public DataFragment(long offset, byte[] data) {
			this.offset = offset;
			this.data = data;
		}
		
		public long getOffset() {
			return offset;
		}
		
		public byte[] getData() {
			return data;
		}
	}
}
