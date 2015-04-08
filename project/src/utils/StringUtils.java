package utils;

import java.nio.charset.Charset;

public final class StringUtils {
	public static final Charset CHARSET = Charset.forName("UTF-8");
	
	public static byte[] getBytes(String str) {
		return str.getBytes(CHARSET);
	}
	
	public static String getString(byte[] buffer, int offset, int length) {
		return new String(buffer, offset, length, CHARSET);
	}
}
