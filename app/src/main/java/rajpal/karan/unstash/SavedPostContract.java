package rajpal.karan.unstash;

import android.content.ContentResolver;
import android.net.Uri;
import android.provider.BaseColumns;

public class SavedPostContract {

    static final String CONTENT_AUTHORITY = SavedPostContract.class.getPackage().getName();
    static final String PATH_POST = "posts";
    static final String PATH_RANDOM = "random";

    /* Use CONTENT_AUTHORITY to create the base of all URI's which apps will use to contact
     the content provider.*/
    private static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);

    //Inner class that defines the contents of "posts" table
    public static final class SavedPostEntry implements BaseColumns {

        public static final Uri CONTENT_URI =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_POST).build();

        public static final Uri CONTENT_URI_RANDOM =
                BASE_CONTENT_URI.buildUpon().appendPath(PATH_RANDOM).build();

        public static final String CONTENT_TYPE =
                ContentResolver.CURSOR_DIR_BASE_TYPE + "/" +
                        CONTENT_AUTHORITY + "/" +
                        PATH_POST;

        public static final String CONTENT_ITEM_TYPE =
                ContentResolver.CURSOR_ITEM_BASE_TYPE + "/" +
                        CONTENT_AUTHORITY + "/" +
                        PATH_POST;

        public static final String TABLE_NAME = PATH_POST;

        // columns
        public static final String COLUMN_POST_ID = "post_id";
        public static final String COLUMN_TITLE = "title";
        public static final String COLUMN_AUTHOR = "author";
        public static final String COLUMN_THUMBNAIL = "thumbnail_url";
        public static final String COLUMN_CREATED_TIME = "created_time";
        public static final String COLUMN_SUBREDDIT_NAME = "subreddit_name";
        public static final String COLUMN_DOMAIN = "domain";
        public static final String COLUMN_POST_HINT = "post_hint";
        public static final String COLUMN_PERMALINK = "permalink";
        public static final String COLUMN_URL = "url";
        public static final String COLUMN_SCORE = "score";
        public static final String COLUMN_IS_NSFW = "is_nsfw";
        public static final String COLUMN_IS_SAVED = "is_saved";

        // index positions of columns
        public static final int INDEX_POST_ID = 0;
        public static final int INDEX_TITLE = 1;
        public static final int INDEX_AUTHOR = 2;
        public static final int INDEX_THUMBNAIL = 3;
        public static final int INDEX_CREATED_TIME = 4;
        public static final int INDEX_SUBREDDIT_NAME = 5;
        public static final int INDEX_DOMAIN = 6;
        public static final int INDEX_POST_HINT = 7;
        public static final int INDEX_PERMALINK = 8;
        public static final int INDEX_URL = 9;
        public static final int INDEX_SCORE = 10;
        public static final int INDEX_IS_NSFW = 11;
        public static final int INDEX_IS_SAVED = 12;
    }
}
