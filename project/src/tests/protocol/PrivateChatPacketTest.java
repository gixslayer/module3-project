package tests.protocol;

import static org.junit.Assert.*;

import java.net.InetAddress;
import org.junit.Before;
import org.junit.Test;
import protocol.PrivateChatPacket;
import protocol.Packet;

import client.Client;

public class PrivateChatPacketTest {
	private Client client1;
	private String message = "Message";
	private PrivateChatPacket privateChatPacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		client1 = new Client("John", InetAddress.getByName("192.168.5.1"));
		privateChatPacket = new PrivateChatPacket(client1, message);
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = privateChatPacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}

	@Test
	public void testDeserializeContent() {
		serializedPacket = privateChatPacket.serialize();
		PrivateChatPacket deserializedSerializedPacket = (PrivateChatPacket)Packet.deserialize(client1.getAddress(), serializedPacket);
		
		assertEquals("Clients are not equal", deserializedSerializedPacket.getClient(), client1);
		assertEquals("Messages are not equal", deserializedSerializedPacket.getMessage(), message);
		assertNotNull("Deserialization of the packet returns null.", deserializedSerializedPacket);
		assertArrayEquals("Deserialize does not return the packet which was serialized", deserializedSerializedPacket.getContent(), privateChatPacket.getContent());
	}
}

