package rajpal.karan.unstash;

import android.app.Activity;
import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;

import com.fasterxml.jackson.databind.JsonNode;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;
import net.dean.jraw.paginators.UserContributionPaginator;

import timber.log.Timber;

public class UnstashFetchService extends IntentService {

    public static final String ACTION_START_FETCH_SERVICE = "start-fetch";
    public static final String ACTION_MARK_POST_AS_DONE = "mark-as-done";
    public static final String ACTION_DISMISS_NOTIFICATION = "dismiss-notification";
    public static final String ACTION_READ_POST_REMINDER = "read-post-reminder";
    public static final String INTENT_KEY_RESULT_CODE = "result-code";
    public static final int INTENT_EXTRA_RESULT_NO_NETWORK = -1;
    public static final int INTENT_EXTRA_RESULT_OK = 1;

    public UnstashFetchService() {
        super(UnstashFetchService.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Timber.d("Created service");
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {

        Timber.d("Received intent");
        assert intent != null;
        String action = intent.getAction() != null ? intent.getAction() : "default";
        Timber.d("onHandleIntent: Action " + action);

        switch (action) {
            case ACTION_START_FETCH_SERVICE:
                Timber.d("onHandleIntent: Starting fetch");
                syncImmediately();
                break;
            case ACTION_MARK_POST_AS_DONE:
                String id = intent.getStringExtra(SavedPostContract.SavedPostEntry.COLUMN_POST_ID);
                if (unSavePost(id)) {
                    Timber.d("OK" + Activity.RESULT_OK);
                    intent.putExtra(INTENT_KEY_RESULT_CODE, INTENT_EXTRA_RESULT_OK);
                } else {
                    Timber.d("Not OK");
                    intent.putExtra(INTENT_KEY_RESULT_CODE, INTENT_EXTRA_RESULT_NO_NETWORK);
                }
                LocalBroadcastManager.getInstance(this).sendBroadcast(intent);
                break;
            case ACTION_READ_POST_REMINDER:
                Timber.d("Read post reminder case");
            default:
                NotificationUtils.executeTask(getApplicationContext(), action);
                break;
        }
    }

    public boolean unSavePost(String id) {
        if (Utils.isConnected(this)) {
            RedditClient reddit = AuthenticationManager.get().getRedditClient();
            AccountManager accountManager = new AccountManager(reddit);
            Submission submission = reddit.getSubmission(id);
            try {
                accountManager.unsave(submission);
            } catch (ApiException e) {
                e.printStackTrace();
            }
            return true;
        } else {
            return false;
        }
    }

    public void syncImmediately() {
        try {
            RedditClient redditClient = AuthenticationManager.get().getRedditClient();
            UserContributionPaginator saved = new UserContributionPaginator(redditClient, "saved", redditClient.me().getFullName());
            saved.setTimePeriod(TimePeriod.ALL);
            saved.setSorting(Sorting.NEW);
            ContentValues savedPostValues;
            for (Listing<Contribution> items : saved) {
                for (Contribution item : items) {
                    JsonNode dataNode = item.getDataNode();
                    String id = dataNode.get("id").asText();
                    Cursor checkIfPostPresentCursor = getContentResolver().query(
                            SavedPostContract.SavedPostEntry.CONTENT_URI,
                            null,
                            SavedPostProvider.postWithID,
                            new String[]{id},
                            null
                    );

                    if (checkIfPostPresentCursor != null && checkIfPostPresentCursor.getCount() == 0) {
                        Timber.d("Post not present. Inserting.");
                        Submission submission = redditClient.getSubmission(id);

                        if (submission != null) {

                            savedPostValues = new ContentValues();

                            // Fetching saved post data
                            String author = submission.getAuthor();
                            long created_time = submission.getCreated().getTime();
                            String domain = submission.getDomain();
                            String permalink = submission.getPermalink();
                            String postHint = submission.getPostHint().toString();
                            int score = submission.getScore();
                            String subredditName = submission.getSubredditName();
                            String title = submission.getTitle();
                            String url = submission.getShortURL();
                            int isNSFW = (submission.isNsfw()) ? 1 : 0;
                            int isSaved = (submission.isSaved()) ? 1 : 0;
                            String thumbnailURL = submission.getThumbnail();

                            savedPostValues.put("post_id", id);
                            savedPostValues.put("title", title);
                            savedPostValues.put("author", author);
                            savedPostValues.put("thumbnail_url", thumbnailURL);
                            savedPostValues.put("created_time", created_time);
                            savedPostValues.put("subreddit_name", subredditName);
                            savedPostValues.put("domain", domain);
                            savedPostValues.put("post_hint", postHint);
                            savedPostValues.put("permalink", permalink);
                            savedPostValues.put("url", url);
                            savedPostValues.put("score", score);
                            savedPostValues.put("is_nsfw", isNSFW);
                            savedPostValues.put("is_saved", isSaved);

                            getContentResolver().insert(SavedPostContract.SavedPostEntry.CONTENT_URI, savedPostValues);
                        }
                    } else {
                        Timber.d("Post already present. Skipping post " + id);
                    }
                    assert checkIfPostPresentCursor != null;
                    checkIfPostPresentCursor.close();
                }
            }
            saved.next(true);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
