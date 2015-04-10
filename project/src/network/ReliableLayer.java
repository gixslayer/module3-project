package network;

import java.net.InetAddress;

import project.TCP;
import protocol.Packet;

public final class ReliableLayer {
	private final InetAddress localAddress;
	private final NetworkInterface ni;
	
	public ReliableLayer(InetAddress localAddress, NetworkInterface ni) {
		this.localAddress = localAddress;
		this.ni = ni;
	}
	
	public void close() {
		// TODO: Needed processing. The network interface will be closed and is unusable as soon as this
		// method returns.
	}
	
	public void send(InetAddress dest, Packet packet) {
		TCP.sendData(ni, localAddress, dest, packet.serialize());
		ni.send(dest, packet);
	}
	
	public void onPacketReceived(Packet packet) {
		// TODO: Needed processing. Perhaps a way to signal to drop the packet, EG return a boolean?
	}
}
