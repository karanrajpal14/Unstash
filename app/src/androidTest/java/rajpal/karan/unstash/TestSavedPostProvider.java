package rajpal.karan.unstash;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.pm.PackageManager;
import android.content.pm.ProviderInfo;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertTrue;
import static junit.framework.Assert.fail;

@RunWith(AndroidJUnit4.class)
public class TestSavedPostProvider {

    private static final Uri TEST_POSTS = SavedPostContract.SavedPostEntry.CONTENT_URI;
    // Content URI for a single task with id = 1
    private static final Uri TEST_POST_WITH_ID = TEST_POSTS.buildUpon().appendPath("1").build();
    /* Context used to access various parts of the system */
    private final Context mContext = InstrumentationRegistry.getTargetContext();

    //================================================================================
    // Test UriMatcher
    //================================================================================
    public final String postID = "Test ID";

    /**
     * Because we annotate this method with the @Before annotation, this method will be called
     * before every single method with an @Test annotation. We want to start each test clean, so we
     * delete all entries in the tasks directory to do so.
     */
    @Before
    public void setUp() {
        /* Use TaskDbHelper to get access to a writable database */
        SavedPostDBHelper dbHelper = new SavedPostDBHelper(mContext);
        SQLiteDatabase database = dbHelper.getWritableDatabase();
        database.delete(SavedPostContract.SavedPostEntry.TABLE_NAME, null, null);
    }

    /**
     * This test checks to make sure that the content provider is registered correctly in the
     * AndroidManifest file. If it fails, you should check the AndroidManifest to see if you've
     * added a <provider/> tag and that you've properly specified the android:authorities attribute.
     */
    @Test
    public void testProviderRegistry() {

        /*
         * A ComponentName is an identifier for a specific application component, such as an
         * Activity, ContentProvider, BroadcastReceiver, or a Service.
         *
         * Two pieces of information are required to identify a component: the package (a String)
         * it exists in, and the class (a String) name inside of that package.
         *
         * We will use the ComponentName for our ContentProvider class to ask the system
         * information about the ContentProvider, specifically, the authority under which it is
         * registered.
         */
        String packageName = mContext.getPackageName();
        String taskProviderClassName = SavedPostProvider.class.getName();
        ComponentName componentName = new ComponentName(packageName, taskProviderClassName);

        try {

            /*
             * Get a reference to the package manager. The package manager allows us to access
             * information about packages installed on a particular device. In this case, we're
             * going to use it to get some information about our ContentProvider under test.
             */
            PackageManager pm = mContext.getPackageManager();

            /* The ProviderInfo will contain the authority, which is what we want to test */
            ProviderInfo providerInfo = pm.getProviderInfo(componentName, 0);
            String actualAuthority = providerInfo.authority;
            String expectedAuthority = packageName;

            /* Make sure that the registered authority matches the authority from the Contract */
            String incorrectAuthority =
                    "Error: TaskContentProvider registered with authority: " + actualAuthority +
                            " instead of expected authority: " + expectedAuthority;
            assertEquals(incorrectAuthority,
                    actualAuthority,
                    expectedAuthority);

        } catch (PackageManager.NameNotFoundException e) {
            String providerNotRegisteredAtAll =
                    "Error: TaskContentProvider not registered at " + mContext.getPackageName();
            /*
             * This exception is thrown if the ContentProvider hasn't been registered with the
             * manifest at all. If this is the case, you need to double check your
             * AndroidManifest file
             */
            fail(providerNotRegisteredAtAll);
        }
    }

    //================================================================================
    // Test Insert
    //================================================================================

    /**
     * This function tests that the UriMatcher returns the correct integer value for
     * each of the Uri types that the ContentProvider can handle. Uncomment this when you are
     * ready to test your UriMatcher.
     */
    @Test
    public void testUriMatcher() {

        /* Create a URI matcher that the TaskContentProvider uses */
        UriMatcher testMatcher = SavedPostProvider.buildUriMatcher();

        /* Test that the code returned from our matcher matches the expected POSTS int */
        String tasksUriDoesNotMatch = "Error: The POSTS URI was matched incorrectly.";
        int actualTasksMatchCode = testMatcher.match(TEST_POSTS);
        int expectedTasksMatchCode = SavedPostProvider.POSTS;
        assertEquals(tasksUriDoesNotMatch,
                actualTasksMatchCode,
                expectedTasksMatchCode);

        /* Test that the code returned from our matcher matches the expected POST_WITH_ID */
        String taskWithIdDoesNotMatch =
                "Error: The POST_WITH_ID URI was matched incorrectly.";
        int actualTaskWithIdCode = testMatcher.match(TEST_POST_WITH_ID);
        int expectedTaskWithIdCode = SavedPostProvider.POST_WITH_ID;
        assertEquals(taskWithIdDoesNotMatch,
                actualTaskWithIdCode,
                expectedTaskWithIdCode);
    }

