package network;

import protocol.Packet;

public interface MulticastCallbacks {
	void onMulticastPacketReceived(Packet packet);
}
