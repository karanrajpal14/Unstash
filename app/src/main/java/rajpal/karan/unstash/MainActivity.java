package rajpal.karan.unstash;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.BaseTransientBottomBar;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.webkit.CookieManager;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

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
    final static String sharedPrefsKey = "mainPrefs";
    final static String showDoneKey = "showDoneKey";
    final static String isSavedKey = "isSavedKey";
    private final String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    int position = RecyclerView.NO_POSITION;
    @BindView(R.id.coordinator_layout_main)
    CoordinatorLayout mainCoordinatorLayout;
    @BindView(R.id.posts_list_empty_view)
    View emptyView;
    @BindView(R.id.empty_view_refresh_button)
    Button refreshButton;
    @BindView(R.id.posts_list_rv)
    StateAwareRecyclerView postsListRecyclerView;
    @BindView(R.id.toolbar)
    Toolbar myToolbar;
    RedditClient redditClient;
    @BindView(R.id.adView)
    AdView adView;
    /*
     * References to RecyclerView and Adapter to reset the list to its
     * "pretty" state when the reset menu item is clicked.
     */
    SavedPostsAdapter mAdapter;
    SharedPreferences prefs;
    private FirebaseAnalytics firebaseAnalytics;
    private BroadcastReceiver UnstashFetchReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            int resultCode = intent.getIntExtra(UnstashFetchService.INTENT_KEY_RESULT_CODE, RESULT_CANCELED);
            String action = intent.getAction();
            Timber.d(action + ' ' + resultCode);
            switch (action) {
                case UnstashFetchService.ACTION_MARK_POST_AS_DONE:
                    if (resultCode == UnstashFetchService.INTENT_EXTRA_RESULT_OK) {
                        mAdapter.notifyDataSetChanged();
                    } else if (resultCode == UnstashFetchService.INTENT_EXTRA_RESULT_NO_NETWORK) {
                        Toast.makeText(context, "Unstash: Please connect to the internet to mark this post as \"done\"", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case UnstashFetchService.ACTION_START_FETCH_SERVICE:
                    if (resultCode == UnstashFetchService.INTENT_EXTRA_RESULT_OK) {
                        Snackbar.make(mainCoordinatorLayout, "Fetch completed successfully", BaseTransientBottomBar.LENGTH_LONG).show();
                    }
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);
        setSupportActionBar(myToolbar);

        prefs = getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE);
        SharedPreferences.Editor prefsEditor = prefs.edit();
        prefsEditor.putBoolean(showDoneKey, true);
        prefsEditor.apply();

        Utils.scheduleReadPostReminder(this);

        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        firebaseAnalytics.setAnalyticsCollectionEnabled(true);
        firebaseAnalytics.setMinimumSessionDuration(5000);
        firebaseAnalytics.setSessionTimeoutDuration(1000000);

        redditClient = AuthenticationManager.get().getRedditClient();

        MobileAds.initialize(this, ADMOB_APP_ID);
        // Create an ad request. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("ABCDEF012345")
                .build();
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Timber.d("Ads", "onAdLoaded");
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "AD_SHOWN");
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "AD_SHOWN");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Ad");
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Timber.d("Ads", "onAdFailedToLoad error no = " + errorCode);
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Timber.d("Ads", "onAdOpened");
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "AD_CLICKED");
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "AD_CLICKED");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Ad");
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
                adView.pause();
            }

            @Override
            public void onAdLeftApplication() {
                // Code to be executed when the user has left the app.
                Timber.d("Ads", "onAdLeftApplication");
                adView.destroy();
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
                Timber.d("Ads", "onAdClosed");
                adView.resume();
            }
        });

        final int columns = getResources().getInteger(R.integer.grid_columns);
        GridLayoutManager gridLayoutManager = new GridLayoutManager(this, columns);
        postsListRecyclerView.setLayoutManager(gridLayoutManager);

        postsListRecyclerView.setEmptyView(emptyView);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchFetchService();
            }
        });
        mAdapter = new SavedPostsAdapter(this, this);
        postsListRecyclerView.setAdapter(mAdapter);

        Bundle bundle = new Bundle();
        bundle.putInt(isSavedKey, 1);
        getSupportLoaderManager().initLoader(MAIN_LOADER_ID, bundle, this);

    }

    @Override
    protected void onResume() {
        super.onResume();
        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        Timber.d("AuthenticationState for onResume(): " + state);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UnstashFetchService.ACTION_START_FETCH_SERVICE);
        filter.addAction(UnstashFetchService.ACTION_MARK_POST_AS_DONE);
        LocalBroadcastManager.getInstance(this).registerReceiver(UnstashFetchReceiver, filter);

        TextView appTitle = findViewById(R.id.app_title_main_tv);
        appTitle.setText(R.string.app_name);
        TextView username = findViewById(R.id.username_main_tv);

        if (redditClient.isAuthenticated()) {
            username.setText(redditClient.getAuthenticatedUser());
        } else {
            username.setText(R.string.main_toolbar_not_logged_in);
        }

        switch (state) {
            case READY:
                launchFetchService();
                adView.setVisibility(View.VISIBLE);
                break;
            case NONE:
                adView.setVisibility(View.GONE);
                Snackbar.make(
                        mainCoordinatorLayout,
                        "Please login to continue",
                        Snackbar.LENGTH_INDEFINITE)
                        .setAction("Login", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                startActivity(new Intent(getApplicationContext(), LoginActivity.class));
                            }
                        })
                        .show();
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
        Timber.d("refreshAccessTokenAsync: Refreshing");
        new AsyncTask<Credentials, Void, Void>() {
            @Override
            protected Void doInBackground(Credentials... params) {
                try {
                    AuthenticationManager.get().refreshAccessToken(LoginActivity.CREDENTIALS);
                } catch (NoSuchTokenException | OAuthException | RuntimeException e) {
                    Timber.e("Could not refresh access token", e);
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
        Bundle bundle = new Bundle();
        bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "POST_CLICKED");
        bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "POST_CLICKED");
        bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Button");
        firebaseAnalytics.logEvent(FirebaseAnalytics.Event.SELECT_CONTENT, bundle);
        startActivity(intent);
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        String savedArg = "0";
        if (args != null) {
            savedArg = String.valueOf(args.getInt(isSavedKey));
            Timber.d("Loader Saved Arg" + savedArg);
        }
        switch (id) {
            case MAIN_LOADER_ID:

                return new CursorLoader(
                        this,
                        SavedPostContract.SavedPostEntry.CONTENT_URI,
                        null,
                        SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED + " = " + savedArg,
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
        if (position == RecyclerView.NO_POSITION) position = 0;
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mAdapter.swapCursor(null);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {

        prefs = getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE);
        boolean status = prefs.getBoolean(showDoneKey, true);
        if (status) {
            menu.findItem(R.id.show_done).setTitle(getString(R.string.menu_show_done_string));
        } else {
            menu.findItem(R.id.show_done).setTitle(getString(R.string.menu_show_todo_string));
        }
        return true;
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
            case R.id.action_refresh:
                launchFetchService();
                return true;
            case R.id.action_logout:
                Timber.d("Action logout");
                logout();
                return true;
            case R.id.test_notification:
                NotificationUtils.remindUserToReadSavedPost(this);
                return true;
            case R.id.show_done:
                prefs = getSharedPreferences(sharedPrefsKey, Context.MODE_PRIVATE);
                SharedPreferences.Editor editor = prefs.edit();
                boolean status = prefs.getBoolean(showDoneKey, false);
                if (status) {
                    // is saved is 0 here
                    Timber.d(String.valueOf(prefs.getBoolean(showDoneKey, false)));
                    editor.clear();
                    Bundle notSavedBundle = new Bundle();
                    notSavedBundle.putInt(isSavedKey, 0);
                    getSupportLoaderManager().restartLoader(MAIN_LOADER_ID, notSavedBundle, this);
                    editor.putBoolean(showDoneKey, false);
                    editor.apply();
                    Timber.d(String.valueOf(prefs.getBoolean(showDoneKey, false)));
                    invalidateOptionsMenu();
                } else {
                    // is saved is 1 here
                    Timber.d(String.valueOf(prefs.getBoolean(showDoneKey, false)));
                    editor.clear();
                    Bundle savedBundle = new Bundle();
                    savedBundle.putInt(isSavedKey, 1);
                    getSupportLoaderManager().restartLoader(MAIN_LOADER_ID, savedBundle, this);
                    editor.putBoolean(showDoneKey, true);
                    editor.apply();
                    Timber.d(String.valueOf(prefs.getBoolean(showDoneKey, false)));
                    invalidateOptionsMenu();
                }
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

    public void launchFetchService() {
        Intent i = new Intent(this, UnstashFetchService.class);
        i.setAction(UnstashFetchService.ACTION_START_FETCH_SERVICE);
        startService(i);
    }
}