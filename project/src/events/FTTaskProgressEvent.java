package events;

import filetransfer.FileTransferHandle;

public final class FTTaskProgressEvent extends Event {
	private final FileTransferHandle handle;
	private final float progress;
	
	public FTTaskProgressEvent(FileTransferHandle handle, float progress) {
		super(Event.TYPE_FTTASK_PROGRESS);
		
		this.handle = handle;
		this.progress = progress;
	}
	
	public FileTransferHandle getHandle() {
		return handle;
	}
	
	public float getProgress() {
		return progress;
	}
}
