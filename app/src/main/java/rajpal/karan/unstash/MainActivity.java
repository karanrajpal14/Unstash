package rajpal.karan.unstash;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.webkit.CookieManager;
import android.widget.TextView;
import android.widget.Toast;

import com.fasterxml.jackson.databind.JsonNode;
import com.santalu.emptyview.EmptyView;

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

public class MainActivity extends AppCompatActivity
		implements SavedPostsAdapter.ListItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {

	public static final String TAG = MainActivity.class.getSimpleName();
	static final int MAIN_LOADER_ID = 0;
	int position = RecyclerView.NO_POSITION;
	@BindView(R.id.posts_list_rv)
	RecyclerView postsListRecyclerView;
	@BindView(R.id.empty_view)
	EmptyView emptyView;
	@BindView(R.id.toolbar)
	Toolbar myToolbar;
	private RedditClient redditClient;
	/*
	 * References to RecyclerView and Adapter to reset the list to its
	 * "pretty" state when the reset menu item is clicked.
	 */
	private SavedPostsAdapter mAdapter;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		ButterKnife.bind(this);
		setSupportActionBar(myToolbar);

		redditClient = AuthenticationManager.get().getRedditClient();

        /*
         * A LinearLayoutManager is responsible for measuring and positioning item views within a
         * RecyclerView into a linear list. This means that it can produce either a horizontal or
         * vertical list depending on which parameter you pass in to the LinearLayoutManager
         * constructor. By default, if you don't specify an orientation, you get a vertical list.
         * In our case, we want a vertical list, so we don't need to pass in an orientation flag to
         * the LinearLayoutManager constructor.
         *
         * There are other LayoutManagers available to display your data in uniform grids,
         * staggered grids, and more! See the developer documentation for more details.
         */
		LinearLayoutManager layoutManager = new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false);
		postsListRecyclerView.setLayoutManager(layoutManager);

        /*
         * Use this setting to improve performance if you know that changes in content do not
         * change the child layout size in the RecyclerView
         */
		postsListRecyclerView.setHasFixedSize(true);

        /*
         * The SavedPostsAdapter is responsible for displaying each item in the list.
         */
		mAdapter = new SavedPostsAdapter(this, this);
		postsListRecyclerView.setAdapter(mAdapter);

		getSupportLoaderManager().initLoader(MAIN_LOADER_ID, null, this);

	}

	public void fetchSavedPosts() {
		new AsyncTask<Void, Integer, ContentValues>() {

			@Override
			protected ContentValues doInBackground(Void... voids) {
				redditClient = AuthenticationManager.get().getRedditClient();
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

		TextView appTitle = findViewById(R.id.app_title_main_tv);
		appTitle.setText(R.string.app_name);
		TextView username = findViewById(R.id.username_main_tv);

		showEmpty();

		if (redditClient.isAuthenticated()) {
			username.setText(redditClient.getAuthenticatedUser());
		} else {
			username.setText(R.string.toolbar_not_logged_in);
		}

		switch (state) {
			case READY:
				break;
			case NONE:
				showEmpty();
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

	@Override
	public void onListItemClick(Intent intent) {
		startActivity(intent);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		switch (id) {
			case MAIN_LOADER_ID:

				return new CursorLoader(
						this,
						SavedPostContract.SavedPostEntry.CONTENT_URI,
						null,
						null,
						null,
						null
				);
			default:
				throw new RuntimeException("Loader not implemented: " + id);
		}
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		mAdapter.swapCursor(data);
		emptyView.showContent();
		if (position == RecyclerView.NO_POSITION) position = 0;
		postsListRecyclerView.smoothScrollToPosition(position);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		mAdapter.swapCursor(null);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.main, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		int id = item.getItemId();

		switch (id) {
			case R.id.action_fetch:
				fetchSavedPosts();
				return true;
			case R.id.action_logout:
				Timber.d("Action logout");
				logout();
				return true;
		}
		return super.onOptionsItemSelected(item);
	}

	private void logout() {
		new AsyncTask<Void, Void, Boolean>() {
			@Override
			protected Boolean doInBackground(Void... voids) {
				Timber.d("logging out");
				redditClient.getOAuthHelper().revokeAccessToken(LoginActivity.CREDENTIALS);
				redditClient.deauthenticate();
				CookieManager.getInstance().removeAllCookies(null);
				return redditClient.isAuthenticated();
			}

		}.execute();

	}

	public void showEmpty() {
		Log.d(TAG, "showEmpty: Empty");
		if (mAdapter.getItemCount() == 0) {
			emptyView.showEmpty();
		}
	}
}
