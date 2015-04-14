package tests.protocol;

import static org.junit.Assert.*;

import java.net.InetAddress;

import org.junit.Before;
import org.junit.Test;

import client.Client;

import protocol.FTRequestPacket;
import protocol.Packet;

public class FTRequestPacketTest {
	private int requestID = 1;
	private String fileName = "file.txt";
	private long fileSize = 50;
	private Client client;
	private FTRequestPacket ftRequestPacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		client = new Client("John", InetAddress.getByName("192.168.5.1"));
		ftRequestPacket = new FTRequestPacket(requestID, fileName, fileSize, client);
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = ftRequestPacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}

	@Test
	public void testDeserializeContent() {
		serializedPacket = ftRequestPacket.serialize();
		FTRequestPacket deserializedSerializedPacket = (FTRequestPacket)Packet.deserialize(null, serializedPacket);
		
		assertEquals("Request IDs are not equal", deserializedSerializedPacket.getRequestId(), requestID);
		assertEquals("File names are not equal", deserializedSerializedPacket.getFileName(), fileName);
		assertEquals("File sizes are not equal", deserializedSerializedPacket.getFileSize(), fileSize);
		assertEquals("Clients are not equal", deserializedSerializedPacket.getSender(), client);
		assertNotNull("Deserialization of the packet returns null.", deserializedSerializedPacket);
		assertArrayEquals("Deserialize does not return the packet which was serialized", deserializedSerializedPacket.getContent(), ftRequestPacket.getContent());
	}
}

