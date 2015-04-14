package tests.protocol;

import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import protocol.PacketHeader;

public class PacketHeaderTest {
	private int seq = 1;
	private int ack = 2;
	private int flags = 2;
	private PacketHeader packetHeader;
	private byte[] serializedPacket;
	
	@Before
	public void setUp() throws Exception {
			packetHeader = new PacketHeader(seq, ack, flags);
	}

	@Test
	public void testSerializeContent() {
		serializedPacket = packetHeader.serialize();
		
		assertNotNull("Serialization of the packet returns null.", serializedPacket);
	}
}

