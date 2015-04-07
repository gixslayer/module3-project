package network;

import java.net.InetAddress;

import protocol.Packet;

public interface MulticastCallbacks {
	void onMulticastPacketReceived(Packet packet, InetAddress address);
}
