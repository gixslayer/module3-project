package network;

import protocol.Packet;

public interface MulticastCallbacks {
	void onPacketReceived(Packet packet);
}
