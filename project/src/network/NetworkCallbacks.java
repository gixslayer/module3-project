package network;

import protocol.Packet;

public interface NetworkCallbacks {
	void onPacketReceived(Packet packet);
}
