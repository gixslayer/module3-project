package tests.protocol;

import static org.junit.Assert.*;

import java.net.InetAddress;
import org.junit.Before;
import org.junit.Test;
import protocol.GroupChatPacket;
import protocol.Packet;

import client.Client;

public class GroupChatPacketTest {
	private Client client1;
	private String groupName = "Group Name";
	private String message = "Message";
	private GroupChatPacket groupChatPacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		client1 = new Client("John", InetAddress.getByName("192.168.5.1"));
		groupChatPacket = new GroupChatPacket(client1, groupName, message);
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = groupChatPacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}

	@Test
	public void testDeserializeContent() {
		serializedPacket = groupChatPacket.serialize();
		GroupChatPacket deserializedSerializedPacket = (GroupChatPacket)Packet.deserialize(client1.getAddress(), serializedPacket);
		
		assertEquals("Clients are not equal", deserializedSerializedPacket.getSender(), client1);
		assertEquals("Group Names are not equal", deserializedSerializedPacket.getGroup(), groupName);
		assertEquals("Messages are not equal", deserializedSerializedPacket.getMessage(), message);
		assertNotNull("Deserialization of the packet returns null.", deserializedSerializedPacket);
		assertArrayEquals("Deserialize does not return the packet which was serialized", deserializedSerializedPacket.getContent(), groupChatPacket.getContent());
	}
}

