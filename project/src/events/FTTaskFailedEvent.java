package events;

import filetransfer.FileTransferHandle;

public final class FTTaskFailedEvent extends Event {
	private final FileTransferHandle handle;
	private final String reason;
	
	public FTTaskFailedEvent(FileTransferHandle handle, String reason) {
		super(Event.TYPE_FTTASK_FAILED);
		
		this.handle = handle;
		this.reason = reason;
	}
	
	public FileTransferHandle getHandle() {
		return handle;
	}
	
	public String getReason() {
		return reason;
	}
}
