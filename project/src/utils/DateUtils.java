package utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

/**
 * Utility class to handle time.
 * @author ciske
 *
 */
public final class DateUtils {
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}
	
	/**
	 * Formats an epoch timestamp to a string.
	 * @param epoch The timestamp
	 * @param format The format passed to the SimpleDataFormat class
	 * @return The formatted date string.
	 */
	public static String epochToDateString(long epoch, String format) {
		Date date = new Date(epoch);
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		
		return formatter.format(date);
	}
	
	/**
	 * Returns the number of milliseconds since January 1, 1970, 00:00:00 UTC.
	 * @return the number of milliseconds since January 1, 1970, 00:00:00 UTC.
	 */
	public static long getEpochTime() {
		return (new Date()).getTime();
	}
}
