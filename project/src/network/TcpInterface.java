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
		
	}
	
	public void onPacketReceived(Packet packet) {
		
	}
	
	public void forceClose(InetAddress remoteAddress) {
		
	}
}
