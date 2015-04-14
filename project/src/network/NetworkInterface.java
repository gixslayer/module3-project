package network;

import containers.Priority;
import protocol.Packet;
import client.Client;

public interface NetworkInterface {
	void sendTo(Client client, Packet packet);
	void sendReliableTo(Client client, Packet packet, Priority priority);
	void sendToAll(Packet packet);
	void sendReliableToAll(Packet packet, Priority priority);
	
	Client getLocalClient();
}
