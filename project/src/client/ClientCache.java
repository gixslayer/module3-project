package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import utils.DateUtils;

public final class ClientCache {
	public static final long TIMEOUT_DURATION = 10000; // Clients time out after not being seen for this many milliseconds.
	public static final long RECONNECT_DURATION = 5000; // A client from the same IP/name cannot be indirectly added after it manually disconnects before this many milliseconds expire.
	
	private final Client localClient;
	private final List<Client> cache;
	private final Map<Client, Long> recentlyDisconnected;
	private final CacheCallbacks callbacks;
	
	public ClientCache(Client localClient, CacheCallbacks callbacks) {
		this.localClient = localClient;
		this.cache = new ArrayList<Client>();
		this.recentlyDisconnected = new HashMap<Client, Long>();
		this.callbacks = callbacks;
	}
	
	public void process() {
		// Check if any clients currently in cache have timed out.
		checkForTimeouts();
		
		// Update the recently disconnected map to remove outdated entries so they can be added indirectly again.
		updateRecentlyDisconnected();
	}
	
	public void updateDirect(Client client) {
		if(!cache.contains(client)) {
			if(!hasRecentlyDisconnected(client)) {
				// Client currently isn't in the cache, but we received an announcement message from him directly.
				client.setDirect();
				cache.add(client);
				callbacks.onClientConnected(client);
			}
		} else {
			// Client is already in the cache, so grab the cached entry.
			Client cachedClient = getCachedClient(client);
				
			// If the client was seen more recently than the cache currently suggests update the cache.
			// This keeps clients from timing out as long as they send an announcement every now and then.
			if(client.getLastSeen() > cachedClient.getLastSeen()) {
				cachedClient.setLastSeen(client.getLastSeen());
			}
			
			// If the cached client is marked as indirect (unreachable over multicast) mark him as direct as we
			// just received a direct message from him.
			if(cachedClient.isIndirect()) {
				cachedClient.setDirect();
			}
		}
	}
	
	public void updateIndirect(Client source, Client client) {
		if(client.equals(localClient)) {
			return;
		}
		
		if(!cache.contains(client)) {
			// If a client disconnects through a disconnect packet this client might receive the
			// disconnect packet and remove it from the cache before other clients do (due to latency/packetloss etc).
			// If that client would then send its announce packet we would indirectly add the disconnected client again.
			// This effect can bounce around in the network causing a lot of connected/disconnected spam and possibly
			// other issues. This is a bit of a hack, but should work fine.
			if(!hasRecentlyDisconnected(client)) {
				client.setIndirect(source);
				cache.add(client);
				callbacks.onClientConnected(client);
			}
		} else {
			// Client is already in the cache, so grab the cached entry.
			Client cachedClient = getCachedClient(client);
			
			// Only process if this client cannot reach us directly.
			if(cachedClient.isIndirect()) {
				
				// If the client was seen more recently than the cache currently suggests update the cache.
				// This keeps clients from timing out as long as we receive receive their announcement data indirectly
				// through other client announcements.
				if(client.getLastSeen() > cachedClient.getLastSeen()) {
					cachedClient.setLastSeen(client.getLastSeen());
					
					// TODO: Do we even need to keep track of this? The idea was to route through the client that saw the
					// client most recently, but if we can delegate the routing to the IP layer we can ignore this.
					cachedClient.setRoute(source);
				}
			}
		}
	}
	
	public void clientDisconnected(Client client) {
		// If the client isn't in the cache there is nothing to do. 
		if(!cache.contains(client)) {
			return;
		}

		// Grab a list of all clients can no longer be reached when this client is removed.
		// TODO: Do we need this? Perhaps just let the clients be disconnected through the timeout system instead?
		List<Client> lostRouteClients = removeClient(client);
		
		callbacks.onClientDisconnected(client);
		
		for(Client c : lostRouteClients) {
			callbacks.onClientLostRoute(c);
		}
	}
	
	public void routeLost(Client destination) {
		boolean wasInCache = false;
		List<Client> removedClients = null;
		
			destination = getCachedClient(destination);
			
			if(destination != null) {
				wasInCache = true;
				removedClients = removeClient(destination);
			}
		
		// Don't call the callbacks if the client was no longer in the cache.
		if(!wasInCache) {
			return;
		}

		// Process callbacks outside critical section to avoid holding the lock longer than needed.
		callbacks.onClientLostRoute(destination);
		
		for(Client client : removedClients) {
			callbacks.onClientLostRoute(client);
		}
	}
	
	public Client getLocalClient() {
		return localClient;
	}

	public Client getCachedClient(Client client) {
		for(Client cachedClient : cache) {
			if(cachedClient.equals(client)) {
				return cachedClient;
			}
		}
		
		return null;
	}
	
	public Client[] getClients() {
		Client[] buffer;
		int offset = 0;
		
			buffer = new Client[cache.size()];
		
			for(Client client : cache) {
				buffer[offset++] = client;
			}
		
		return buffer;
	}
	
	//-------------------------------------------
	// Private helper methods.
	//-------------------------------------------
	private void updateRecentlyDisconnected() {
		Iterator<Map.Entry<Client, Long> > it = recentlyDisconnected.entrySet().iterator();
		long now = DateUtils.getEpochTime();
		
		// While there is a client to iterate over.
		while(it.hasNext()) {
			// Grab the next client.
			Map.Entry<Client, Long> entry = it.next();
				
			// If the client was disconnected at least RECONNECT_DURATION milliseconds ago remove him from the map.
			if(now - entry.getValue() >= RECONNECT_DURATION) {
				it.remove();
			}
		}
	}
	
	private void checkForTimeouts() {
		List<Client> timedOutClients = new ArrayList<Client>();
		List<Client> lostRouteClients = new ArrayList<Client>();
		long now = DateUtils.getEpochTime();
			
		for(Client client : cache) {
			if(now - client.getLastSeen() >= TIMEOUT_DURATION) {
				timedOutClients.add(client);
			}
		}
		
		for(Client client : timedOutClients) {
			lostRouteClients.addAll(removeClient(client));
		}
		
		// Process callbacks outside critical section to avoid holding the lock longer than needed.
		for(Client client : timedOutClients) {
			callbacks.onClientTimedOut(client);
		}
		
		for(Client client : lostRouteClients) {
			callbacks.onClientLostRoute(client);
		}
	}
	
	private boolean hasRecentlyDisconnected(Client client) {
		return recentlyDisconnected.containsKey(client);
	}
	
	private List<Client> removeClient(Client client) {
		List<Client> lostRouteClients = new ArrayList<Client>();
		
		cache.remove(client);
		recentlyDisconnected.put(client, DateUtils.getEpochTime());
		
		for(Client c : cache) {
			if(c.isIndirect() && c.getRoute().equals(client)) {
				lostRouteClients.add(c);
			}
		}
		
		for(Client c : lostRouteClients) {
			lostRouteClients.addAll(removeClient(c));
		}
		
		return lostRouteClients;
	}
}
