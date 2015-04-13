package test;

import java.net.SocketException;

public class Test {
	
	public static void main(String[] args) throws SocketException, InterruptedException {
		NetworkInterface networkA = new NetworkInterface(6969, 6970);
		NetworkInterface networkB = new NetworkInterface(6970, 6969);
		TcpConnection tcpA = new TcpConnection("A", networkA);
		TcpConnection tcpB = new TcpConnection("B", networkB);
		
		tcpB.listen();
		Thread.sleep(100);
		tcpA.listen();
		Thread.sleep(100);
		
		for(int i = 0; i < 1; i++) {
			byte[] data = new byte[4];
			data[0] = (byte)i;
			
			tcpA.send(new Packet(data));
		}
		
		tcpA.hashCode();
	}
}
