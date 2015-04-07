package protocol;

public final class ByteUtils {
	public static void getIntBytes(int value, byte[] dest, int offset) {
		dest[offset++] = (byte)(value & 0xff);
		dest[offset++] = (byte)((value >> 8) & 0xff);
		dest[offset++] = (byte)((value >> 16) & 0xff);
		dest[offset++] = (byte)((value >> 24) & 0xff);
	}
	
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
	
	public static int getIntFromBytes(byte[] src, int offset) {
		return (src[offset++] & 0xff) | 
				(src[offset++] & 0xff) << 8 | 
				(src[offset++] & 0xff) << 16 | 
				(src[offset++] & 0xff) << 24;
	}
	
	public static long getLongFromBytes(byte[] src, int offset) {
		return (src[offset++] & 0xff) | 
				(src[offset++] & 0xff) << 8 | 
				(src[offset++] & 0xff) << 16 | 
				(src[offset++] & 0xff) << 24 |
				(src[offset++] & 0xff) << 32 |
				(src[offset++] & 0xff) << 40 |
				(src[offset++] & 0xff) << 48 |
				(src[offset++] & 0xff) << 56;
	}
}
