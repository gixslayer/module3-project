package network;

import java.net.InetAddress;

import project.TCP;
import protocol.Packet;

public class TcpInterface {
	private final UnicastInterface unicastInterface;
	private final TcpCallbacks callbacks;
	private final InetAddress localAddress;
	
	public TcpInterface(UnicastInterface unicastInterface, InetAddress localAddress, TcpCallbacks callbacks) {
		this.unicastInterface = unicastInterface;
		this.localAddress = localAddress;
		this.callbacks = callbacks;
	}
	
	public void process() {
		//TCP.processTime();
	}
	
	public void send(InetAddress destination, Packet packet) {
		// Act as a simple pass-through to the unicast interface as we have no working TCP implementation.
		unicastInterface.send(destination, packet);
		//TCP.sendData(unicastInterface, localAddress, destination, packet);
	}
	
	public void onPacketReceived(Packet packet) {
		// Act as a simple pass-through to the unicast interface as we have no working TCP implementation.
		//if(TCP.handlePacket(unicastInterface, localAddress, packet.getHeader())) {
			callbacks.onTcpPacketReceived(packet);
		//}
	}
	
	public void forceClose(InetAddress remoteAddress) {
		
	}
}
