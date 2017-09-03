package rajpal.karan.unstash;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

import timber.log.Timber;

import static rajpal.karan.unstash.UnstashFetchService.ACTION_DISMISS_NOTIFICATION;
import static rajpal.karan.unstash.UnstashFetchService.ACTION_MARK_POST_AS_DONE;
import static rajpal.karan.unstash.UnstashFetchService.ACTION_READ_POST_REMINDER;

/**
 * Utility class for creating Saved Post notifications
 */
public class NotificationUtils {

    /**
     * This pending intent id is used to uniquely reference the pending intent
     */
    private static final int POST_REMINDER_PENDING_INTENT_ID = 1121;
    private static final int ACTION_MARK_POST_AS_DONE_PENDING_INTENT_ID = 11214;
    private static final int ACTION_IGNORE_PENDING_INTENT_ID = 112143;

    private static final int POST_REMINDER_NOTIFICATION_ID = 8139;

    public static void executeTask(final Context context, String action) {
        Timber.d("In execute task");
        switch (action) {
            case ACTION_MARK_POST_AS_DONE:
                Timber.d("Marked as done action");
                clearAllCreatedNotifications(context);
                break;
            case ACTION_DISMISS_NOTIFICATION:
                Timber.d("Dismissing notification action");
                clearAllCreatedNotifications(context);
                break;
            case ACTION_READ_POST_REMINDER:
                Timber.d("Setting up reminder action");
                notifyReadPost(context);
                break;
        }
    }

    private static void notifyReadPost(Context context) {
        // Add method to update read post
        remindUserToReadSavedPost(context);
    }

    private static NotificationCompat.Action ignoreReminderAction(Context context) {
        Intent ignoreReminderIntent = new Intent(context, UnstashFetchService.class);
        ignoreReminderIntent.setAction(ACTION_DISMISS_NOTIFICATION);
        PendingIntent ignorePendingIntent = PendingIntent.getService(context,
                ACTION_IGNORE_PENDING_INTENT_ID,
                ignoreReminderIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action ignoreReminderAction = new NotificationCompat.Action(
                R.drawable.ic_error, "Dismiss", ignorePendingIntent
        );
        return ignoreReminderAction;
    }

    private static NotificationCompat.Action markAsDoneAction(Context context, String id) {
        Timber.d("Mark as done action created");
        Intent markAsDoneIntent = new Intent(context, UnstashFetchService.class);
        markAsDoneIntent.setAction(ACTION_MARK_POST_AS_DONE);
        markAsDoneIntent.putExtra(SavedPostContract.SavedPostEntry.COLUMN_POST_ID, id);
        PendingIntent donePendingIntent = PendingIntent.getService(context,
                ACTION_MARK_POST_AS_DONE_PENDING_INTENT_ID,
                markAsDoneIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);
        NotificationCompat.Action doneAction = new NotificationCompat.Action(
                R.drawable.ic_done, "Done", donePendingIntent
        );
        return doneAction;
    }

    private static PendingIntent getContentIntent(Context context) {
        Timber.d("Getting content intent");
        Intent startMainActivityIntent = new Intent(context, MainActivity.class);
        return PendingIntent.getActivity(
                context,
                POST_REMINDER_PENDING_INTENT_ID,
                startMainActivityIntent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );
    }

    private static Bitmap getLargeIcon(Context context) {
        Resources resources = context.getResources();
        return BitmapFactory.decodeResource(resources, R.drawable.ic_markunread_mailbox);
    }

    private static void clearAllCreatedNotifications(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
    }

    public static void remindUserToReadSavedPost(Context context) {
        Timber.d("Reminder to read set");

        Cursor randomPostCursor = context.getContentResolver().query(
                SavedPostContract.SavedPostEntry.CONTENT_URI_RANDOM,
                null,
                null,
                null,
                null
        );

        if (randomPostCursor != null && randomPostCursor.getCount() == 1) {
            randomPostCursor.moveToFirst();
            String postID = randomPostCursor.getString(SavedPostContract.SavedPostEntry.INDEX_POST_ID);
            Timber.d(postID);
            String postTitle = randomPostCursor.getString(SavedPostContract.SavedPostEntry.INDEX_TITLE);
            String postDetails = context.getResources().getString(
                    R.string.post_details_textview,
                    randomPostCursor.getString(SavedPostContract.SavedPostEntry.INDEX_AUTHOR),
                    Utils.getRelativeTime(randomPostCursor.getInt(SavedPostContract.SavedPostEntry.INDEX_CREATED_TIME)),
                    randomPostCursor.getString(SavedPostContract.SavedPostEntry.INDEX_SUBREDDIT_NAME)
            );
            randomPostCursor.close();

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            final String NOTIFICATION_CHANNEL_ID = "UnstashPostReminderChannel";
            final CharSequence NOTIFICATION_CHANNEL_NAME = context.getString(R.string.notification_channel_name);
            final String NOTIFICATION_CHANNEL_DESCRIPTION = context.getString(R.string.notification_channel_description);

            int NOTIFICATION_CHANNEL_IMPORTANCE = 0;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                NOTIFICATION_CHANNEL_IMPORTANCE = NotificationManager.IMPORTANCE_HIGH;
            }

            NotificationChannel channel;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                channel = new NotificationChannel(NOTIFICATION_CHANNEL_ID, NOTIFICATION_CHANNEL_NAME, NOTIFICATION_CHANNEL_IMPORTANCE);
                channel.setDescription(NOTIFICATION_CHANNEL_DESCRIPTION);
                channel.enableLights(true);
                channel.setLightColor(context.getColor(R.color.colorPrimary));
                channel.enableVibration(true);
                notificationManager.createNotificationChannel(channel);
            }

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, NOTIFICATION_CHANNEL_ID)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setSmallIcon(R.drawable.ic_stat_markunread_mailbox)
                    .setLargeIcon(getLargeIcon(context))
                    .setContentTitle("Let's read something new. Shall we?")
                    .setContentText(postTitle)
                    .setStyle(
                            new NotificationCompat.BigTextStyle().bigText(
                                    postTitle + "\r\n" + postDetails
                            )
                    )
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setContentIntent(getContentIntent(context))
                    .setAutoCancel(true)
                    .addAction(markAsDoneAction(context, postID))
                    .addAction(ignoreReminderAction(context))
                    .setPriority(NOTIFICATION_CHANNEL_IMPORTANCE);

            notificationManager.notify(POST_REMINDER_NOTIFICATION_ID, builder.build());
        }
    }
}