    public ContentValues putTestContentValues(ContentValues testTaskValues) {
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_POST_ID, postID);
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_TITLE, "Test Title");
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_AUTHOR, "Test Author");
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_CREATED_TIME, 123456);
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_SUBREDDIT_NAME, "Test Sub Name");
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_DOMAIN, "Test Domain");
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_POST_HINT, "Test Post Hint");
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_PERMALINK, "Test Permalink");
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_URL, "Test URL");
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_SCORE, 123456);
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_IS_NSFW, 0);
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED, 1);
        return testTaskValues;
    }

    /**
     * Tests inserting a single row of data via a ContentResolver
     */
    @Test
    public void testInsert() {

        /* Create values to insert */
        ContentValues testTaskValues = new ContentValues();
        testTaskValues = putTestContentValues(testTaskValues);

        /* TestContentObserver allows us to test if notifyChange was called appropriately */
        TestUtilities.TestContentObserver taskObserver = TestUtilities.getTestContentObserver();

        ContentResolver contentResolver = mContext.getContentResolver();

        /* Register a content observer to be notified of changes to data at a given URI (posts) */
        contentResolver.registerContentObserver(
                /* URI that we would like to observe changes to */
                SavedPostContract.SavedPostEntry.CONTENT_URI,
                /* Whether or not to notify us if descendants of this URI change */
                true,
                /* The observer to register (that will receive notifyChange callbacks) */
                taskObserver);


        Uri uri = contentResolver.insert(SavedPostContract.SavedPostEntry.CONTENT_URI, testTaskValues);


        Uri expectedUri = ContentUris.withAppendedId(SavedPostContract.SavedPostEntry.CONTENT_URI, 1);

        String insertProviderFailed = "Unable to insert item through Provider";
        assertEquals(insertProviderFailed, uri, expectedUri);

        /*
         * If this fails, it's likely you didn't call notifyChange in your insert method from
         * your ContentProvider.
         */
        taskObserver.waitForNotificationOrFail();

        /*
         * waitForNotificationOrFail is synchronous, so after that call, we are done observing
         * changes to content and should therefore unregister this observer.
         */
        contentResolver.unregisterContentObserver(taskObserver);
    }

    //================================================================================
    // Test Query (for posts directory)
    //================================================================================


    /**
     * Inserts data, then tests if a query for the posts directory returns that data as a Cursor
     */
    @Test
    public void testQueryAll() {

        /* Get access to a writable database */
        SavedPostDBHelper dbHelper = new SavedPostDBHelper(mContext);
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        /* Create values to insert */
        ContentValues testTaskValues = new ContentValues();
        testTaskValues = putTestContentValues(testTaskValues);

        /* Insert ContentValues into database and get a row ID back */
        long taskRowId = database.insert(
                /* Table to insert values into */
                SavedPostContract.SavedPostEntry.TABLE_NAME,
                null,
                /* Values to insert into table */
                testTaskValues);

        String insertFailed = "Unable to insert directly into the database";
        assertTrue(insertFailed, taskRowId != -1);

        /* We are done with the database, close it now. */
        database.close();

        /* Perform the ContentProvider query */
        Cursor taskCursor = mContext.getContentResolver().query(
                SavedPostContract.SavedPostEntry.CONTENT_URI,
                /* Columns; leaving this null returns every column in the table */
                null,
                /* Optional specification for columns in the "where" clause above */
                null,
                /* Values for "where" clause */
                null,
                /* Sort order to return in Cursor */
                null);


        String queryFailed = "Query failed to return a valid Cursor";
        assertTrue(queryFailed, taskCursor != null);

        /* We are done with the cursor, close it now. */
        taskCursor.close();
    }

    //================================================================================
    // Test Query (for single post)
    //================================================================================


    /**
     * Inserts data, then tests if a query for the posts directory for a specified post_id returns that row or not
     */
    @Test
    public void testQuerySingle() {

        /* Get access to a writable database */
        SavedPostDBHelper dbHelper = new SavedPostDBHelper(mContext);
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        /* Create values to insert */
        ContentValues testTaskValues = new ContentValues();
        testTaskValues = putTestContentValues(testTaskValues);

        /* Insert ContentValues into database and get a row ID back */
        long taskRowId = database.insert(
                /* Table to insert values into */
                SavedPostContract.SavedPostEntry.TABLE_NAME,
                null,
                /* Values to insert into table */
                testTaskValues);

        String insertFailed = "Unable to insert directly into the database";
        assertTrue(insertFailed, taskRowId != -1);

        /* We are done with the database, close it now. */
        database.close();

        String selection = "post_id=?";
        String[] selectionArgs = new String[]{postID};
        int initialPosition = 0;

	    /* Perform the ContentProvider query */
        Cursor taskCursor = mContext.getContentResolver().query(
                SavedPostContract.SavedPostEntry.CONTENT_URI,
                /* Columns; leaving this null returns every column in the table */
                null,
                /* Optional specification for columns in the "where" clause above */
                selection,
                /* Values for "where" clause */
                selectionArgs,
                /* Sort order to return in Cursor */
                null);


        String queryFailed = "Query failed to return a valid Cursor";
        assert taskCursor != null;
        taskCursor.moveToPosition(initialPosition);
        assertTrue(queryFailed, taskCursor.getString(initialPosition).equals(postID));

        /* We are done with the cursor, close it now. */
        taskCursor.close();
    }

    //================================================================================
    // Test Delete (for a single item)
    //================================================================================


    /**
     * Tests deleting a single row of data via a ContentResolver
     */
    @Test
    public void testDelete() {
        /* Access writable database */
        SavedPostDBHelper helper = new SavedPostDBHelper(InstrumentationRegistry.getTargetContext());
        SQLiteDatabase database = helper.getWritableDatabase();

        /* Create a new row of post data */
        ContentValues testTaskValues = new ContentValues();
        testTaskValues = putTestContentValues(testTaskValues);

	    /* Insert ContentValues into database and get a row ID back */
        long taskRowId = database.insert(
                /* Table to insert values into */
                SavedPostContract.SavedPostEntry.TABLE_NAME,
                null,
                /* Values to insert into table */
                testTaskValues);

        /* Always close the database when you're through with it */
        database.close();

        String insertFailed = "Unable to insert into the database";
        assertTrue(insertFailed, taskRowId != -1);


        /* TestContentObserver allows us to test if notifyChange was called appropriately */
        TestUtilities.TestContentObserver taskObserver = TestUtilities.getTestContentObserver();

        ContentResolver contentResolver = mContext.getContentResolver();

        /* Register a content observer to be notified of changes to data at a given URI (posts) */
        contentResolver.registerContentObserver(
                /* URI that we would like to observe changes to */
                SavedPostContract.SavedPostEntry.CONTENT_URI,
                /* Whether or not to notify us if descendants of this URI change */
                true,
                /* The observer to register (that will receive notifyChange callbacks) */
                taskObserver);



        /* The delete method deletes the previously inserted row with post_id = "Test ID" */
        Uri uriToDelete = SavedPostContract.SavedPostEntry.CONTENT_URI.buildUpon().appendPath(postID).build();
        int tasksDeleted = contentResolver.delete(uriToDelete, null, null);

        String deleteFailed = "Unable to delete item in the database";
        assertTrue(deleteFailed, tasksDeleted != 0);

        /*
         * If this fails, it's likely you didn't call notifyChange in your delete method from
         * your ContentProvider.
         */
        taskObserver.waitForNotificationOrFail();

        /*
         * waitForNotificationOrFail is synchronous, so after that call, we are done observing
         * changes to content and should therefore unregister this observer.
         */
        contentResolver.unregisterContentObserver(taskObserver);
    }

    @Test
    public void testUpdateSingleColumn() {

		/* Get access to a writable database */
        SavedPostDBHelper dbHelper = new SavedPostDBHelper(mContext);
        SQLiteDatabase database = dbHelper.getWritableDatabase();

        /* Create values to insert */
        ContentValues testTaskValues = new ContentValues();
        testTaskValues = putTestContentValues(testTaskValues);

        /* Insert ContentValues into database and get a row ID back */
        long taskRowId = database.insert(
                /* Table to insert values into */
                SavedPostContract.SavedPostEntry.TABLE_NAME,
                null,
                /* Values to insert into table */
                testTaskValues);

        String insertFailed = "Unable to insert directly into the database";
        assertTrue(insertFailed, taskRowId != -1);

        /* We are done with the database, close it now. */
        database.close();

        String selection = "post_id=?";
        String[] selectionArgs = new String[]{postID};
        String[] projection = new String[]{SavedPostContract.SavedPostEntry.COLUMN_POST_ID, SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED};
        int initialPosition = 0;
        int postIDPosition = initialPosition;
        int isSavedPosition = 1;
        int defaultIsSavedValue = isSavedPosition;

	    /* Perform the ContentProvider query */
        Cursor taskCursor = mContext.getContentResolver().query(
                SavedPostContract.SavedPostEntry.CONTENT_URI,
                /* Columns; leaving this null returns every column in the table */
                projection,
                /* Optional specification for columns in the "where" clause above */
                selection,
                /* Values for "where" clause */
                selectionArgs,
                /* Sort order to return in Cursor */
                null);

        assert taskCursor != null;
        taskCursor.moveToPosition(initialPosition);
        String queryFailed = "Query failed to return a valid Cursor";
        assertTrue(queryFailed, taskCursor.getString(postIDPosition).equals(postID));
        int isSaved = taskCursor.getInt(isSavedPosition);
        assertEquals("Post is not saved", defaultIsSavedValue, isSaved);

        /* We are done with the cursor, close it now. */
        taskCursor.close();

        // Unsaving post and checking if update works
        testTaskValues.clear();
        testTaskValues.put(SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED, "0");
        int noUpdated = mContext.getContentResolver().update(
                SavedPostContract.SavedPostEntry.CONTENT_URI,
                testTaskValues,
                selection,
                selectionArgs
        );

        assertEquals("No rows updated", 1, noUpdated);

    }

}
