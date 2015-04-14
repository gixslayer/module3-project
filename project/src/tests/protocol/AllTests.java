package tests.protocol;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AnnouncePacketTest.class, ChatPacketTest.class, DisconnectPacketTest.class, EmptyPacketTest.class, 
				FTCancelPacketTest.class, FTDataPacketTest.class, FTFailedPacketTest.class, FTReplyPacketTest.class,
				FTRequestPacketTest.class, GroupChatPacketTest.class, PacketHeaderTest.class, PokePacketTest.class,
				PrivateChatPacketTest.class })
public class AllTests {

}
