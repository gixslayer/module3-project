package client;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import project.TCP;
import subscription.Subscribable;
import subscription.SubscriptionCollection;
import utils.DateUtils;

public final class ClientCache implements Subscribable<CacheCallbacks>{
	public static final long TIMEOUT_DURATION = 10000; // Clients time out after not being seen for this many milliseconds.
	public static final long RECONNECT_DURATION = 2500; // A client from the same IP/name cannot be indirectly added after it manually disconnects before this many milliseconds expire.
	
	private final Object syncRoot;
	private final Client localClient;
	private final List<Client> cache;
	private final Map<Client, Long> recentlyDisconnected;
	private final SubscriptionCollection<CacheCallbacks> callbacks;
	
	public ClientCache(Client localClient) {
		this.syncRoot = new Object();
		this.localClient = localClient;
		this.cache = new ArrayList<Client>();
		this.recentlyDisconnected = new HashMap<Client, Long>();
		this.callbacks = new SubscriptionCollection<CacheCallbacks>();
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
		
		// Process callbacks outside critical section to avoid holding the lock longer than needed.
		if(clientConnected) {
			for(CacheCallbacks subscriber : callbacks) {
				subscriber.onClientConnected(client);
			}
		}
	}
	
	public void updateIndirect(Client source, Client client) {
		if(client.equals(localClient)) {
			return;
		}
		
		boolean clientConnected = false;
		
		synchronized(syncRoot) {
			if(!cache.contains(client)) {
				// If a client disconnects through a disconnect packet this client might receive the
				// disconnect packet and remove it from the cache before other clients do (due to latency/packetloss etc).
				// If that client would then send its announce packet we would indirectly add the disconnected client again.
				// This effect can bounce around in the network causing a lot of connected/disconnected spam and possibly
				// other issues. This is a bit of a hack, but should work fine.
				if(!hasRecentlyDisconnected(client)) {
					client.setIndirect(source);
					cache.add(client);
					clientConnected = true;
				}
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
		
		// Process callbacks outside critical section to avoid holding the lock longer than needed.
		if(clientConnected) {
			for(CacheCallbacks subscriber : callbacks) {
				subscriber.onClientConnected(client);
			}
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
			for(CacheCallbacks subscriber : callbacks) {
				subscriber.onClientTimedOut(client);
			}
		}
		
		for(Client client : lostRouteClients) {
			for(CacheCallbacks subscriber : callbacks) {
				subscriber.onClientLostRoute(client);
			}
		}
	}
	
	public void clientDisconnected(Client client) {
		// TODO: Is this a proper place to call this?
		TCP.closeConnection(client.getAddress());
		
		List<Client> lostRouteClients;
		
		synchronized(syncRoot) {
			if(!cache.contains(client)) {
				return;
			}
			
			recentlyDisconnected.put(client, DateUtils.getEpochTime());
			lostRouteClients = removeClient(client);
		}
		
		// Process callbacks outside critical section to avoid holding the lock longer than needed.
		for(CacheCallbacks subscriber : callbacks) {
			subscriber.onClientDisconnected(client);
		}
		
		for(Client c : lostRouteClients) {
			for(CacheCallbacks subscriber : callbacks) {
				subscriber.onClientLostRoute(c);
			}
		}
	}
	
	public void updateRecentlyDisconnected() {
		synchronized(syncRoot) {
			Iterator<Map.Entry<Client, Long> > it = recentlyDisconnected.entrySet().iterator();
			long now = DateUtils.getEpochTime();
			
			while(it.hasNext()) {
				Map.Entry<Client, Long> entry = it.next();
				
				if(now - entry.getValue() >= RECONNECT_DURATION) {
					it.remove();
				}
			}
		}
	}
	
	public Client getLocalClient() {
		return localClient;
	}
	
	public Client getClientFromName(String name) {
		// TODO: This should be synchronized on syncRoot to avoid concurrent modification exceptions, but ideally
		// this method shouldn't even exists and clients are identified by the combination of their name and
		// address instead of just their name.
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
	
	private boolean hasRecentlyDisconnected(Client client) {
		// Note: Must always be called from within a synchronized(syncRoot) block.
		return recentlyDisconnected.containsKey(client);
	}
	
	private List<Client> removeClient(Client client) {
		// Note: Must always be called from within a synchronized(syncRoot) block.
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

	//-------------------------------------------
	// Subscribable<CacheCallbacks>.
	//-------------------------------------------
	@Override
	public void subscribe(CacheCallbacks subscription) {
		callbacks.subscribe(subscription);
		
	}

	@Override
	public void unsubscribe(CacheCallbacks subscription) {
		callbacks.unsubscribe(subscription);
		
	}
}
