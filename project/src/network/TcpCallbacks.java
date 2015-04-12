package network;

import protocol.Packet;

public interface TcpCallbacks {
	void onTcpPacketReceived(Packet packet);
}
