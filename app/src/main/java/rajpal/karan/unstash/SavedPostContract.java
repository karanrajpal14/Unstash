package rajpal.karan.unstash;

import android.content.ContentResolver;
import android.content.ContentUris;
import android.net.Uri;
import android.provider.BaseColumns;

import timber.log.Timber;

public class SavedPostContract {

	static final String CONTENT_AUTHORITY = App.class.getPackage().getName();
	static final String PATH_POST = "posts";

	/* Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
	 the content provider.*/
	private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

	//Inner class that defines the contents of "posts" table
	public static final class SavedPostEntry implements BaseColumns {

		public static final Uri CONTENT_URI =
				BASE_CONTENT_URI.buildUpon().appendPath(PATH_POST).build();

		public static final String CONTENT_TYPE =
				ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
						CONTENT_AUTHORITY + "/" +
						PATH_POST;

		public static final String CONTENT_ITEM_TYPE =
				ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
						CONTENT_AUTHORITY + "/" +
						PATH_POST;

		public static final String TABLE_NAME = PATH_POST;

		//columns
		public static final String _ID = "_id";
		public static final String COLUMN_POST_ID = "post_id";
		public static final String COLUMN_TITLE = "title";
		public static final String COLUMN_AUTHOR = "author";
		public static final String COLUMN_CREATED_TIME = "created_time";
		public static final String COLUMN_SUBREDDIT_NAME = "subreddit_name";
		public static final String COLUMN_DOMAIN = "domain";
		public static final String COLUMN_POST_HINT = "post_hint";
		public static final String COLUMN_PERMALINK = "permalink";
		public static final String COLUMN_URL = "url";
		public static final String COLUMN_SCORE = "score";
		public static final String COLUMN_IS_NSFW = "is_nsfw";
		public static final String COLUMN_IS_SAVED = "is_saved";

		public static Uri buildPostUri(long id) {
			Timber.d("buildPostUri: " + id);
			return ContentUris.withAppendedId(CONTENT_URI, id);
		}

		public static String getPostIDFromUri(Uri postUriWithID) {
			String id = postUriWithID.getPathSegments().get(2);
			Timber.d("Post ID from URI: " + id);
			return id;
		}
	}
}
