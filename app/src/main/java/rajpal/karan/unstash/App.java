package rajpal.karan.unstash;

import android.app.Application;
import android.util.Log;

import com.facebook.stetho.Stetho;

import net.dean.jraw.RedditClient;
import net.dean.jraw.android.AndroidRedditClient;
import net.dean.jraw.android.AndroidTokenStore;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.auth.RefreshTokenHandler;
import net.dean.jraw.http.LoggingMode;

import timber.log.Timber;

public class App extends Application {

	@Override
	public void onCreate() {
		super.onCreate();

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
