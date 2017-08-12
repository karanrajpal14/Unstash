package rajpal.karan.unstash;

import android.content.ContentValues;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.Sorting;
import net.dean.jraw.paginators.TimePeriod;
import net.dean.jraw.paginators.UserContributionPaginator;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity implements View.OnClickListener {
	public static final String TAG = MainActivity.class.getSimpleName();
	@BindView(R.id.getSavedPosts)
	Button getSavedPosts;
	private RedditClient redditClient;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		redditClient = AuthenticationManager.get().getRedditClient();
		ButterKnife.bind(this);

		getSavedPosts.setOnClickListener(this);
	}

	@Override
	public void onClick(View view) {
		if (view.getId() == R.id.getSavedPosts) {
			fetchSavedPosts();
		}
	}

	public void fetchSavedPosts() {
		new AsyncTask<Void, Integer, ContentValues >() {

			@Override
			protected ContentValues doInBackground(Void... voids) {
				RedditClient redditClient = AuthenticationManager.get().getRedditClient();
				UserContributionPaginator saved = new UserContributionPaginator(redditClient, "saved", redditClient.me().getFullName());
				saved.setTimePeriod(TimePeriod.ALL);
				saved.setLimit(0);
				saved.setSorting(Sorting.NEW);
				ContentValues savedPostValues = null;
				for (Listing<Contribution> items : saved) {
					for (Contribution item : items) {
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
								String url = submission.getUrl();
								int isNSFW = (submission.isNsfw()) ? 1 : 0;
								int isSaved = (submission.isSaved()) ? 1 : 0;

								savedPostValues.put("post_id", id);
								savedPostValues.put("title", title);
								savedPostValues.put("author", author);
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
				}
				return savedPostValues;
			}

			@Override
			protected void onPostExecute(ContentValues contentValues) {
				super.onPostExecute(contentValues);
			}
		}.execute();
	}

	@Override
	protected void onResume() {
		super.onResume();
		AuthenticationState state = AuthenticationManager.get().checkAuthState();
		Log.d(TAG, "AuthenticationState for onResume(): " + state);

		switch (state) {
			case READY:
				break;
			case NONE:
				Toast.makeText(MainActivity.this, "Log in first", Toast.LENGTH_SHORT).show();
				startActivity(new Intent(getApplicationContext(), LoginActivity.class));
				break;
			case NEED_REFRESH:
				refreshAccessTokenAsync();
				break;
		}
	}

	private void refreshAccessTokenAsync() {
		new AsyncTask<Credentials, Void, Void>() {
			@Override
			protected Void doInBackground(Credentials... params) {
				try {
					AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
				} catch (NoSuchTokenException | OAuthException e) {
					Log.e(TAG, "Could not refresh access token", e);
				}
				return null;
			}

			@Override
			protected void onPostExecute(Void v) {
				Log.d(TAG, "Re-authenticated");
			}
		}.execute();
	}

}
