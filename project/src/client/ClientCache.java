package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import application.DateUtils;

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
	
	public void updateDirect(Client client) {
		boolean clientConnected = false;
		
		synchronized(syncRoot) {
			if(!cache.containsKey(client.getName())) {
				client.setDirect();
				cache.put(client.getName(), client);
				clientConnected = true;
			} else {
				Client cachedClient = cache.get(client.getName());
			
				if(client.getLastSeen() > cachedClient.getLastSeen()) {
					cachedClient.setLastSeen(client.getLastSeen());
				}
				if(client.isIndirect()) {
					client.setDirect();
				}
			}
		}
		
		// Process callback outside critical section to avoid holding the lock longer than needed.
		if(clientConnected) {
			callbacks.onClientConnected(client);
		}
	}
	
	public void updateIndirect(Client source, Client client) {
		if(client.equals(localClient)) {
			return;
		}
		
		boolean clientConnected = false;
		
		synchronized(syncRoot) {
			if(!cache.containsKey(client.getName())) {
				client.setIndirect(source.getName());
				cache.put(client.getName(), client);
				clientConnected = true;
			} else {
				Client cachedClient = cache.get(client.getName());
			
				if(cachedClient.isIndirect()) {
					if(client.getLastSeen() > cachedClient.getLastSeen()) {
						cachedClient.setRoute(source.getName());
						cachedClient.setLastSeen(client.getLastSeen());
					}
				}
			}
		}
		
		// Process callback outside critical section to avoid holding the lock longer than needed.
		if(clientConnected) {
			callbacks.onClientConnected(client);
		}
	}
	
	public void checkForTimeouts() {
		// TODO: Route lost is a recursive problem!
		List<Client> timedOutClients = new ArrayList<Client>();
		Map<Client, Client> lostRouteClients = new HashMap<Client, Client>();
		
		synchronized(syncRoot) {
			long now = DateUtils.getEpochTime();
			
			for(Client client : cache.values()) {
				if(now - client.getLastSeen() >= TIMEOUT_DURATION) {
					timedOutClients.add(client);
				}
			}
		
			for(Client client : timedOutClients) {
				cache.remove(client.getName());
				
				for(Client c : cache.values()) {
					if(c.isIndirect() && c.getRoute().equals(client.getName())) {
						lostRouteClients.put(c, client);
					}
				}
			}
			
			for(Client client : lostRouteClients.keySet()) {
				cache.remove(client.getName());
			}
		}
		
		// Process callbacks outside critical section to avoid holding the lock longer than needed.
		for(Client client : timedOutClients) {
			callbacks.onClientTimedOut(client);
		}
		
		for(Client client : lostRouteClients.keySet()) {
			callbacks.onClientLostRoute(client, lostRouteClients.get(client));
		}
	}
	
	public void clientDisconnected(String name) {
		// TODO: Route lost is a recursive problem!
		// TODO: Disconnect clients might get added as indirect through case, prevent this!
		Client disconnectedClient;
		Map<Client, Client> lostRouteClients;
		
		synchronized(syncRoot) {
			disconnectedClient = cache.get(name);
			
			if(disconnectedClient == null) {
				return;
			}
			
			cache.remove(name);
			lostRouteClients = new HashMap<Client, Client>();
			
			for(Client client : cache.values()) {
				if(client.isIndirect() && client.getRoute().equals(name)) {
					lostRouteClients.put(client, disconnectedClient);
				}
			}
			
			for(Client client : lostRouteClients.keySet()) {
				cache.remove(client.getName());
			}
		}
		
		callbacks.onClientDisconnected(disconnectedClient);
		
		for(Client client : lostRouteClients.keySet()) {
			callbacks.onClientLostRoute(client, lostRouteClients.get(name));
		}
	}
	
	public Client getLocalClient() {
		return localClient;
	}
	
	public Client[] getClients() {
		// TODO: This is problematic with multi-threading, return a deep copy?
		Client[] buffer;
		int offset = 0;
		
		synchronized(syncRoot) {
			buffer = new Client[cache.values().size()];
		
			for(Client client : cache.values()) {
				buffer[offset++] = client;
			}
		}
		
		return buffer;
	}
}
