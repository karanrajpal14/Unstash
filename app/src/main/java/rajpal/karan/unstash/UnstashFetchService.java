package rajpal.karan.unstash;

import android.app.IntentService;
import android.content.Intent;
import android.support.annotation.Nullable;
import android.util.Log;

import timber.log.Timber;

public class UnstashFetchService extends IntentService {

	public static final String ACTION_START_FETCH_SERVICE = "start-fetch";
	public static final String ACTION_MARK_POST_AS_DONE = "mark-as-done";
	public static final String ACTION_DISMISS_NOTIFICATION = "dismiss-notification";

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
		String action = intent.getAction();
		Log.d("onHandleIntent", "onHandleIntent: Action " + action);
		NotificationUtils.executeTask(this, action);

		switch (action) {
			case ACTION_START_FETCH_SERVICE:
				Log.d("TAG", "onHandleIntent: Starting fetch");
				/*RedditClient redditClient = AuthenticationManager.get().getRedditClient();
		UserContributionPaginator saved = new UserContributionPaginator(redditClient, "saved", redditClient.me().getFullName());
		saved.setTimePeriod(TimePeriod.WEEK);
		saved.setLimit(0);
		saved.setSorting(Sorting.NEW);
		ContentValues savedPostValues;
		for (Listing<Contribution> items : saved) {
			for (Contribution item : items) {
				Log.d(this.getClass().getSimpleName(), "onHandleIntent: Fetching");
				JsonNode dataNode = item.getDataNode();
				String id = dataNode.get("id").asText();
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
			}
			saved.next(true);
		}*/
				break;
			default:
				NotificationUtils.executeTask(getApplicationContext(),action);
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
