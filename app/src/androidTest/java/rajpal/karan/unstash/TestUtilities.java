package rajpal.karan.unstash;

import android.database.ContentObserver;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;

class TestUtilities {

	static TestContentObserver getTestContentObserver() {
		return TestContentObserver.getTestContentObserver();
	}

	/**
	 * Students: The test functions for insert and delete use TestContentObserver to test
	 * the ContentObserver callbacks using the PollingCheck class from the Android Compatibility
	 * Test Suite tests.
	 * NOTE: This only tests that the onChange function is called; it DOES NOT test that the
	 * correct Uri is returned.
	 */
	static class TestContentObserver extends ContentObserver {
		final HandlerThread mHT;
		boolean mContentChanged;

		private TestContentObserver(HandlerThread ht) {
			super(new Handler(ht.getLooper()));
			mHT = ht;
		}

		static TestContentObserver getTestContentObserver() {
			HandlerThread ht = new HandlerThread("ContentObserverThread");
			ht.start();
			return new TestContentObserver(ht);
		}

		/**
		 * Called when a content change occurs.
		 * <p>
		 * To ensure correct operation on older versions of the framework that did not provide a
		 * Uri argument, applications should also implement this method whenever they implement
		 * the { #onChange(boolean, Uri)} overload.
		 *
		 * @param selfChange True if this is a self-change notification.
		 */
		@Override
		public void onChange(boolean selfChange) {
			onChange(selfChange, null);
		}

		/**
		 * Called when a content change occurs. Includes the changed content Uri when available.
		 *
		 * @param selfChange True if this is a self-change notification.
		 * @param uri        The Uri of the changed content, or null if unknown.
		 */
		@Override
		public void onChange(boolean selfChange, Uri uri) {
			mContentChanged = true;
		}

		/**
		 * Note: The PollingCheck class is taken from the Android CTS (Compatibility Test Suite).
		 * It's useful to look at the Android CTS source for ideas on how to test your Android
		 * applications. The reason that PollingCheck works is that, by default, the JUnit testing
		 * framework is not running on the main Android application thread.
		 */
		void waitForNotificationOrFail() {

			new PollingCheck(5000) {
				@Override
				protected boolean check() {
					return mContentChanged;
				}
			}.run();
			mHT.quit();
		}
	}
}