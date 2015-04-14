package tests.protocol;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

import test.network.PriorityQueueTest;
import tests.utils.ByteUtilsTests;
import tests.utils.DateUtilsTest;
import tests.utils.StringUtilsTest;

@RunWith(Suite.class)
@SuiteClasses({ AnnouncePacketTest.class, ChatPacketTest.class, DisconnectPacketTest.class, EmptyPacketTest.class, 
				FTCancelPacketTest.class, FTDataPacketTest.class, FTFailedPacketTest.class, FTReplyPacketTest.class,
				FTRequestPacketTest.class, GroupChatPacketTest.class, PacketHeaderTest.class, PokePacketTest.class,
				PrivateChatPacketTest.class, ByteUtilsTests.class, DateUtilsTest.class, StringUtilsTest.class,
				PriorityQueueTest.class })
public class AllTests {

}
