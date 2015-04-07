package network;

import protocol.AnnouncePacket;
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
			// Construct the announcement packet.
			AnnouncePacket packet = new AnnouncePacket();
			
			packet.setSourceClient(localClient);
			
			for(Client client : clientCache.getClients()) {
				packet.getKnownClients().add(client);
			}
			
			// Broadcast the announcement to all clients within range.
			mci.send(packet);
			
			// Sleep the thread for the interval duration before exiting 
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
