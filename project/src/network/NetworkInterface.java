package network;

import protocol.Packet;
import client.Client;

public interface NetworkInterface {
	void sendTo(Client client, Packet packet);
	void sendReliableTo(Client client, Packet packet);
	void sendToAll(Packet packet);
	void sendReliableToAll(Packet packet);
	
	Client getLocalClient();
}
