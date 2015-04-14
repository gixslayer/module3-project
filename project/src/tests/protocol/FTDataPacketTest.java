package tests.protocol;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import protocol.FTDataPacket;
import protocol.Packet;

public class FTDataPacketTest {
	private int transferID = 1;
	private long offset = 50;
	private byte[] data = {(byte) 0xFF, (byte) 0xFF, 0x00, (byte) 0xAA, (byte) 0xFF, 0x12, 0x13};
	private FTDataPacket ftDataPacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		ftDataPacket = new FTDataPacket(transferID, offset, data, data.length);
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = ftDataPacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}

	@Test
	public void testDeserializeContent() {
		serializedPacket = ftDataPacket.serialize();
		FTDataPacket deserializedSerializedPacket = (FTDataPacket)Packet.deserialize(null, serializedPacket);
		
		assertEquals("Transfer IDs are not equal", deserializedSerializedPacket.getTransferId(), transferID);
		assertEquals("Offsets are not equal", deserializedSerializedPacket.getOffset(), offset);
		assertArrayEquals("Data Arrays are not equal", deserializedSerializedPacket.getData(), data);
		assertNotNull("Deserialization of the packet returns null.", deserializedSerializedPacket);
		assertArrayEquals("Deserialize does not return the packet which was serialized", deserializedSerializedPacket.getContent(), ftDataPacket.getContent());
	}
}

