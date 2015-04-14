package tests.protocol;

import static org.junit.Assert.*;
import java.net.InetAddress;
import org.junit.Before;
import org.junit.Test;
import protocol.DisconnectPacket;
import protocol.Packet;

import client.Client;

public class DisconnectPacketTest {
	private Client client1;
	private DisconnectPacket disconnectPacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		client1 = new Client("John", InetAddress.getByName("192.168.5.1"));
		disconnectPacket = new DisconnectPacket(client1);
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = disconnectPacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}

	@Test
	public void testDeserializeContent() {
		serializedPacket = disconnectPacket.serialize();
		DisconnectPacket deserializedSerializedPacket = (DisconnectPacket)Packet.deserialize(client1.getAddress(), serializedPacket);
		
		assertEquals("Clients are not equal", deserializedSerializedPacket.getClient(), client1);
		assertNotNull("Deserialization of the packet returns null.", deserializedSerializedPacket);
		assertArrayEquals("Deserialize does not return the packet which was serialized", deserializedSerializedPacket.getContent(), disconnectPacket.getContent());
	}
}

