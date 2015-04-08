package application;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;

public final class DateUtils {
	static Calendar calendar;

	static {
		calendar = Calendar.getInstance(TimeZone.getTimeZone("UTC"));
		calendar.clear();
		calendar.set(2011, Calendar.OCTOBER, 1);
	}
	
	public static String timestampToDateString(long timestamp, String format) {
		Date date = new Date(timestamp);
		SimpleDateFormat formatter = new SimpleDateFormat(format);
		
		return formatter.format(date);
	}
	
	public static long getEpochTime() {
		return calendar.getTimeInMillis();
	}
}
