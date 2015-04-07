package client;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public final class ClientCache {
	private final Client localClient;
	private final Map<String, Client> cache;
	
	public ClientCache(Client localClient) {
		this.localClient = localClient;
		this.cache = new HashMap<String, Client>();
	}
	
	public boolean update(Client client) {
		if(!cache.containsKey(client.getName())) {
			cache.put(client.getName(), client);
			return true; // Updated cache.
		}
		
		return false; // Did not update cache.
	}
	
	public Client getLocalClient() {
		return localClient;
	}
	
	public Collection<Client> getClients() {
		return cache.values();
	}
}
