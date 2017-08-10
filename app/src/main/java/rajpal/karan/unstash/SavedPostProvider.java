package rajpal.karan.unstash;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;

import timber.log.Timber;

import static android.content.ContentValues.TAG;

public class SavedPostProvider extends ContentProvider {

	// Mapping uris to functions
	private static final int post = 100;
	private static final int post_with_id = 101;
	// post.id = ?
	private static final String postWithID =
			SavedPostContract.SavedPostEntry.TABLE_NAME + "." +
					SavedPostContract.SavedPostEntry.COLUMN_POST_ID + " = ? ";
	// Adding a uri matcher to map the uri calls to respective queries
	private static final UriMatcher URI_MATCHER = buildUriMatcher();
	private SavedPostDBHelper postDBHelper;

	private static UriMatcher buildUriMatcher() {
		Timber.d("Building Uri Matcher");

		UriMatcher builtUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

		// Building different types of uris for the matcher to match
		// /post
		// /post/id
		builtUriMatcher.addURI(SavedPostContract.CONTENT_AUTHORITY, SavedPostContract.PATH_POST, post);
		builtUriMatcher.addURI(SavedPostContract.CONTENT_AUTHORITY, SavedPostContract.PATH_POST + "/*", post_with_id);

		Timber.d("Built Uri Matcher");
		return builtUriMatcher;
	}

	@Override
	public boolean onCreate() {
		postDBHelper = new SavedPostDBHelper(getContext());
		Timber.d("DB Helper initialized");
		return true;
	}

	private Cursor getAllSavedPosts() {
		Timber.d(TAG, "getAllSavedPosts: Fetching posts");

		return postDBHelper.getReadableDatabase().query(
				SavedPostContract.SavedPostEntry.TABLE_NAME,
				null,
				null,
				null,
				null,
				null,
				null
		);
	}

	private Cursor getPostByID(Uri uri, String[] columns, String sortOrder) {
		// post.id = ?
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
		final int match = URI_MATCHER.match(uri);

		switch (match) {
			case post:
				resultCursor = getAllSavedPosts();
				break;
			case post_with_id:
				resultCursor = getPostByID(uri, projection, sortOrder);
				break;
			default:
				throw new UnsupportedOperationException("Invalid query");
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
			case post:
				return SavedPostContract.SavedPostEntry.CONTENT_TYPE;
			case post_with_id:
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
			case post:
				long _id = database.insert(SavedPostContract.SavedPostEntry.TABLE_NAME, null, contentValues);
				if (_id > 0)
					returnUri = SavedPostContract.SavedPostEntry.buildPostUri(_id);
				else
					throw new UnsupportedOperationException("Failed to insert row into" + uri);
				break;
			default:
				throw new UnsupportedOperationException("Failed to insert row into" + uri);
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
			case post:
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
			case post:
				numberOfDeletedRows = database.delete(SavedPostContract.SavedPostEntry.TABLE_NAME, selection, selectionArgs);
				break;
			default:
				throw new UnsupportedOperationException("Cannot perform delete. Unknown Uri:" + uri);
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
			case post:
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
