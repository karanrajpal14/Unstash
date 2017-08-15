package rajpal.karan.unstash;

import android.text.format.DateUtils;

public class Utils {
	public static String getRelativeTime(long createdTime) {
		return (String) DateUtils.getRelativeTimeSpanString(
				createdTime,
				System.currentTimeMillis(),
				DateUtils.MINUTE_IN_MILLIS,
				DateUtils.FORMAT_ABBREV_RELATIVE
		);
	}
}
