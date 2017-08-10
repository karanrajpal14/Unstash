package rajpal.karan.unstash;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthException;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Contribution;
import net.dean.jraw.models.Listing;
import net.dean.jraw.models.Submission;
import net.dean.jraw.paginators.TimePeriod;
import net.dean.jraw.paginators.UserContributionPaginator;

import java.util.Date;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
	public static final String TAG = MainActivity.class.getSimpleName();
	private RedditClient redditClient;
	private AccountManager accountManager;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		redditClient = AuthenticationManager.get().getRedditClient();
		accountManager = new AccountManager(redditClient);
	}

	public void userInfo(View view) {
		startActivity(new Intent(this, UserInfoActivity.class));
	}

	public void savePost(View view) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				String id = "15lrh7";
				Submission submission = redditClient.getSubmission(id);
				try {
					accountManager.save(submission);
				} catch (ApiException e) {
					e.printStackTrace();
				}
				return null;
			}
		}.execute();

	}

	public void unsavePost(View view) {
		new AsyncTask<Void, Void, Void>() {
			@Override
			protected Void doInBackground(Void... voids) {
				String id = "15lrh7";
				Submission submission = redditClient.getSubmission(id);
				try {
					accountManager.unsave(submission);
				} catch (ApiException e) {
					e.printStackTrace();
				}
				return null;
			}
		}.execute();
	}

	public void fetchMetadataFromIDs(final String id) {
		new AsyncTask<Void, Void, Void>() {

			@Override
			protected Void doInBackground(Void... voids) {
				Submission submission = redditClient.getSubmission(id);
				String author = submission.getAuthor();
				Date created = submission.getCreated();
				String domain = submission.getDomain();
				String permalink = submission.getPermalink();
				Submission.PostHint postHint = submission.getPostHint();
				Integer score = submission.getScore();
				String subredditName = submission.getSubredditName();
				String title = submission.getTitle();
				String url = submission.getUrl();
				Boolean nsfw = submission.isNsfw();
				Boolean saved = submission.isSaved();
				Timber.d("Author " + author);
				Timber.d("Created " + created);
				Timber.d("Domain " + domain);
				Timber.d("Permalink " + permalink);
				Timber.d("PostHint " + postHint);
				Timber.d("Score " + score);
				Timber.d("SubredditName " + subredditName);
				Timber.d("Title " + title);
				Timber.d("Url " + url);
				Timber.d("NSFW " + nsfw);
				Timber.d("Saved " + saved);
				return null;
			}
		}.execute();
	}

	public void getSavedPostIDs(View view) {
		Timber.d("Saved posts");
		new AsyncTask<Object, Object, UserContributionPaginator>() {

			@Override
			protected UserContributionPaginator doInBackground(Object... voids) {
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
						Timber.d("Item " + i + id);
						fetchMetadataFromIDs(id);
						i++;
					}
					saved.next();
				}
				return saved;
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
				Log.d(TAG, "Reauthenticated");
			}
		}.execute();
	}

}
