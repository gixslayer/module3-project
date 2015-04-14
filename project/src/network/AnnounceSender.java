
package network;
import protocol.AnnouncePacket;
import utils.DateUtils;
import client.Client;
import client.ClientCache;

public final class AnnounceSender {
	private final MulticastInterface multicastInterface;
	private final Client localClient;
	private final ClientCache clientCache;
	private final int interval;
	private long lastSent;
	
	public AnnounceSender(MulticastInterface multicastInterface, ClientCache clientCache, int interval) {
		this.multicastInterface = multicastInterface;
		this.localClient = clientCache.getLocalClient();
		this.clientCache = clientCache;
		this.interval = interval;
		this.lastSent = 0;
	}
	
	public void process() {
		if(System.currentTimeMillis() - lastSent >= interval) {
			System.out.println("ANN " + (System.currentTimeMillis()-lastSent));
			// Update the last seen time-stamp of the local client to the current epoch time.
			localClient.setLastSeen(DateUtils.getEpochTime());

			// Construct the announcement packet.
			AnnouncePacket packet = new AnnouncePacket(localClient, clientCache.getClients());

			// Broadcast the announcement to all clients within range.
			multicastInterface.send(packet);
			
			lastSent = System.currentTimeMillis();
		}
	}
}
