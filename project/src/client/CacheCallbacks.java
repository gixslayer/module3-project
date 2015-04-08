package client;

public interface CacheCallbacks {
	void onClientTimedOut(Client client);
	void onClientConnected(Client client);
	void onClientDisconnected(Client client);
	void onClientLostRoute(Client client);
}
