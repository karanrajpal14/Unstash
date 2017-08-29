package rajpal.karan.unstash;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.text.format.DateUtils;

import com.firebase.jobdispatcher.Constraint;
import com.firebase.jobdispatcher.Driver;
import com.firebase.jobdispatcher.FirebaseJobDispatcher;
import com.firebase.jobdispatcher.GooglePlayDriver;
import com.firebase.jobdispatcher.Job;
import com.firebase.jobdispatcher.Lifetime;
import com.firebase.jobdispatcher.RetryStrategy;
import com.firebase.jobdispatcher.Trigger;

import java.util.Calendar;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class Utils {

    private static final int SYNC_FLEXTIME_SECONDS = 60;
    private static final String REMINDER_JOB_TAG = "read_post_reminder_tag";
    private static int REMINDER_INTERVAL_SECONDS;
    private static boolean initialized;

    public static String getRelativeTime(long createdTime) {
        return (String) DateUtils.getRelativeTimeSpanString(
                createdTime,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        );
    }

    private static void getTime() {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, 1);
        calendar.set(Calendar.HOUR_OF_DAY, 9);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long tomorrowEpoch = calendar.getTimeInMillis();
        long secondsLeft = (tomorrowEpoch - System.currentTimeMillis());
        Timber.d("Job scheduled for " + getRelativeTime(tomorrowEpoch));
        REMINDER_INTERVAL_SECONDS = (int) TimeUnit.MILLISECONDS.toSeconds(secondsLeft);
    }

    synchronized public static void scheduleReadPostReminder(@NonNull final Context context) {

        if (initialized) return;

        getTime();
        if (REMINDER_INTERVAL_SECONDS < 0) {
            REMINDER_INTERVAL_SECONDS = (int) TimeUnit.MINUTES.toSeconds(60);
        }

        Driver driver = new GooglePlayDriver(context);
        FirebaseJobDispatcher dispatcher = new FirebaseJobDispatcher(driver);
        Job constrainedReminderJob = dispatcher.newJobBuilder()
                .setService(TestJobService.class)
                .setTag(REMINDER_JOB_TAG)
                .setConstraints(Constraint.DEVICE_CHARGING)
                .setConstraints(Constraint.ON_UNMETERED_NETWORK)
                .setRetryStrategy(RetryStrategy.DEFAULT_EXPONENTIAL)
                .setLifetime(Lifetime.FOREVER)
                .setRecurring(true)
                .setTrigger(Trigger.executionWindow(REMINDER_INTERVAL_SECONDS,
                        REMINDER_INTERVAL_SECONDS + SYNC_FLEXTIME_SECONDS))
                .setReplaceCurrent(true)
                .build();

        dispatcher.schedule(constrainedReminderJob);
        initialized = true;
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
