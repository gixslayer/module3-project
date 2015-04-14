package tests.utils;

import static org.junit.Assert.*;

import org.junit.Test;

import utils.ByteUtils;

public class ByteUtilsTests {

	@Test
	public void getIntBytesTest() {
		int beginValue = 1065823;
		byte[] expectedBytes = new byte[]{95,67,16,0};
		byte[] actualBytes = new byte[4];
		ByteUtils.getIntBytes(beginValue, actualBytes, 0);
		assertArrayEquals(expectedBytes, actualBytes);
	}
	
	@Test
	public void getIntBytesTestWithOffset() {
		int beginValue = 79876534;
		byte[] expectedBytes = new byte[]{0,0,0,0,0,(byte)182,(byte)209,(byte)194,4,0};
		byte[] actualBytes = new byte[10];
		ByteUtils.getIntBytes(beginValue, actualBytes, 5);
		assertArrayEquals(expectedBytes, actualBytes);
	}
	
	@Test
	public void getLongBytesTest() {
		long beginValue = 913526394569l;
		byte[] expectedBytes = new byte[]{-55,-114,106,-78,-44,0,0,0};
		byte[] actualBytes = new byte[8];
		ByteUtils.getLongBytes(beginValue, actualBytes, 0);
		assertArrayEquals(expectedBytes, actualBytes);
	}
	
	@Test
	public void getLongBytesTestWithOffset() {
		long beginValue = 81952639144569l;
		byte[] expectedBytes = new byte[]{0,0,0,121,-46,-15,21,-119,74,0,0,0};
		byte[] actualBytes = new byte[12];
		ByteUtils.getLongBytes(beginValue, actualBytes, 3);
		assertArrayEquals(expectedBytes, actualBytes);
	}
	
	@Test
	public void getIntFromByteArrayTest() {
		int beginValue = 1065823;
		byte[] actual = new byte[4];
		ByteUtils.getIntBytes(beginValue, actual, 0);
		int actualValue = ByteUtils.getIntFromBytes(actual, 0);
		assertEquals(beginValue, actualValue);
	}
	
	@Test
	public void getIntFromByteArrayTestWithOffset() {
		int beginValue = 981632548;
		byte[] actual = new byte[8];
		ByteUtils.getIntBytes(beginValue, actual, 3);
		int actualValue = ByteUtils.getIntFromBytes(actual, 3);
		assertEquals(beginValue, actualValue);
	}
	
	@Test
	public void getLongFromByteArrayTest() {
		long beginValue = 91278465983427867l;
		byte[] actual = new byte[8];
		ByteUtils.getLongBytes(beginValue, actual, 0);
		long actualValue = ByteUtils.getLongFromBytes(actual, 0);
		assertEquals(beginValue, actualValue);
	}
	
	@Test
	public void getLongFromByteArrayTestWithOffset() {
		long beginValue = 893629275869327423l;
		byte[] actual = new byte[14];
		ByteUtils.getLongBytes(beginValue, actual, 5);
		long actualValue = ByteUtils.getLongFromBytes(actual, 5);
		assertEquals(beginValue, actualValue);
	}
}
