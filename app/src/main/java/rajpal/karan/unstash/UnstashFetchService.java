package rajpal.karan.unstash;

import android.app.IntentService;
import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.annotation.Nullable;
import android.util.Log;

import com.fasterxml.jackson.databind.JsonNode;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;
import net.dean.jraw.paginators.UserContributionPaginator;

import timber.log.Timber;

import static android.content.ContentValues.TAG;

public class UnstashFetchService extends IntentService {

    public static final String ACTION_START_FETCH_SERVICE = "start-fetch";
    public static final String ACTION_MARK_POST_AS_DONE = "mark-as-done";
    public static final String ACTION_DISMISS_NOTIFICATION = "dismiss-notification";
    public static final String ACTION_READ_POST_REMINDER = "read-post-reminder";

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
        Log.d("onHandleIntent", "onHandleIntent: Action " + action);

        switch (action) {
            case ACTION_START_FETCH_SERVICE:
                Log.d("TAG", "onHandleIntent: Starting fetch");
                RedditClient redditClient = AuthenticationManager.get().getRedditClient();
                UserContributionPaginator saved = new UserContributionPaginator(redditClient, "saved", redditClient.me().getFullName());
                saved.setTimePeriod(TimePeriod.DAY);
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
                            try {
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
                            } catch (Exception e) {
                                Timber.d(e.getMessage());
                            }
                        } else {
                            Timber.d("Post already present. Skipping post " + id);
                        }

                    }
                    saved.next(true);
                }
                break;
            case ACTION_MARK_POST_AS_DONE:
                Timber.d(TAG, "onHandleIntent: Mark as done case");
                break;
            case ACTION_READ_POST_REMINDER:
                Timber.d("Read post reminder case");
            default:
                NotificationUtils.executeTask(getApplicationContext(), action);
                break;
        }

		/*assert intent != null;
        String val = intent.getStringExtra("foo");
		Timber.d(val + ACTION_START_FETCH_SERVICE);
		Intent in = new Intent(ACTION_START_FETCH_SERVICE);
		in.putExtra("ResultCode", Activity.RESULT_OK);
		LocalBroadcastManager.getInstance(this).sendBroadcast(in);*/
    }
}
