package events;

import filetransfer.FileTransferHandle;

public final class ReplyToFileTransferEvent extends Event {
	private final FileTransferHandle handle;
	private final boolean response;
	private final String savePath;
	
	public ReplyToFileTransferEvent(FileTransferHandle handle, boolean response, String savePath) {
		super(Event.TYPE_REPLY_TO_FILE_TRANSFER);
		
		this.handle = handle;
		this.response = response;
		this.savePath = savePath;
	}
	
	public FileTransferHandle getHandle() {
		return handle;
	}
	
	public boolean getResponse() {
		return response;
	}
	
	public String getSavePath() {
		return savePath;
	}
}
