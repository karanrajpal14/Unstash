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

    private static final String REFRESH_ACTION = AppWidgetManager.ACTION_APPWIDGET_UPDATE;

    public static void refreshWidgetBroadcast(Context context) {
        Timber.d("Received refresh broadcast from CP");
        Intent intent = new Intent(REFRESH_ACTION);
        intent.setComponent(new ComponentName(context, TodoCountWidgetProvider.class));
        context.sendBroadcast(intent);
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();

        if (action.equals(REFRESH_ACTION)) {
            Timber.d("Refreshing widget on CP change");
            AppWidgetManager manager = AppWidgetManager.getInstance(context);
            ComponentName name = new ComponentName(context, SavedPostProvider.class);
            manager.notifyAppWidgetViewDataChanged(manager.getAppWidgetIds(name), R.id.todo_widget_textview);
            manager.notifyAppWidgetViewDataChanged(manager.getAppWidgetIds(name), R.id.done_widget_textview);
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        for (int appWidgetId : appWidgetIds) {
            RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_todo_count);

            int todoWidgetTVID = R.id.todo_widget_textview;
            int doneWidgetTVID = R.id.done_widget_textview;

            Intent intent = new Intent(context, MainActivity.class);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);

            views.setOnClickPendingIntent(todoWidgetTVID, pendingIntent);
            views.setOnClickPendingIntent(doneWidgetTVID, pendingIntent);

            int todoCount;
            int doneCount;

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
                todoCount = 0;
                doneCount = 0;
            }

            views.setTextViewText(
                    todoWidgetTVID,
                    context.getResources().getString(R.string.widget_todo_string, todoCount)
            );
            views.setTextViewText(
                    doneWidgetTVID,
                    context.getResources().getString(R.string.widget_done_string, doneCount)
            );

            appWidgetManager.updateAppWidget(appWidgetId, views);
        }

    }
}
