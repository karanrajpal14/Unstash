package rajpal.karan.unstash;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;

import timber.log.Timber;

import static android.content.ContentValues.TAG;


public class SavedPostProvider extends ContentProvider {

	private SavedPostDBHelper postDBHelper;

	// Mapping uris to functions
	private static final int post = 100;
	private static final int post_with_id = 101;

	// post.id = ?
	private static final String postWithID =
			SavedPostContract.SavedPostEntry.TABLE_NAME + "." +
			SavedPostContract.SavedPostEntry.COLUMN_POST_ID + " = ? ";

	// Adding a uri matcher to map the uri calls to respective queries
	private static final UriMatcher URI_MATCHER = buildUriMatcher();

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

		switch (URI_MATCHER.match(uri)) {
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
		return null;
	}

	@Nullable
	@Override
	public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
		return null;
	}

	@Override
	public int delete(@NonNull Uri uri, @Nullable String s, @Nullable String[] strings) {
		return 0;
	}

	@Override
	public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
		return 0;
	}

}
