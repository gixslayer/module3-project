package tests.protocol;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import protocol.FTCancelPacket;
import protocol.Packet;

public class FTCancelPacketTest {
	private int transferID = 1;
	private int requestID = 2;
	private boolean receiverCancelled = true;
	private FTCancelPacket ftCancelPacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		ftCancelPacket = new FTCancelPacket(transferID, requestID, receiverCancelled);
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = ftCancelPacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}

	@Test
	public void testDeserializeContent() {
		serializedPacket = ftCancelPacket.serialize();
		FTCancelPacket deserializedSerializedPacket = (FTCancelPacket)Packet.deserialize(null, serializedPacket);
		
		assertEquals("Transfer IDs are not equal", deserializedSerializedPacket.getTransferId(), transferID);
		assertEquals("Request IDs are not equal", deserializedSerializedPacket.getRequestId(), requestID);
		assertEquals("Receiver Cancelled booleans are not equal", deserializedSerializedPacket.hasReceiverCancelled(), receiverCancelled);
		assertNotNull("Deserialization of the packet returns null.", deserializedSerializedPacket);
		assertArrayEquals("Deserialize does not return the packet which was serialized", deserializedSerializedPacket.getContent(), ftCancelPacket.getContent());
	}
}

