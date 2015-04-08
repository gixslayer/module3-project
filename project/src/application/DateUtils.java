package application;

import java.text.SimpleDateFormat;
import java.util.Date;

public final class DateUtils {
	public static String timestampToDateString(long timestamp, String format) {
		Date date = new Date(timestamp);
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		
		return formatter.format(date);
	}
	
	public static long getEpochTime() {
		return (new Date()).getTime();
	}
}
