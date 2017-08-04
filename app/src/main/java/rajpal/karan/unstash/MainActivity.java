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
import net.dean.jraw.paginators.UserContributionPaginator;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {
	public static final String TAG = MainActivity.class.getSimpleName();
	String id = "15lrh7";

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
	}

	public void login(View view) { startActivity(new Intent(this, LoginActivity.class)); }
	public void userInfo(View view) { startActivity(new Intent(this, UserInfoActivity.class)); }
	public void savePost(View view) {
		new AsyncTask<Void, Void, Void>(){
			@Override
			protected Void doInBackground(Void... voids) {
				RedditClient reddit = AuthenticationManager.get().getRedditClient();
				AccountManager accountManager = new AccountManager(reddit);
				Submission submission = reddit.getSubmission(id);
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
		new AsyncTask<Void, Void, Void>(){
			@Override
			protected Void doInBackground(Void... voids) {
				RedditClient reddit = AuthenticationManager.get().getRedditClient();
				AccountManager accountManager = new AccountManager(reddit);
				Submission submission = reddit.getSubmission(id);
				try {
					accountManager.unsave(submission);
				} catch (ApiException e) {
					e.printStackTrace();
				}
				return null;
			}
		}.execute();
	}
	public void getSavedPosts(View view) {
		Timber.d("SAved posts");
		new AsyncTask<Object, Object, UserContributionPaginator>(){

			@Override
			protected UserContributionPaginator doInBackground(Object... voids) {
				RedditClient redditClient = AuthenticationManager.get().getRedditClient();
				UserContributionPaginator saved = new UserContributionPaginator(redditClient,"saved",redditClient.me().getFullName());
				Timber.d(String.valueOf(saved.getTimePeriod()));
				/*saved.setTimePeriod(TimePeriod.MONTH);
				Timber.d(String.valueOf(saved.getTimePeriod()));*/
				for (Listing<Contribution> items : saved) {
					for (Contribution item: items){
						JsonNode dataNode = item.getDataNode();
						Timber.d("Item1" + dataNode.get("author"));
						Timber.d("Item2" + dataNode.get("created").getClass());
						Timber.d("Item3" + dataNode.get("domain"));
						Timber.d("Item4" + dataNode.get("permalink"));
						Timber.d("Item5" + dataNode.get("post_hint"));
						Timber.d("Item6" + dataNode.get("score"));
						Timber.d("Item7" + dataNode.get("subreddit"));
						Timber.d("Item8" + dataNode.get("title"));
						Timber.d("Item9" + dataNode.get("url"));
						Timber.d("Item10" + dataNode.get("over_18"));
						Timber.d("Item11" + dataNode.get("saved"));
						Timber.d("Item11" + dataNode.get("id"));
					}
					saved.next();
				}
				return saved;
			}

			@Override
			protected void onPostExecute(UserContributionPaginator listings) {
				super.onPostExecute(listings);
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
