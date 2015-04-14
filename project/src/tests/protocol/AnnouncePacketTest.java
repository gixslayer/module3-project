package tests.protocol;

import static org.junit.Assert.*;
import java.net.InetAddress;
import org.junit.Before;
import org.junit.Test;
import protocol.AnnouncePacket;
import protocol.Packet;
import client.Client;

public class AnnouncePacketTest {
	private Client client1;
	private Client client2;
	private Client client3;
	private AnnouncePacket announcePacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		client1 = new Client("John", InetAddress.getByName("192.168.5.1"));
		client2 = new Client("Pete", InetAddress.getByName("192.168.5.2"));
		client3 = new Client("Rachel", InetAddress.getByName("192.168.5.3"));
		Client[] clientList = {client2, client3};
		
		announcePacket = new AnnouncePacket(client1, clientList);
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = announcePacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}

	@Test
	public void testDeserializeContent() {
		serializedPacket = announcePacket.serialize();
		AnnouncePacket deserializedSerializedPacket = (AnnouncePacket)Packet.deserialize(client1.getAddress(), serializedPacket);
		
		assertNotNull("Deserialization of the packet returns null.", deserializedSerializedPacket);
		assertArrayEquals("Deserialize does not return the packet which was serialized", deserializedSerializedPacket.getContent(), announcePacket.getContent());
	}

}
