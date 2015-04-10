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
		TCP.stopConnections();
	}
	
	public void send(InetAddress dest, Packet packet) {
		TCP.sendData(ni, localAddress, dest, packet);
	}
	
	public boolean onPacketReceived(Packet packet) {
		return TCP.handlePacket(ni, localAddress, packet.getHeader());
	}
}
