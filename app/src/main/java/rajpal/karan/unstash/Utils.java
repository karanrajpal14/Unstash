package rajpal.karan.unstash;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.preference.PreferenceManager;
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
import java.util.Date;
import java.util.concurrent.TimeUnit;

import timber.log.Timber;

public class Utils {

    private static final int SYNC_FLEXTIME_SECONDS = 60;
    private static final String REMINDER_JOB_TAG = "read_post_reminder_tag";
    private static int REMINDER_INTERVAL_SECONDS;
    private static String value;
    private static int hourOfDay;
    private static int minute;

    public static String getRelativeTime(long createdTime) {
        return (String) DateUtils.getRelativeTimeSpanString(
                createdTime,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
                DateUtils.FORMAT_ABBREV_RELATIVE
        );
    }

    private static void getTime(String value, int hourOfDay, int minute) {
        int noOfDays = Integer.valueOf(value);
        Timber.d("getTime: 46 - " + noOfDays);
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, noOfDays);
        calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long tomorrowEpoch = calendar.getTimeInMillis();
        long secondsLeft = (tomorrowEpoch - System.currentTimeMillis());
        Timber.d("Job scheduled " + new Date(tomorrowEpoch));
        REMINDER_INTERVAL_SECONDS = (int) TimeUnit.MILLISECONDS.toSeconds(secondsLeft);
    }

    synchronized public static void scheduleReadPostReminder(@NonNull final Context context, @Nullable String value, int hourOfDay, int  minute) {

        Timber.d("scheduleReadPostReminder: 54 - Scheduling");

        if (value == null || hourOfDay < 0 || minute < 0) {
            SharedPreferences settings = PreferenceManager.getDefaultSharedPreferences(context);
            value = settings.getString(context.getString(R.string.pref_notification_frequency_key), "1");
            hourOfDay = Integer.parseInt(settings.getString(context.getString(R.string.pref_notification_time_hour_of_day_key), "9"));
            minute = Integer.parseInt(settings.getString(context.getString(R.string.pref_notification_time_minute_key), "0"));
            Timber.d("scheduleReadPostReminder: 71 - Null, fetched value from sharedprefs " + value);
            Timber.d("scheduleReadPostReminder: 71 - hour < 0, fetched value from sharedprefs " + hourOfDay);
            Timber.d("scheduleReadPostReminder: 71 - minute < 0, fetched value from sharedprefs " + minute);
        }

        if (value.equals(Utils.value) && hourOfDay == Utils.hourOfDay && minute == Utils.minute) {
            Timber.d("scheduleReadPostReminder: 63 - Already initialized");
            return;
        }

        Timber.d("scheduleReadPostReminder: 69 - Value is: " + value);
        getTime(value, hourOfDay, minute);

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

        Utils.value = value;
        Utils.hourOfDay = hourOfDay;
        Utils.minute = minute;
    }

    public static boolean isConnected(Context context) {
        ConnectivityManager manager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = manager.getActiveNetworkInfo();
        return info != null && info.isConnected();
    }
}
