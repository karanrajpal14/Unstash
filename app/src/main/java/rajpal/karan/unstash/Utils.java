package rajpal.karan.unstash;

import android.app.job.JobInfo;
import android.app.job.JobScheduler;
import android.content.ComponentName;
import android.content.Context;
import android.os.Build;
import android.support.annotation.RequiresApi;
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

	// schedule the start of the service every 10 - 30 seconds
	@RequiresApi(api = Build.VERSION_CODES.M)
	public static void scheduleJob(Context context) {
		JobInfo.Builder builder = new JobInfo.Builder(0, new ComponentName(context, TestJobService.class));
		builder.setMinimumLatency(1 * 1000); // wait at least
		builder.setOverrideDeadline(3 * 1000); // maximum delay
		builder.setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED); // require unmetered network
		builder.setRequiresCharging(true); // we don't care if the device is charging or not
		//builder.setRequiresDeviceIdle(true); // device should be idle
		builder.setOverrideDeadline(300);
		JobScheduler jobScheduler = context.getSystemService(JobScheduler.class);
		jobScheduler.schedule(builder.build());
	}
}
