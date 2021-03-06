package utils;

/**
 * Utility class to handle binary conversions of primitive types.
 * @author ciske
 *
 */
public final class ByteUtils {
	/**
	 * Converts the specified value to a little-endian binary format.
	 * @param value The value to convert
	 * @param dest The destination buffer to store the converted data
	 * @param offset The offset within the buffer to begin storing data
	 */
	public static void getIntBytes(int value, byte[] dest, int offset) {
		dest[offset++] = (byte)(value & 0xff);
		dest[offset++] = (byte)((value >> 8) & 0xff);
		dest[offset++] = (byte)((value >> 16) & 0xff);
		dest[offset++] = (byte)((value >> 24) & 0xff);
	}
	
	/**
	 * Converts the specified value to a little-endian binary format.
	 * @param value The value to convert
	 * @param dest The destination buffer to store the converted data
	 * @param offset The offset within the buffer to begin storing data
	 */
	public static void getLongBytes(long value, byte[] dest, int offset) {
		dest[offset++] = (byte)(value & 0xff);
		dest[offset++] = (byte)((value >> 8) & 0xff);
		dest[offset++] = (byte)((value >> 16) & 0xff);
		dest[offset++] = (byte)((value >> 24) & 0xff);
		dest[offset++] = (byte)((value >> 32) & 0xff);
		dest[offset++] = (byte)((value >> 40) & 0xff);
		dest[offset++] = (byte)((value >> 48) & 0xff);
		dest[offset++] = (byte)((value >> 56) & 0xff);
	}
	
	/**
	 * Converts the specified buffer to a signed integer.
	 * @param src The source buffer containing the little-endian integer bytes
	 * @param offset The offset within the buffer to begin reading data
	 * @return The converted signed integer.
	 */
	public static int getIntFromBytes(byte[] src, int offset) {
		return (src[offset++] & 0xff) | 
				(src[offset++] & 0xff) << 8 | 
				(src[offset++] & 0xff) << 16 | 
				(src[offset++] & 0xff) << 24;
	}
	
	/**
	 * Converts the specified buffer to a signed long.
	 * @param src The source buffer containing the little-endian long bytes
	 * @param offset The offset within the buffer to begin reading data
	 * @return The converted signed long.
	 */
	public static long getLongFromBytes(byte[] src, int offset) {
		long result = 0;
		
		result |= ((long)src[offset++] & 0xff) << 0;
		result |= ((long)src[offset++] & 0xff) << 8;
		result |= ((long)src[offset++] & 0xff) << 16;
		result |= ((long)src[offset++] & 0xff) << 24;
		result |= ((long)src[offset++] & 0xff) << 32;
		result |= ((long)src[offset++] & 0xff) << 40;
		result |= ((long)src[offset++] & 0xff) << 48;
		result |= ((long)src[offset++] & 0xff) << 56;
		
		return result;
	}
}
