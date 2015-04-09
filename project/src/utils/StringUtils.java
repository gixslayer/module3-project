package utils;

import java.nio.charset.Charset;

/**
 * Utility class to handle string encoding.
 * @author ciske
 *
 */
public final class StringUtils {
	/**
	 * The Charset used for encoding. 
	 */
	public static final Charset CHARSET = Charset.forName("UTF-8");
	
	/**
	 * Encodes the string to a byte array using the Charset specified by the CHARSET constant.
	 * @param str The string to encode
	 * @return The encoded data.
	 */
	public static byte[] getBytes(String str) {
		return str.getBytes(CHARSET);
	}
	
	/**
	 * Decodes a byte array to a string using the Charset specified by the CHARSET constant.
	 * @param buffer The byte array containing the encoded data
	 * @param offset The offset within the buffer to begin reading from
	 * @param length The length of the encoded string in bytes
	 * @return The decoded string.
	 */
	public static String getString(byte[] buffer, int offset, int length) {
		return new String(buffer, offset, length, CHARSET);
	}
}
