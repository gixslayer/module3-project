package tests.protocol;

import static org.junit.Assert.*;

import org.junit.Before;
import org.junit.Test;

import protocol.FTReplyPacket;
import protocol.Packet;

public class FTReplyPacketTest {
	private int transferID = 1;
	private int requestID = 2;
	private boolean response = true;
	private FTReplyPacket ftReplyPacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		ftReplyPacket = new FTReplyPacket(requestID, transferID, response);
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = ftReplyPacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}

	@Test
	public void testDeserializeContent() {
		serializedPacket = ftReplyPacket.serialize();
		FTReplyPacket deserializedSerializedPacket = (FTReplyPacket)Packet.deserialize(null, serializedPacket);
		
		assertEquals("Transfer IDs are not equal", deserializedSerializedPacket.getTransferId(), transferID);
		assertEquals("Request IDs are not equal", deserializedSerializedPacket.getRequestId(), requestID);
		assertEquals("Response booleans are not equal", deserializedSerializedPacket.getResponse(), response);
		assertNotNull("Deserialization of the packet returns null.", deserializedSerializedPacket);
		assertArrayEquals("Deserialize does not return the packet which was serialized", deserializedSerializedPacket.getContent(), ftReplyPacket.getContent());
	}
}

