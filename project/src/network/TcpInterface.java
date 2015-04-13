package network;

import java.net.InetAddress;

import project.TCP;
import protocol.Packet;

public class TcpInterface {
	private final UnicastInterface unicastInterface;
	private final TcpCallbacks callbacks;
	
	public TcpInterface(UnicastInterface unicastInterface, TcpCallbacks callbacks) {
		this.unicastInterface = unicastInterface;
		this.callbacks = callbacks;
	}
	
	public void process() {
		TCP.processTime();
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
