
package network;
import protocol.AnnouncePacket;
import utils.DateUtils;
import client.Client;
import client.ClientCache;

public final class AnnounceThread extends Thread {
	private final MulticastInterface multicastInterface;
	private final Client localClient;
	private final ClientCache clientCache;
	private final int interval;
	private volatile boolean keepRunning;
	
	public AnnounceThread(MulticastInterface multicastInterface, ClientCache clientCache, int interval) {
		this.multicastInterface = multicastInterface;
		this.localClient = clientCache.getLocalClient();
		this.clientCache = clientCache;
		this.interval = interval;
	}
	
	@Override
	public void run() {
		keepRunning = true;
		setName("Announce");
		
		while(keepRunning) {
			// Update the last seen time-stamp of the local client to the current epoch time.
			localClient.setLastSeen(DateUtils.getEpochTime());

			// Construct the announcement packet.
			AnnouncePacket packet = new AnnouncePacket(localClient, clientCache.getClients());

			// Broadcast the announcement to all clients within range.
			multicastInterface.send(packet);
			
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
		try {
			// Block until the thread actually closes. Note that this could take up to 'interval' amount of milliseconds.
			join();
		} catch (InterruptedException e) { }
	}
	
	private void sleep(int millis) {
		try {
			Thread.sleep(millis);
		} catch (InterruptedException e) { }
	}
}
