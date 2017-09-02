package rajpal.karan.unstash;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import timber.log.Timber;

public class TodoCountWidgetProvider extends AppWidgetProvider {

    static final int todoWidgetTVID = R.id.todo_widget_textview;
    static final int doneWidgetTVID = R.id.done_widget_textview;
    private static final String REFRESH_ACTION = AppWidgetManager.ACTION_APPWIDGET_UPDATE;

    public static void refreshWidgetBroadcast(Context context) {
        Timber.d("Received refresh broadcast from CP");
        Intent intent = new Intent(REFRESH_ACTION);
        intent.setComponent(new ComponentName(context, TodoCountWidgetProvider.class));
        context.sendBroadcast(intent);
    }

    private static RemoteViews setIntents(RemoteViews rm, Context context) {

        Intent intent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

        rm.setOnClickPendingIntent(todoWidgetTVID, pendingIntent);
        rm.setOnClickPendingIntent(doneWidgetTVID, pendingIntent);

        return rm;
    }

    public int[] getCounts(Context context) {
        int todoCount = 0;
        int doneCount = 0;
        int[] counts = new int[2];

        try {

            todoCount = context.getContentResolver().query(
                    SavedPostContract.SavedPostEntry.CONTENT_URI,
                    null,
                    SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED + " = 1",
                    null,
                    null
            ).getCount();

            doneCount = context.getContentResolver().query(
                    SavedPostContract.SavedPostEntry.CONTENT_URI,
                    null,
                    SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED + " = 0",
                    null,
                    null
            ).getCount();

        } catch (NullPointerException e) {
            e.printStackTrace();
        } finally {
            counts[0] = todoCount;
            counts[1] = doneCount;
        }
        return counts;
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(REFRESH_ACTION)) {
            Timber.d("Refreshing widget on CP change");
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_todo_count);
            views = setIntents(views, context);
            int[] counts = getCounts(context);
            views.setTextViewText(
                    todoWidgetTVID,
                    context.getResources().getString(R.string.widget_todo_string, counts[0])
            );
            views.setTextViewText(
                    doneWidgetTVID,
                    context.getResources().getString(R.string.widget_done_string, counts[1])
            );
            ComponentName name = new ComponentName(context, TodoCountWidgetProvider.class);
            AppWidgetManager.getInstance(context).updateAppWidget(name, views);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_todo_count);
            views = setIntents(views, context);

            int[] counts = getCounts(context);

            views.setTextViewText(
                    todoWidgetTVID,
                    context.getResources().getString(R.string.widget_todo_string, counts[0])
            );
            views.setTextViewText(
                    doneWidgetTVID,
                    context.getResources().getString(R.string.widget_done_string, counts[1])
            );

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

    }
}
