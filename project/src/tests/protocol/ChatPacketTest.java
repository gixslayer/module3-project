package tests.protocol;

import static org.junit.Assert.*;

import java.net.InetAddress;
import org.junit.Before;
import org.junit.Test;
import protocol.ChatPacket;
import protocol.Packet;

import client.Client;

public class ChatPacketTest {
	private Client client1;
	private ChatPacket chatPacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		client1 = new Client("John", InetAddress.getByName("192.168.5.1"));
		chatPacket = new ChatPacket(client1, "Test Message");
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = chatPacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}

	@Test
	public void testDeserializeContent() {
		serializedPacket = chatPacket.serialize();
		ChatPacket deserializedSerializedPacket = (ChatPacket)Packet.deserialize(client1.getAddress(), serializedPacket);
		
		assertEquals("Clients are not equal", deserializedSerializedPacket.getClient(), client1);
		assertEquals("Messages are not equal", deserializedSerializedPacket.getMessage(), "Test Message");
		assertNotNull("Deserialization of the packet returns null.", deserializedSerializedPacket);
		assertArrayEquals("Deserialize does not return the packet which was serialized", deserializedSerializedPacket.getContent(), chatPacket.getContent());
	}
}

