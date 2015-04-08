package utils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

public final class DateUtils {
	static {
		TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
	}
	
	public static String timestampToDateString(long timestamp, String format) {
		Date date = new Date(timestamp);
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		
		return formatter.format(date);
	}
	
	public static long getEpochTime() {
		return (new Date()).getTime();
	}
}
