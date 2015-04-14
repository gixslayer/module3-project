package network;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import protocol.Packet;

public class TcpInterface {
	private static final boolean USE_TCP= true;
	
	private final UnicastInterface unicastInterface;
	private final TcpCallbacks callbacks;
	private final Map<InetAddress, TcpConnection> connections;
	
	public TcpInterface(UnicastInterface unicastInterface, TcpCallbacks callbacks) {
		this.unicastInterface = unicastInterface;
		this.callbacks = callbacks;
		this.connections = new HashMap<InetAddress, TcpConnection>();
	}
	
	public void process() {
		if(USE_TCP) {
			for(TcpConnection connection : connections.values()) {
				connection.process();
			}
		}
	}
	
	public void send(InetAddress destination, Packet packet, Priority priority) {
		if(USE_TCP) {
			getConnection(destination).queuePacket(packet, priority);
		} else {
			unicastInterface.send(destination, packet);
		}
	}
	
	public void onPacketReceived(Packet packet) {
		if(USE_TCP) {
			getConnection(packet.getSourceAddress()).onPacketReceived(packet);
		} else {
			callbacks.onTcpPacketReceived(packet);
		}
	}
	
	public void forceClose(InetAddress remoteAddress) {
		if(USE_TCP) {
			connections.remove(remoteAddress);
		}
	}

	private TcpConnection getConnection(InetAddress remoteAddress) {
		TcpConnection connection = connections.get(remoteAddress);
		
		if(connection == null) {
			connection = new TcpConnection(unicastInterface, remoteAddress, callbacks);
			connections.put(remoteAddress, connection);
		}
		
		return connection;
	}
}
