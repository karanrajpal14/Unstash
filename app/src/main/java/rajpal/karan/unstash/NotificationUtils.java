package rajpal.karan.unstash;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Handler;
import android.os.Looper;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;
import android.widget.Toast;

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
        switch (action) {
            case ACTION_MARK_POST_AS_DONE:
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    @Override
                    public void run() {
                        Toast.makeText(context, "To be implemented", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
            case ACTION_DISMISS_NOTIFICATION:
                clearAllCreatedNotifications(context);
                break;
            case ACTION_READ_POST_REMINDER:
                notifyReadPost(context);
                break;
        }
    }

    private static void notifyReadPost(Context context) {
        // Add method to update read post
        remindUserToReadSavedPost(context);
    }

    private static NotificationCompat.Action ignoreReminderAction(Context context) {
        Intent ignoreReminderIntent = new Intent(context, UnstashFetchService.class); //SomeService.class)
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

    private static NotificationCompat.Action markAsDoneAction(Context context) {
        Intent markAsDoneIntent = new Intent(context, UnstashFetchService.class); //SomeService.class)
        markAsDoneIntent.setAction(ACTION_MARK_POST_AS_DONE);
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
        // TODO: 22/8/17 Image size should be 24 px
        return BitmapFactory.decodeResource(resources, R.drawable.ic_markunread_mailbox);
    }

    private static void clearAllCreatedNotifications(Context context) {
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        manager.cancelAll();
    }

    public static void remindUserToReadSavedPost(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_stat_markunread_mailbox)
                .setLargeIcon(getLargeIcon(context))
                .setContentTitle("Post Title")
                .setContentText("Author . Date Saved . Subreddit Name")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(" Big Author . Date Saved . Subreddit Name"))
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentIntent(getContentIntent(context))
                .setAutoCancel(true)
                .addAction(markAsDoneAction(context))
                .addAction(ignoreReminderAction(context));

        builder.setPriority(Notification.PRIORITY_HIGH);

        NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(POST_REMINDER_NOTIFICATION_ID, builder.build());
    }
}
