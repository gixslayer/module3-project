package client;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class ClientCache {
	public static final long TIMEOUT_DURATION = 10000; // Clients time out after not being seen for this many miliseconds.
	public static final long LAST_SEEN_DISCONNECTED = -1;
	
	private final Object syncRoot;
	private final Client localClient;
	private final Map<String, Client> cache;
	private final CacheCallbacks callbacks;
	
	public ClientCache(Client localClient, CacheCallbacks callbacks) {
		this.syncRoot = new Object();
		this.localClient = localClient;
		this.cache = new HashMap<String, Client>();
		this.callbacks = callbacks;
	}
	
	public void update(Client client) {
		if(client == localClient) {
			return;
		}
		
		boolean clientDisconnected = false;
		boolean clientConnected = false;
		
		synchronized(syncRoot) {
			if(client.getLastSeen() == LAST_SEEN_DISCONNECTED) {
				if(cache.containsKey(client.getName())) {
					cache.remove(client.getName());
					clientDisconnected = true;
				}
			} else if(!cache.containsKey(client.getName())) {
				cache.put(client.getName(), client);
				clientConnected = true;
			} else {
				Client cachedClient = cache.get(client.getName());
			
				if(client.getLastSeen() > cachedClient.getLastSeen()) {
					cachedClient.setLastSeen(client.getLastSeen());
				}
			}
		}
		
		// Process callbacks outside critical section to avoid holding the lock longer than needed.
		if(clientConnected) {
			callbacks.onClientConnected(client);
		} else if(clientDisconnected) {
			callbacks.onClientDisconnected(client);
		}
	}
	
	public void checkForTimeouts() {
		List<Client> timedOutClients = new ArrayList<Client>();
		long now = System.currentTimeMillis();
		
		synchronized(syncRoot) {
			for(Client client : cache.values()) {
				if(now - client.getLastSeen() >= TIMEOUT_DURATION) {
					timedOutClients.add(client);
				}
			}
		
			for(Client client : timedOutClients) {
				cache.remove(client.getName());
			}
		}
		
		// Process callbacks outside critical section to avoid holding the lock longer than needed.
		for(Client client : timedOutClients) {
			callbacks.onClientTimedOut(client);
		}
	}
	
	public Client getLocalClient() {
		return localClient;
	}
	
	public Collection<Client> getClients() {
		return cache.values();
	}
}
