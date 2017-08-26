package rajpal.karan.unstash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
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

import com.santalu.emptyview.EmptyView;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.NoSuchTokenException;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthException;

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
    RedditClient redditClient;
    /*
     * References to RecyclerView and Adapter to reset the list to its
     * "pretty" state when the reset menu item is clicked.
     */
    private SavedPostsAdapter mAdapter;
    private BroadcastReceiver UnstashFetchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            int resultCode = intent.getIntExtra("ResultCode", RESULT_CANCELED);
            if (resultCode == RESULT_OK) {
                Toast.makeText(context, "Fetch completed successfully", Toast.LENGTH_SHORT).show();
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(myToolbar);

//        checkConnection();
        Utils.scheduleReadPostReminder(this);

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

    @Override
    protected void onResume() {
        super.onResume();
        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        Log.d(TAG, "AuthenticationState for onResume(): " + state);

        IntentFilter filter = new IntentFilter(UnstashFetchService.ACTION_START_FETCH_SERVICE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UnstashFetchReceiver, filter);

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
				launchFetchService();
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

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(UnstashFetchReceiver);
    }

    private void refreshAccessTokenAsync() {
        Log.d(TAG, "refreshAccessTokenAsync: Refreshing");
        new AsyncTask<Credentials, Void, Void>() {
            @Override
            protected Void doInBackground(Credentials... params) {
                try {
                    AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
                } catch (NoSuchTokenException | OAuthException | RuntimeException e) {
                    Log.e(TAG, "Could not refresh access token", e);
                }
                return null;
            }

            @Override
            protected void onPostExecute(Void v) {
				launchFetchService();
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
                        SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED + " = 1 ",
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
                launchFetchService();
                return true;
            case R.id.action_logout:
                Timber.d("Action logout");
                logout();
                return true;
            case R.id.test_notification:
                NotificationUtils.remindUserToReadSavedPost(this);
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
        if (mAdapter.getItemCount() == 0) {
            emptyView.showEmpty();
        }
    }

    public void checkConnection() {
        ConnectivityManager manager =
                (ConnectivityManager) this.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetworkInfo = manager.getActiveNetworkInfo();
        boolean isConnected = activeNetworkInfo != null && activeNetworkInfo.isConnectedOrConnecting();
        if (isConnected) {
            emptyView.showContent();
        } else {
            Log.d(TAG, "checkConnection: No internet");
            emptyView.showError("No Internet :(");
        }
    }

    public void launchFetchService() {
        Intent i = new Intent(this, UnstashFetchService.class);
        i.setAction(UnstashFetchService.ACTION_START_FETCH_SERVICE);
        startService(i);
    }
}
