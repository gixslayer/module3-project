package tests.protocol;

import org.junit.runner.RunWith;
import org.junit.runners.Suite;
import org.junit.runners.Suite.SuiteClasses;

@RunWith(Suite.class)
@SuiteClasses({ AnnouncePacketTest.class, ChatPacketTest.class })
public class AllTests {

}
