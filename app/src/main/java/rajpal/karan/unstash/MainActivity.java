package rajpal.karan.unstash;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import timber.log.Timber;

public class MainActivity extends AppCompatActivity {

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		if (BuildConfig.DEBUG) {
			Timber.plant(new Timber.DebugTree());
		} else {
			Timber.plant(new CrashReportingTree());

		}

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
