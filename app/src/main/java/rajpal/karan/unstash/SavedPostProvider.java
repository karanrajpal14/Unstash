package rajpal.karan.unstash;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;

import timber.log.Timber;

import static android.content.ContentValues.TAG;

public class SavedPostProvider extends ContentProvider {

	private SavedPostDBHelper postDBHelper;

	// Mapping uris to functions
	static final int POSTS = 100;
	static final int POST_WITH_ID = 101;

	// Adding a uri matcher to map the uri calls to respective queries
	private static final UriMatcher URI_MATCHER = buildUriMatcher();

	static UriMatcher buildUriMatcher() {
		Timber.d("Building Uri Matcher");

		// Constructing an empty matcher
		UriMatcher builtUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		// Building different types of uris for the matcher to match

		// /POSTS directory of posts
		builtUriMatcher.addURI(SavedPostContract.CONTENT_AUTHORITY, SavedPostContract.PATH_POST, POSTS);
		// /POSTS/id individual post with id = 'id'
		builtUriMatcher.addURI(SavedPostContract.CONTENT_AUTHORITY, SavedPostContract.PATH_POST + "/*", POST_WITH_ID);

		Timber.d("Built Uri Matcher");
		return builtUriMatcher;
	}

	// POSTS.id = ?
	private static final String postWithID =
			SavedPostContract.SavedPostEntry.TABLE_NAME + "." +
					SavedPostContract.SavedPostEntry.COLUMN_POST_ID + " = ? ";

	// Initializing the DbHelper while creating the provider
	@Override
	public boolean onCreate() {
		postDBHelper = new SavedPostDBHelper(getContext());
		Timber.d("DB Helper initialized");
		return true;
	}

	private Cursor getPostByID(Uri uri, String[] columns, String sortOrder) {
		// POSTS.id = ?
		String selection = postWithID;
		String[] selectionArgs = new String[]{SavedPostContract.SavedPostEntry.getPostIDFromUri(uri)};
		Timber.d(TAG, "getPostByID: Selection: " + selection + "\n Selection args: " + Arrays.toString(selectionArgs));

		return postDBHelper.getReadableDatabase().query(
				SavedPostContract.SavedPostEntry.TABLE_NAME,
				columns,
				selection,
				selectionArgs,
				null,
				null,
				sortOrder
		);
	}

	@Nullable
	@Override
	public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection, @Nullable String[] selectionArgs, @Nullable String sortOrder) {
		Cursor resultCursor;
		Timber.d("Started querying");
		final SQLiteDatabase database = postDBHelper.getReadableDatabase();

		final int match = URI_MATCHER.match(uri);

		switch (match) {
			case POSTS:
				resultCursor = database.query(
						SavedPostContract.SavedPostEntry.TABLE_NAME,
						projection,
						selection,
						selectionArgs,
						null,
						null,
						sortOrder
				);
				break;
			case POST_WITH_ID:
				resultCursor = getPostByID(uri, projection, sortOrder);
				break;
			default:
				throw new UnsupportedOperationException("Unknown query uri " + uri);
		}
		// Watching content uri for changes
		resultCursor.setNotificationUri(getContext().getContentResolver(), uri);
		Timber.d("Querying complete");
		return resultCursor;
	}

	@Nullable
	@Override
	public String getType(@NonNull Uri uri) {
		Timber.d("Fetching uri type");
		final int match = URI_MATCHER.match(uri);

		switch (match) {
			case POSTS:
				return SavedPostContract.SavedPostEntry.CONTENT_TYPE;
			case POST_WITH_ID:
				return SavedPostContract.SavedPostEntry.CONTENT_ITEM_TYPE;
			default:
				throw new UnsupportedOperationException("Invalid query uri" + uri);
		}
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
		Timber.d("Inserting values");

		final SQLiteDatabase database = postDBHelper.getWritableDatabase();
		final int match = URI_MATCHER.match(uri);
		Uri returnUri;

		switch (match) {
			case POSTS:
				long id = database.insert(SavedPostContract.SavedPostEntry.TABLE_NAME, null, contentValues);
				if (id > 0)
					// Using built-in helper method to construct uri instead of custom method
					returnUri = ContentUris.withAppendedId(SavedPostContract.SavedPostEntry.CONTENT_URI, id);
				else
					throw new SQLiteException("Failed to insert row into " + uri);
				break;
			default:
				throw new UnsupportedOperationException("Unknown uri " + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		Timber.d("Completed insertion");
		return returnUri;
	}

	@Override
	public int bulkInsert(@NonNull Uri uri, @NonNull ContentValues[] values) {
		Timber.d("Bulk Inserting values");

		final SQLiteDatabase database = postDBHelper.getWritableDatabase();
		final int match = URI_MATCHER.match(uri);
		int noOfInsertedRows = 0;

		switch (match) {
			case POSTS:
				database.beginTransaction();
				try {
					for (ContentValues value : values) {
						long _id = database.insert(SavedPostContract.SavedPostEntry.TABLE_NAME, null, value);
						if (_id != -1)
							noOfInsertedRows++;
					}
					database.setTransactionSuccessful();
				} finally {
					database.endTransaction();
				}
				break;
			default:
				throw new UnsupportedOperationException("Could not bull insert. Invalid uri" + uri);
		}

		getContext().getContentResolver().notifyChange(uri, null);
		Timber.d("Completed bulk insertion");
		return noOfInsertedRows;
	}

	@Override
	public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
		final SQLiteDatabase database = postDBHelper.getWritableDatabase();
		final int match = URI_MATCHER.match(uri);
		int numberOfDeletedRows = 0;
		Timber.d("Deleting rows");

		if (selection == null)
			selection = "1";

		switch (match) {
			case POST_WITH_ID:
				numberOfDeletedRows = database.delete(SavedPostContract.SavedPostEntry.TABLE_NAME, selection, selectionArgs);
				break;
			default:
				throw new UnsupportedOperationException("Cannot perform delete. Unknown Uri: " + uri);
		}

		if (numberOfDeletedRows != 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}

		Timber.d("Completed deletion");
		return numberOfDeletedRows;
	}

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
		final SQLiteDatabase database = postDBHelper.getWritableDatabase();
		final int match = URI_MATCHER.match(uri);
		int noOfUpdatedRows = 0;
		Timber.d("Updating rows");

		switch (match) {
			case POSTS:
				noOfUpdatedRows = database.update(SavedPostContract.SavedPostEntry.TABLE_NAME, contentValues, selection, selectionArgs);
				break;
			default:
				throw new UnsupportedOperationException("Cannot perform delete. Unknown Uri:" + uri);
		}

		if (noOfUpdatedRows != 0) {
			getContext().getContentResolver().notifyChange(uri, null);
		}

		Timber.d("Updated rows");
		return 0;
	}

}
