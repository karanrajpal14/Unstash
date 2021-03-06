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

import timber.log.Timber;

public class SavedPostProvider extends ContentProvider {

    // post_id = ?
    public static final String postWithID = SavedPostContract.SavedPostEntry.COLUMN_POST_ID + " = ? ";
    public static final String defaultOrder = "created_time DESC";
    // Mapping uris to functions
    static final int POSTS = 100;
    static final int POST_WITH_ID = 101;
    static final int RANDOM = 200;
    // Adding a uri matcher to map the uri calls to respective queries
    private static final UriMatcher URI_MATCHER = buildUriMatcher();
    private SavedPostDBHelper postDBHelper;

    static UriMatcher buildUriMatcher() {
        Timber.d("Building Uri Matcher");

        // Constructing an empty matcher
        UriMatcher builtUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);

        // Building different types of uris for the matcher to match

        // /posts directory of posts
        builtUriMatcher.addURI(SavedPostContract.CONTENT_AUTHORITY, SavedPostContract.PATH_POST, POSTS);
        // /posts/id individual post with id = 'id'
        builtUriMatcher.addURI(SavedPostContract.CONTENT_AUTHORITY, SavedPostContract.PATH_POST + "/*", POST_WITH_ID);
        // /random individual random post
        builtUriMatcher.addURI(SavedPostContract.CONTENT_AUTHORITY, SavedPostContract.PATH_RANDOM, RANDOM);

        Timber.d("Built Uri Matcher");
        return builtUriMatcher;
    }

    // Initializing the DbHelper while creating the provider
    @Override
    public boolean onCreate() {
        postDBHelper = new SavedPostDBHelper(getContext());
        Timber.d("DB Helper initialized");
        return true;
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
                        defaultOrder
                );
                break;
            case POST_WITH_ID:
                String id = uri.getPathSegments().get(1);
                Timber.d("Query: ID of post: " + id);
                // Selection is the post_id column = ?, and selectionArgs is the post_id from the uri
                String selectionTemp = "post_id=?";
                String[] selectionArgsTemp = new String[]{id};
                resultCursor = database.query(
                        SavedPostContract.SavedPostEntry.TABLE_NAME,
                        projection,
                        selectionTemp,
                        selectionArgsTemp,
                        null,
                        null,
                        defaultOrder
                );
                break;
            case RANDOM:
                Timber.d("Fetching random post");
                resultCursor = database.rawQuery(
                        "SELECT * FROM " +
                                SavedPostContract.SavedPostEntry.TABLE_NAME +
                                " WHERE " + SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED +
                                " = 1 ORDER BY RANDOM() LIMIT 1",
                        null
                );
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
        TodoCountWidgetProvider.refreshWidgetBroadcast(getContext());
        Timber.d("Completed insertion");
        return returnUri;
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase database = postDBHelper.getWritableDatabase();
        final int match = URI_MATCHER.match(uri);
        int numberOfDeletedRows;
        Timber.d("Deleting rows");
        String id = null;

        if (selection == null) {
            selection = "1";
            selectionArgs = null;
        } else {
            selection = postWithID;
            id = uri.getPathSegments().get(1);
            selectionArgs = new String[]{id};
        }

        switch (match) {
            case POSTS:
                numberOfDeletedRows = database.delete(
                        SavedPostContract.SavedPostEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            case POST_WITH_ID:
                Timber.d("Delete: ID of post: " + id);
                numberOfDeletedRows = database.delete(
                        SavedPostContract.SavedPostEntry.TABLE_NAME,
                        selection,
                        selectionArgs
                );
                break;
            default:
                throw new UnsupportedOperationException("Cannot perform delete. Unknown Uri: " + uri);
        }

        if (numberOfDeletedRows != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            TodoCountWidgetProvider.refreshWidgetBroadcast(getContext());
        }

        Timber.d("Completed deletion");
        return numberOfDeletedRows;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String selection, @Nullable String[] selectionArgs) {
        final SQLiteDatabase database = postDBHelper.getWritableDatabase();
        final int match = URI_MATCHER.match(uri);
        int noOfUpdatedRows;
        Timber.d("Updating rows");

        switch (match) {
            case POSTS:
                noOfUpdatedRows = database.update(SavedPostContract.SavedPostEntry.TABLE_NAME, contentValues, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Cannot perform update. Unknown Uri:" + uri);
        }

        if (noOfUpdatedRows != 0) {
            getContext().getContentResolver().notifyChange(uri, null);
            TodoCountWidgetProvider.refreshWidgetBroadcast(getContext());
        }

        Timber.d("No of updated rows = " + noOfUpdatedRows);
        return noOfUpdatedRows;
    }

}
