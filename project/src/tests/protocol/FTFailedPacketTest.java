package tests.protocol;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import protocol.FTFailedPacket;
import protocol.Packet;

public class FTFailedPacketTest {
	private int transferID = 1;
	private int requestID = 2;
	private boolean receiverFailed = true;
	private FTFailedPacket ftFailedPacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		ftFailedPacket = new FTFailedPacket(transferID, requestID, receiverFailed);
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = ftFailedPacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}

	@Test
	public void testDeserializeContent() {
		serializedPacket = ftFailedPacket.serialize();
		FTFailedPacket deserializedSerializedPacket = (FTFailedPacket)Packet.deserialize(null, serializedPacket);
		
		assertEquals("Transfer IDs are not equal", deserializedSerializedPacket.getTransferId(), transferID);
		assertEquals("Request IDs are not equal", deserializedSerializedPacket.getRequestId(), requestID);
		assertEquals("Receiver Failed booleans are not equal", deserializedSerializedPacket.hasReceiverFailed(), receiverFailed);
		assertNotNull("Deserialization of the packet returns null.", deserializedSerializedPacket);
		assertArrayEquals("Deserialize does not return the packet which was serialized", deserializedSerializedPacket.getContent(), ftFailedPacket.getContent());
	}
}

