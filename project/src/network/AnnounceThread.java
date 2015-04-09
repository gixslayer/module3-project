
package network;
import protocol.AnnouncePacket;
import utils.DateUtils;
import client.Client;
import client.ClientCache;

public final class AnnounceThread extends Thread {
	private final MulticastInterface mci;
	private final Client localClient;
	private final ClientCache clientCache;
	private final int interval;
	private volatile boolean keepRunning;
	
	public AnnounceThread(MulticastInterface mci, ClientCache clientCache, int interval) {
		this.mci = mci;
		this.localClient = clientCache.getLocalClient();
		this.clientCache = clientCache;
		this.interval = interval;
	}
	
	@Override
	public void run() {
		keepRunning = true;
		
		while(keepRunning) {
			// Update the last seen time-stamp of the local client to the current epoch time.
			localClient.setLastSeen(DateUtils.getEpochTime());

			// Construct the announcement packet.
			AnnouncePacket packet = new AnnouncePacket(localClient, clientCache.getClients());

			// Broadcast the announcement to all clients within range.
			mci.send(packet);
			
			// Check if any clients timed out.
			clientCache.checkForTimeouts();
			
			// Update the recently disconnected table.
			clientCache.updateRecentlyDisconnected();
			
			// Sleep the thread for the interval duration before exiting the loop iteration.
			sleep(interval);
		}
	}
	
	public void close() {
		keepRunning = false;
	}
	
	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) {
			// I don't expect this to ever fail, but just in case it does happen dump the exception to stderr.
			System.err.printf("AnnouceThread.sleep failed: %s%n", e.getMessage());
		}
	}
}
