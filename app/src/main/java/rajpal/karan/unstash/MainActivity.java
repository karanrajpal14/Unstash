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
			getSavedPostIDs();
		}
	}

	public void getSavedPostIDs() {
		Timber.d("Saved posts");
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... voids) {
				RedditClient redditClient = AuthenticationManager.get().getRedditClient();
				UserContributionPaginator saved = new UserContributionPaginator(redditClient, "saved", redditClient.me().getFullName());
				Timber.d(String.valueOf(saved.getTimePeriod()));
				saved.setTimePeriod(TimePeriod.WEEK);
				Timber.d(String.valueOf(saved.getTimePeriod()));
				int i = 0;
				for (Listing<Contribution> items : saved) {
					for (Contribution item : items) {
						JsonNode dataNode = item.getDataNode();
						String id = dataNode.get("id").asText();
						fetchMetadataFromIDs(id);
						i++;
					}
					saved.next();
				}
				Timber.d("Fetched " + i + " posts");
				return null;
			}
		}.execute();
	}

	public void fetchMetadataFromIDs(final String id) {
		new AsyncTask<String, Void, ContentValues>() {

			@Override
			protected ContentValues doInBackground(String... params) {
				Submission submission;
				ContentValues savedPostValues = new ContentValues();
				try {
					submission = redditClient.getSubmission(id);

					if (submission != null) {
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

						savedPostValues.put("id", id);
						savedPostValues.put("author", author);
						savedPostValues.put("created_time", created_time);
						savedPostValues.put("domain", domain);
						savedPostValues.put("permalink", permalink);
						savedPostValues.put("postHint", postHint);
						savedPostValues.put("score", score);
						savedPostValues.put("subredditName", subredditName);
						savedPostValues.put("title", title);
						savedPostValues.put("url", url);
						savedPostValues.put("isNSFW", isNSFW);
						savedPostValues.put("isSaved", isSaved);
					}
				} catch (Exception e) {
					Timber.d(e.getMessage());
				}

				return savedPostValues;
			}

			@Override
			protected void onPostExecute(ContentValues contentValues) {
				super.onPostExecute(contentValues);
				Timber.d(contentValues.valueSet().toString());
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
