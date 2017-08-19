package rajpal.karan.unstash;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import timber.log.Timber;

public class SavedPostDBHelper extends SQLiteOpenHelper {

	private final static String DB_NAME = SavedPostContract.SavedPostEntry.TABLE_NAME.concat(".db");
	private final static int DB_VERSION = 10;

	public SavedPostDBHelper(Context context) {
		super(context, DB_NAME, null, DB_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase sqLiteDatabase) {
		Timber.d("Creating tables");

		final String SQL_CREATE_SAVED_POSTS_TABLE =
				"CREATE TABLE " + SavedPostContract.SavedPostEntry.TABLE_NAME + " ( " +
						SavedPostContract.SavedPostEntry.COLUMN_POST_ID + " TEXT PRIMARY KEY ON CONFLICT IGNORE, " +
						SavedPostContract.SavedPostEntry.COLUMN_TITLE + " TEXT NOT NULL, " +
						SavedPostContract.SavedPostEntry.COLUMN_AUTHOR + " TEXT NOT NULL, " +
						SavedPostContract.SavedPostEntry.COLUMN_THUMBNAIL + " TEXT, " +
						SavedPostContract.SavedPostEntry.COLUMN_CREATED_TIME + " INTEGER NOT NULL, " +
						SavedPostContract.SavedPostEntry.COLUMN_SUBREDDIT_NAME + " TEXT NOT NULL, " +
						SavedPostContract.SavedPostEntry.COLUMN_DOMAIN + " TEXT NOT NULL, " +
						SavedPostContract.SavedPostEntry.COLUMN_POST_HINT + " TEXT NOT NULL, " +
						SavedPostContract.SavedPostEntry.COLUMN_PERMALINK + " TEXT NOT NULL, " +
						SavedPostContract.SavedPostEntry.COLUMN_URL + " TEXT NOT NULL UNIQUE ON CONFLICT IGNORE, " +
						SavedPostContract.SavedPostEntry.COLUMN_SCORE + " INTEGER NOT NULL, " +
						SavedPostContract.SavedPostEntry.COLUMN_IS_NSFW + " INTEGER NOT NULL CHECK ( " +
						SavedPostContract.SavedPostEntry.COLUMN_IS_NSFW + " IN (0,1) )," +
						SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED + " INTEGER NOT NULL CHECK ( " +
						SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED + " IN (0,1) )"
						+ " );";
		Timber.d("Creating table" + SQL_CREATE_SAVED_POSTS_TABLE);
		sqLiteDatabase.execSQL(SQL_CREATE_SAVED_POSTS_TABLE);

		Timber.d("Finished creating tables");
	}

	@Override
	public void onUpgrade(SQLiteDatabase sqLiteDatabase, int oldVersion, int newVersion) {
		Timber.d("Dropping tables");

		sqLiteDatabase.execSQL("DROP TABLE IF EXISTS " + SavedPostContract.SavedPostEntry.TABLE_NAME);
		onCreate(sqLiteDatabase);

		Timber.d("Finished dropping tables");
	}
}
