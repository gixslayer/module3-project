package network;

import java.net.InetAddress;

import protocol.Packet;

public final class TcpInterface {
	private final UnicastInterface unicastInterface;
	private final TcpCallbacks callbacks;
	
	public TcpInterface(UnicastInterface unicastInterface, TcpCallbacks callbacks) {
		this.unicastInterface = unicastInterface;
		this.callbacks = callbacks;
	}
	
	public void process() {
		
	}
	
	public void send(InetAddress destination, Packet packet) {
		// Act as a simple pass-through to the unicast interface as we have no working TCP implementation.
		unicastInterface.send(destination, packet);
	}
	
	public void onPacketReceived(Packet packet) {
		// Act as a simple pass-through to the unicast interface as we have no working TCP implementation.
		callbacks.onTcpPacketReceived(packet);
	}
	
	public void forceClose(InetAddress remoteAddress) {
		
	}
}
