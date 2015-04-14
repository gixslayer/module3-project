package tests.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import utils.DateUtils;

public class DateUtilsTest {
	@Test
	public void epochToDateStringTest() {
		long epochTime = 1429011105585l;
		String expected = "11:31:45";
		assertEquals(expected, DateUtils.epochToDateString(epochTime, "HH:mm:ss"));
	}
}
