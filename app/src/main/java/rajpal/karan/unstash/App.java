package rajpal.karan.unstash;

import android.app.Application;
import android.util.Log;

import com.facebook.stetho.Stetho;
import com.google.android.gms.ads.MobileAds;
import com.google.firebase.analytics.FirebaseAnalytics;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.LoggingMode;
import net.dean.jraw.http.oauth.Credentials;

import java.util.UUID;

import timber.log.Timber;

public class App extends Application {

    private static final String ADMOB_APP_ID = "ca-app-pub-3940256099942544~3347511713";
    private FirebaseAnalytics firebaseAnalytics;

    private static final String CLIENT_ID = "NaP7nvfW4D721A";
    private static final String REDIRECT_URL = "http://www.reddit.com";

    private Credentials installedAppCredentials;
    private Credentials userlessAppCredentials;

    @Override
    public void onCreate() {
        super.onCreate();

        MobileAds.initialize(this, ADMOB_APP_ID);
        firebaseAnalytics = FirebaseAnalytics.getInstance(this);
        firebaseAnalytics.setAnalyticsCollectionEnabled(true);
        firebaseAnalytics.setMinimumSessionDuration(5000);
        firebaseAnalytics.setSessionTimeoutDuration(1000000);

        if (BuildConfig.DEBUG) {
            Timber.plant(new Timber.DebugTree());
            Stetho.initializeWithDefaults(this);
        } else {
            Timber.plant(new CrashReportingTree());
        }

        RedditClient reddit = new AndroidRedditClient(this);
        reddit.setLoggingMode(LoggingMode.ON_FAIL);
        AuthenticationManager.get().init(reddit, new RefreshTokenHandler(new AndroidTokenStore(this), reddit));
    }

    public Credentials getUserlessAppCredentials() {
        if (userlessAppCredentials == null) {
            userlessAppCredentials = Credentials.userlessApp(CLIENT_ID, UUID.randomUUID());
        }
        return userlessAppCredentials;
    }

    public Credentials getInstalledAppCredentials() {
        if (installedAppCredentials == null) {
            installedAppCredentials = Credentials.installedApp(CLIENT_ID, REDIRECT_URL);
        }
        return installedAppCredentials;
    }

    private static class CrashReportingTree extends Timber.Tree {
        @Override
        protected void log(int priority, String tag, String message, Throwable t) {
            if (priority == Log.VERBOSE || priority == Log.DEBUG) {
                return;
            }

//			FakeCrashLibrary.log(priority, tag, message);

            if (t != null) {
                if (priority == Log.ERROR) {
//					FakeCrashLibrary.logError(t);
                } else if (priority == Log.WARN) {
//					FakeCrashLibrary.logWarning(t);
                }
            }
        }
    }

}
