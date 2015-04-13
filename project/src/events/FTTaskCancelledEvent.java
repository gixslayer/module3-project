package events;

import filetransfer.FileTransferHandle;

public final class FTTaskCancelledEvent extends Event {
	private final FileTransferHandle handle;
	
	public FTTaskCancelledEvent(FileTransferHandle handle) {
		super(Event.TYPE_FTTASK_CANCELLED);
		
		this.handle = handle;
	}
	
	public FileTransferHandle getHandle() {
		return handle;
	}
}
