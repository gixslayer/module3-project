package events;

import filetransfer.FileTransferHandle;

public final class CancelFileTransferEvent extends Event {
	private final FileTransferHandle handle;
	
	public CancelFileTransferEvent(FileTransferHandle handle) {
		super(Event.TYPE_CANCEL_FILE_TRANSFER);
		
		this.handle = handle;
	}
	
	public FileTransferHandle getHandle() {
		return handle;
	}
}
