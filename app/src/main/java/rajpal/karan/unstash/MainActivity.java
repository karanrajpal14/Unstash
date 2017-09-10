package rajpal.karan.unstash;

import android.app.ActivityManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.firebase.analytics.FirebaseAnalytics;

import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.AuthenticationState;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.auth.TokenStore;
import net.dean.jraw.http.UserAgent;
import net.dean.jraw.http.oauth.Credentials;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.observers.DisposableCompletableObserver;
import io.reactivex.schedulers.Schedulers;
import timber.log.Timber;

public class MainActivity extends AppCompatActivity
        implements SavedPostsAdapter.ListItemClickListener, LoaderManager.LoaderCallbacks<Cursor> {

    public static final int REQUEST_CODE = 1;
    static final int MAIN_LOADER_ID = 0;
    static final String sharedPrefsKey = "mainPrefs";
    static final String showDoneKey = "showDoneKey";
    static final String isSavedKey = "isSavedKey";
    static final String usernameKey = "usernameKey";
    static final String platform = "android";
    static final String packageName = MainActivity.class.getPackage().getName();
    static final String version = BuildConfig.VERSION_NAME;
    static final String redditAppDevUsername = "artemis73";
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
    @BindView(R.id.adView)
    AdView adView;
    RedditClient redditClient;

    SavedPostsAdapter mAdapter;
    SharedPreferences prefs;
    private FirebaseAnalytics firebaseAnalytics;
    private CompositeDisposable disposables = new CompositeDisposable();

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
                        Toast.makeText(
                                context,
                                getString(R.string.main_disconnected_toast_string),
                                Toast.LENGTH_SHORT
                        ).show();
                    }
                    break;
                case UnstashFetchService.ACTION_START_FETCH_SERVICE:
                    if (resultCode == UnstashFetchService.INTENT_EXTRA_RESULT_OK) {
                        Snackbar.make(
                                mainCoordinatorLayout,
                                getString(R.string.main_fetch_completed_snack_string),
                                BaseTransientBottomBar.LENGTH_LONG
                        ).show();
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

        Utils.scheduleReadPostReminder(this, null, -1, -1);

        // Obtain the FirebaseAnalytics instance.
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);

        redditClient = new RedditClient(UserAgent.of(platform,
                packageName, version, redditAppDevUsername));
        TokenStore store = new AndroidTokenStore(
                PreferenceManager.getDefaultSharedPreferences(this));
        RefreshTokenHandler refreshTokenHandler = new RefreshTokenHandler(store, redditClient);

        AuthenticationManager manager = AuthenticationManager.get();
        manager.init(redditClient, refreshTokenHandler);

        // Create an ad request. Check your logcat output for the hashed device ID to
        // get test ads on a physical device. e.g.
        // "Use AdRequest.Builder.addTestDevice("ABCDEF012345") to get test ads on this device."
        AdRequest adRequest = new AdRequest.Builder()
                .addTestDevice("ABCDEF012345")
                .build();
        adView.setContentDescription(getString(R.string.main_ad_content_description));
        adView.loadAd(adRequest);
        adView.setAdListener(new AdListener() {
            @Override
            public void onAdLoaded() {
                // Code to be executed when an ad finishes loading.
                Timber.d("onAdLoaded");
                Bundle bundle = new Bundle();
                bundle.putString(FirebaseAnalytics.Param.ITEM_ID, "AD_SHOWN");
                bundle.putString(FirebaseAnalytics.Param.ITEM_NAME, "AD_SHOWN");
                bundle.putString(FirebaseAnalytics.Param.CONTENT_TYPE, "Ad");
                firebaseAnalytics.logEvent(FirebaseAnalytics.Event.VIEW_ITEM, bundle);
            }

            @Override
            public void onAdFailedToLoad(int errorCode) {
                // Code to be executed when an ad request fails.
                Timber.d("onAdFailedToLoad error number = " + errorCode);
            }

            @Override
            public void onAdOpened() {
                // Code to be executed when an ad opens an overlay that
                // covers the screen.
                Timber.d("onAdOpened");
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
                Timber.d("onAdLeftApplication");
                adView.destroy();
            }

            @Override
            public void onAdClosed() {
                // Code to be executed when when the user is about to return
                // to the app after tapping on an ad.
                Timber.d("onAdClosed");
                adView.resume();
            }
        });

        GridLayoutManager gridLayoutManager = new GridLayoutManager(
                this,
                getResources().getInteger(R.integer.grid_columns)
        );
        postsListRecyclerView.setLayoutManager(gridLayoutManager);

        postsListRecyclerView.setEmptyView(emptyView);
        refreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                launchFetchService();
            }
        });
        refreshButton.setContentDescription(getString(R.string.main_empty_view_refresh_button_text));

        mAdapter = new SavedPostsAdapter(this, this);
        postsListRecyclerView.setAdapter(mAdapter);

        Bundle bundle = new Bundle();
        bundle.putInt(isSavedKey, 1);
        getSupportLoaderManager().initLoader(MAIN_LOADER_ID, bundle, this);

    }

    void updateUsername() {
        TextView appTitle = findViewById(R.id.app_title_main_tv);
        appTitle.setText(R.string.app_name);
        appTitle.setContentDescription(getString(R.string.app_name));
        TextView usernameTV = findViewById(R.id.username_main_tv);

        SharedPreferences.Editor prefsEditor = prefs.edit();

        if (redditClient.isAuthenticated()) {
            prefsEditor.putString(usernameKey, redditClient.getAuthenticatedUser());
            prefsEditor.apply();
        }

        String usernameText = prefs.getString(usernameKey, getString(R.string.main_toolbar_not_logged_in));
        usernameTV.setText(usernameText);
        usernameTV.setContentDescription(usernameText);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_CODE && resultCode == RESULT_OK) {
            Credentials credentials = ((App) getApplication())
                    .getInstalledAppCredentials();

            disposables.add(RedditService.userAuthentication(
                    AuthenticationManager.get().getRedditClient(),
                    credentials,
                    data.getStringExtra("RESULT_URL"))
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableCompletableObserver() {
                        @Override
                        public void onComplete() {
                            String username = AuthenticationManager.get().getRedditClient()
                                    .getAuthenticatedUser();
                            Snackbar.make(mainCoordinatorLayout,
                                    String.format(
                                            getString(R.string.main_signed_in_as_user_placeholder_snack),
                                            username
                                    ),
                                    Snackbar.LENGTH_SHORT).show();
                            updateUsername();
                            launchFetchService();
                            adView.setVisibility(View.VISIBLE);
                        }

                        @Override
                        public void onError(Throwable e) {
                            Toast.makeText(MainActivity.this,
                                    getString(R.string.main_sign_in_something_wrong_toast),
                                    Toast.LENGTH_SHORT).show();
                            adView.setVisibility(View.GONE);
                            signInToContinueSnack();
                        }
                    })
            );
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        disposables.clear();
    }

    @Override
    protected void onResume() {
        super.onResume();
        AuthenticationState state = AuthenticationManager.get().checkAuthState();
        Timber.d("AuthenticationState for onResume(): " + state);

        IntentFilter filter = new IntentFilter();
        filter.addAction(UnstashFetchService.ACTION_START_FETCH_SERVICE);
        filter.addAction(UnstashFetchService.ACTION_MARK_POST_AS_DONE);

        updateUsername();

        LocalBroadcastManager.getInstance(getApplicationContext()).registerReceiver(UnstashFetchReceiver, filter);

        switch (state) {
            case READY:
                launchFetchService();
                adView.setVisibility(View.VISIBLE);
                break;
            case NONE:
                adView.setVisibility(View.GONE);
                signInToContinueSnack();
                break;
            case NEED_REFRESH:
                refreshToken();
                break;
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(UnstashFetchReceiver);
    }

    private void refreshToken() {
        if (!AuthenticationManager.get().getRedditClient().isAuthenticated()) {
            Credentials credentials = ((App) getApplicationContext())
                    .getInstalledAppCredentials();
            disposables.add(RedditService.refreshToken(credentials)
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribeWith(new DisposableCompletableObserver() {
                        @Override
                        public void onComplete() {
                            /*Toast.makeText(MainActivity.this, "Token refreshed",
                                    Toast.LENGTH_SHORT).show();*/
                        }

                        @Override
                        public void onError(Throwable e) {
                            /*Toast.makeText(MainActivity.this, "Something went wrong",
                                    Toast.LENGTH_SHORT).show();*/
                        }
                    })
            );
        }
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
                throw new RuntimeException(String.format(getString(R.string.main_loader_not_implemented_exception), id));
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

        boolean status = prefs.getBoolean(showDoneKey, true);
        if (status) {
            menu.findItem(R.id.show_done).setTitle(getString(R.string.menu_show_done_action_title));
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
                if (Utils.isConnected(getApplicationContext())) {
                    launchFetchService();
                } else {
                    Snackbar.make(
                            mainCoordinatorLayout,
                            getString(R.string.disconnected_message),
                            Snackbar.LENGTH_LONG
                    ).show();
                }
                return true;

            case R.id.action_logout:
                logout();
                return true;

            case R.id.show_done:
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
                return true;

            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void logout() {
        if (Build.VERSION_CODES.KITKAT <= Build.VERSION.SDK_INT) {
            ((ActivityManager) getApplicationContext().getSystemService(ACTIVITY_SERVICE))
                    .clearApplicationUserData();
        }
    }

    public void launchFetchService() {
        Intent i = new Intent(this, UnstashFetchService.class);
        i.setAction(UnstashFetchService.ACTION_START_FETCH_SERVICE);
        startService(i);
    }


    public void signInToContinueSnack() {
        Snackbar.make(
                mainCoordinatorLayout,
                getString(R.string.main_sign_in_to_continue_snack),
                Snackbar.LENGTH_INDEFINITE)
                .setAction(getString(R.string.main_sign_in_snack_action), new View.OnClickListener() {
                    @Override
                    public void onClick(View view) {
                        Intent intent = new Intent(getApplicationContext(), LoginActivity.class);
                        startActivityForResult(intent, REQUEST_CODE);
                    }
                })
                .show();
    }
}