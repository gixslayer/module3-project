package tests.protocol;

import static org.junit.Assert.*;

import java.net.InetAddress;
import org.junit.Before;
import org.junit.Test;
import protocol.PokePacket;
import protocol.Packet;

import client.Client;

public class PokePacketTest {
	private Client client1;
	private PokePacket pokePacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		client1 = new Client("John", InetAddress.getByName("192.168.5.1"));
		pokePacket = new PokePacket(client1);
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = pokePacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}

	@Test
	public void testDeserializeContent() {
		serializedPacket = pokePacket.serialize();
		PokePacket deserializedSerializedPacket = (PokePacket)Packet.deserialize(client1.getAddress(), serializedPacket);
		
		assertEquals("Clients are not equal", deserializedSerializedPacket.getClient(), client1);
		assertNotNull("Deserialization of the packet returns null.", deserializedSerializedPacket);
		assertArrayEquals("Deserialize does not return the packet which was serialized", deserializedSerializedPacket.getContent(), pokePacket.getContent());
	}
}

