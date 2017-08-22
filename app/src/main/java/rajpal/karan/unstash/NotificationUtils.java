package rajpal.karan.unstash;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.support.v4.app.NotificationCompat;
import android.support.v4.content.ContextCompat;

/**
 * Utility class for creating Saved Post notifications
 */
public class NotificationUtils {

    /**
     * This pending intent id is used to uniquely reference the pending intent
     */
    private static final int POST_REMINDER_PENDING_INTENT_ID = 1121;
    private static final int POST_REMINDER_NOTIFICATION_ID = 8139;

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
        return BitmapFactory.decodeResource(resources, R.drawable.ic_done);
    }

    public static void remindUserToReadSavedPost(Context context) {
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                .setSmallIcon(R.drawable.ic_cancel)
                .setLargeIcon(getLargeIcon(context))
                .setContentTitle("Post Title")
                .setContentText("Author . Date Saved . Subreddit Name")
                .setStyle(new NotificationCompat.BigTextStyle().bigText(" Big Author . Date Saved . Subreddit Name"))
                .setDefaults(Notification.DEFAULT_VIBRATE)
                .setContentIntent(getContentIntent(context))
                .setAutoCancel(true);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
            builder.setPriority(Notification.PRIORITY_HIGH);

            NotificationManager notificationManager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
            notificationManager.notify(POST_REMINDER_NOTIFICATION_ID, builder.build());
        }
    }
}
