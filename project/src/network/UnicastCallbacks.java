package network;

import protocol.Packet;

public interface UnicastCallbacks {
	void onUnicastPacketReceived(Packet packet);
}
