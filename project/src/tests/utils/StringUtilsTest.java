package tests.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import utils.StringUtils;

public class StringUtilsTest {

	@Test
	public void getBytesTest() {
		String input = "Ik ben een string";
		byte[] expectedOutput = new byte[]{73,107,32,98,101,110,32,101,101,110,32,115,116,114,105,110,103};
		assertArrayEquals(expectedOutput, StringUtils.getBytes(input));
	}
	
	@Test
	public void getStringTest() {
		String input = "Een andere string";
		String expectedOutput = "Een andere string";
		assertEquals(expectedOutput, StringUtils.getString(StringUtils.getBytes(input),0,input.length()));
	}
}
