package network;

import java.net.InetAddress;
import java.util.HashMap;
import java.util.Map;

import project.TCP;
import protocol.Packet;

enum TcpMode {
	Passthrough,
	OldTCP,
	NewTCP
}

public class TcpInterface {
	private static final TcpMode MODE = TcpMode.NewTCP;
	
	private final UnicastInterface unicastInterface;
	private final TcpCallbacks callbacks;
	private final InetAddress localAddress;
	private final Map<InetAddress, TcpConnection> connections;
	
	public TcpInterface(UnicastInterface unicastInterface, InetAddress localAddress, TcpCallbacks callbacks) {
		this.unicastInterface = unicastInterface;
		this.localAddress = localAddress;
		this.callbacks = callbacks;
		this.connections = new HashMap<InetAddress, TcpConnection>();
	}
	
	public void process() {
		if(MODE == TcpMode.OldTCP) {
			TCP.processTime();
		} else if(MODE == TcpMode.NewTCP) {
			for(TcpConnection connection : connections.values()) {
				connection.process();
			}
		}
	}
	
	public void send(InetAddress destination, Packet packet) {
		if(MODE == TcpMode.Passthrough) {
			unicastInterface.send(destination, packet);
		} else if(MODE == TcpMode.OldTCP) {
			TCP.sendData(unicastInterface, localAddress, destination, packet);
		} else if(MODE == TcpMode.NewTCP) {
			getConnection(destination).queuePacket(packet);
		}
	}
	
	public void onPacketReceived(Packet packet) {
		if(MODE == TcpMode.Passthrough) {
			callbacks.onTcpPacketReceived(packet);
		} else if(MODE == TcpMode.OldTCP) {
			if(TCP.handlePacket(unicastInterface, localAddress, packet.getHeader())) {
				callbacks.onTcpPacketReceived(packet);
			}
		} else if(MODE == TcpMode.NewTCP) {
			getConnection(packet.getSourceAddress()).onPacketReceived(packet);
		}
	}
	
	public void forceClose(InetAddress remoteAddress) {
		if(MODE == TcpMode.NewTCP) {
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
