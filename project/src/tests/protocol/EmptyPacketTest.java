package tests.protocol;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import protocol.EmptyPacket;

public class EmptyPacketTest {
	private EmptyPacket emptyPacket;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
		emptyPacket = new EmptyPacket();
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = emptyPacket.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}
}

