package client;

import java.util.ArrayList;
import java.util.List;

import utils.DateUtils;

public final class ClientCache {
	public static final long TIMEOUT_DURATION = 10000; // Clients time out after not being seen for this many miliseconds.
	public static final long LAST_SEEN_DISCONNECTED = -1;
	
	private final Object syncRoot;
	private final Client localClient;
	private final List<Client> cache;
	private final CacheCallbacks callbacks;
	
	public ClientCache(Client localClient, CacheCallbacks callbacks) {
		this.syncRoot = new Object();
		this.localClient = localClient;
		this.cache = new ArrayList<Client>();
		this.callbacks = callbacks;
	}
	
	public void updateDirect(Client client) {
		boolean clientConnected = false;
		
		synchronized(syncRoot) {
			if(!cache.contains(client)) {
				client.setDirect();
				cache.add(client);
				clientConnected = true;
			} else {
				Client cachedClient = cache.get(cache.indexOf(client));
				
				if(client.getLastSeen() > cachedClient.getLastSeen()) {
					cachedClient.setLastSeen(client.getLastSeen());
				}
				if(cachedClient.isIndirect()) {
					cachedClient.setDirect();
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
			if(!cache.contains(client)) {
				// TODO: Check if this client was recently disconnected due to a disconnect packet.
				client.setIndirect(source);
				cache.add(client);
				clientConnected = true;
			} else {
				Client cachedClient = cache.get(cache.indexOf(client));
			
				if(cachedClient.isIndirect()) {
					if(client.getLastSeen() > cachedClient.getLastSeen()) {
						cachedClient.setRoute(source);
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
		List<Client> timedOutClients = new ArrayList<Client>();
		List<Client> lostRouteClients = new ArrayList<Client>();
		
		synchronized(syncRoot) {
			long now = DateUtils.getEpochTime();
			
			for(Client client : cache) {
				if(now - client.getLastSeen() >= TIMEOUT_DURATION) {
					timedOutClients.add(client);
				}
			}
		
			for(Client client : timedOutClients) {
				timedOutClients.add(client);
				lostRouteClients.addAll(removeClient(client));
			}
		}
		
		// Process callbacks outside critical section to avoid holding the lock longer than needed.
		for(Client client : timedOutClients) {
			callbacks.onClientTimedOut(client);
		}
		
		for(Client client : lostRouteClients) {
			callbacks.onClientLostRoute(client);
		}
	}
	
	public void clientDisconnected(Client client) {
		List<Client> lostRouteClients;
		
		synchronized(syncRoot) {
			if(!cache.contains(client)) {
				return;
			}
			
			lostRouteClients = removeClient(client);
		}
		
		// Process callbacks outside critical section to avoid holding the lock longer than needed.
		callbacks.onClientDisconnected(client);
		
		for(Client c : lostRouteClients) {
			callbacks.onClientLostRoute(c);
		}
	}
	
	private List<Client> removeClient(Client client) {
		List<Client> lostRouteClients = new ArrayList<Client>();
		
		cache.remove(client);
		
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
	
	public Client getLocalClient() {
		return localClient;
	}
	
	public Client getClientFromName(String name) {
		for(Client c : cache) {
			if(c.getName().equals(name)) return c;
		}
		return null;
	}
	
	public Client[] getClients() {
		// TODO: Is this problematic with multi-threading, return a deep copy perhaps?
		Client[] buffer;
		int offset = 0;
		
		synchronized(syncRoot) {
			buffer = new Client[cache.size()];
		
			for(Client client : cache) {
				buffer[offset++] = client;
			}
		}
		
		return buffer;
	}
}
