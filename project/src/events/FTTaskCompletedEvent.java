package events;

import filetransfer.FileTransferHandle;

public final class FTTaskCompletedEvent extends Event {
	private final FileTransferHandle handle;
	
	public FTTaskCompletedEvent(FileTransferHandle handle) {
		super(Event.TYPE_FTTASK_COMPLETED);
		
		this.handle = handle;
	}
	
	public FileTransferHandle getHandle() {
		return handle;
	}
}
