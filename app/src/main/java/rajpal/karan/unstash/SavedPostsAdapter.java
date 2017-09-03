package rajpal.karan.unstash;

import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.github.zagum.switchicon.SwitchIconView;

import net.dean.jraw.ApiException;
import net.dean.jraw.RedditClient;
import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.managers.AccountManager;
import net.dean.jraw.models.Submission;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_AUTHOR;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_CREATED_TIME;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_IS_SAVED;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_POST_ID;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_SUBREDDIT_NAME;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_THUMBNAIL;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_TITLE;

/**
 * {@link SavedPostsAdapter} exposes a list of the user's saved posts on reddit
 * from a {@link android.database.Cursor} to a {@link android.support.v7.widget.RecyclerView}.
 */
public class SavedPostsAdapter extends RecyclerView.Adapter<SavedPostsAdapter.SavedPostsViewHolder> {

    /*
     * An on-click handler that we've defined to make it easy for an Activity to interface with
     * our RecyclerView
     */
    final private ListItemClickListener mOnClickListener;
    String postID;
    String title;
    String author;
    long createdTime;
    String subName;
    String thumbnailURL;
    int isSaved;
    private Context context;
    private Cursor cursor;

    /**
     * Constructor for SavedPostsAdapter that accepts a context and the specification
     * for the ListItemClickListener.
     */
    public SavedPostsAdapter(Context context, ListItemClickListener listener) {
        this.context = context;
        mOnClickListener = listener;
    }

    /**
     * This gets called when each new ViewHolder is created. This happens when the RecyclerView
     * is laid out. Enough ViewHolders will be created to fill the screen and allow for scrolling.
     *
     * @param viewGroup The ViewGroup that these ViewHolders are contained within.
     * @param viewType  If your RecyclerView has more than one type of item (which ours doesn't) you
     *                  can use this viewType integer to provide a different layout. See
     *                  {@link android.support.v7.widget.RecyclerView.Adapter#getItemViewType(int)}
     *                  for more details.
     * @return A new SavedPostsViewHolder that holds the View for each list item
     */
    @Override
    public SavedPostsViewHolder onCreateViewHolder(ViewGroup viewGroup, int viewType) {

        context = viewGroup.getContext();
        int layoutIdForListItem = R.layout.saved_post_list_item;
        View view = LayoutInflater.from(context).inflate(layoutIdForListItem, viewGroup, false);
        view.setFocusable(true);

        return new SavedPostsViewHolder(view);
    }

    /**
     * OnBindViewHolder is called by the RecyclerView to display the data at the specified
     * position. In this method, we update the contents of the ViewHolder to display the correct
     * indices in the list for this particular position, using the "position" argument that is conveniently
     * passed into us.
     *
     * @param holder   The ViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(SavedPostsViewHolder holder, int position) {
        holder.bind(position);

    }

    void swapCursor(Cursor newCursor) {
        cursor = newCursor;
        notifyDataSetChanged();
    }

    /**
     * This method simply returns the number of items to display. It is used behind the scenes
     * to help layout our Views and for animations.
     *
     * @return The number of items available in our forecast
     */
    @Override
    public int getItemCount() {
        if (cursor == null) return 0;
        return cursor.getCount();
    }

    /**
     * The interface that receives onClick messages.
     */
    public interface ListItemClickListener {
        void onListItemClick(Intent intent);
    }

    /**
     * Cache of the children views for a list item.
     */
    class SavedPostsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        @BindView(R.id.post_title_text_view)
        TextView titleTV;
        @BindView(R.id.post_details_text_view)
        TextView postDetailsTV;
        @BindView(R.id.thumbnail_main_image_view)
        ImageView thumbnailImageView;
        @BindView(R.id.post_saved_state_toggle_main)
        SwitchIconView doneButton;

        /**
         * Constructor for our ViewHolder. Within this constructor, we get a reference to our
         * TextViews and set an onClickListener to listen for clicks. Those will be handled in the
         * onClick method below.
         *
         * @param itemView The View that you inflated in
         *                 {@link SavedPostsAdapter#onCreateViewHolder(ViewGroup, int)}
         */
        public SavedPostsViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
            itemView.setOnClickListener(this);
        }

        /**
         * A method we wrote for convenience. This method will take an integer as input and
         * use that integer to fetch the appropriate values of the post from the cursor.
         *
         * @param position Position of the post in the cursor
         */
        void bind(int position) {
            cursor.moveToPosition(position);

            postID = cursor.getString(INDEX_POST_ID);
            title = cursor.getString(INDEX_TITLE);
            author = cursor.getString(INDEX_AUTHOR);
            thumbnailURL = cursor.getString(INDEX_THUMBNAIL);
            createdTime = cursor.getLong(INDEX_CREATED_TIME);
            subName = cursor.getString(INDEX_SUBREDDIT_NAME);
            isSaved = cursor.getInt(INDEX_IS_SAVED);

            doneButton.setIconEnabled(isSaved != 0, true);

            if (doneButton.isIconEnabled()) {
                doneButton.setContentDescription(context.getString(R.string.main_done_button_content_description));
            } else {
                doneButton.setContentDescription(context.getString(R.string.main_undone_button_content_description));
            }

            titleTV.setText(title);
            titleTV.setContentDescription(title);

            String postDetailsText = context.getResources().getString(
                    R.string.post_details_textview,
                    author,
                    Utils.getRelativeTime(createdTime),
                    subName
            );
            postDetailsTV.setText(postDetailsText);
            postDetailsTV.setContentDescription(postDetailsText);

            RequestOptions requestOptions = new RequestOptions();
            requestOptions.placeholder(R.drawable.ic_action_cloud_download);
            requestOptions.error(R.drawable.ic_error);
            requestOptions.fallback(R.drawable.ic_error);
            requestOptions.centerInside();
            requestOptions.circleCrop();

            Glide.with(context)
                    .setDefaultRequestOptions(requestOptions)
                    .load(thumbnailURL)
                    .thumbnail(1.0f)
                    .into(thumbnailImageView);

            doneButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    toggleSaveStatus(getPostID(getAdapterPosition()), cursor.getInt(INDEX_IS_SAVED));
                }
            });
        }

        public String getPostID(int position) {
            cursor.moveToPosition(position);
            return cursor.getString(INDEX_POST_ID);
        }

        public void toggleSaveStatus(final String id, int currentSaveStatus) {
            if (currentSaveStatus == 1) {
                currentSaveStatus = 0;
                Timber.d(id + " " + currentSaveStatus);
                ContentValues updateSavedStatusValue = new ContentValues();
                updateSavedStatusValue.put(SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED, currentSaveStatus);
                String selection = SavedPostProvider.postWithID;
                String[] selectionArgs = new String[]{id};
                context.getContentResolver().update(
                        SavedPostContract.SavedPostEntry.CONTENT_URI,
                        updateSavedStatusValue,
                        selection,
                        selectionArgs
                );
                notifyDataSetChanged();
                doneButton.setIconEnabled(true, true);
                if (Utils.isConnected(context)) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            RedditClient redditClient = AuthenticationManager.get().getRedditClient();
                            AccountManager manager = new AccountManager(redditClient);
                            Submission submission = redditClient.getSubmission(id);
                            try {
                                manager.unsave(submission);
                            } catch (ApiException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    }.execute();
                } else {
                    Toast.makeText(context, context.getString(R.string.disconnected_message), Toast.LENGTH_SHORT).show();
                }

            } else if (currentSaveStatus == 0) {
                currentSaveStatus = 1;
                Timber.d(id + " " + currentSaveStatus);
                ContentValues updateSavedStatusValue = new ContentValues();
                updateSavedStatusValue.put(SavedPostContract.SavedPostEntry.COLUMN_IS_SAVED, currentSaveStatus);
                String selection = SavedPostProvider.postWithID;
                String[] selectionArgs = new String[]{id};
                context.getContentResolver().update(
                        SavedPostContract.SavedPostEntry.CONTENT_URI,
                        updateSavedStatusValue,
                        selection,
                        selectionArgs
                );
                notifyDataSetChanged();
                doneButton.setIconEnabled(false, true);
                if (Utils.isConnected(context)) {
                    new AsyncTask<Void, Void, Void>() {
                        @Override
                        protected Void doInBackground(Void... voids) {
                            RedditClient redditClient = AuthenticationManager.get().getRedditClient();
                            AccountManager manager = new AccountManager(redditClient);
                            Submission submission = redditClient.getSubmission(id);
                            try {
                                manager.save(submission);
                            } catch (ApiException e) {
                                e.printStackTrace();
                            }
                            return null;
                        }
                    }.execute();
                } else {
                    Toast.makeText(context, context.getString(R.string.disconnected_message), Toast.LENGTH_SHORT).show();
                }
            }
        }

        /**
         * Called whenever a user clicks on an item in the list.
         *
         * @param v The View that was clicked
         */
        @Override
        public void onClick(View v) {
            Intent postIDIntent = new Intent(v.getContext(), PostDetailActivity.class);
            postIDIntent.putExtra("intentClickedPostID", getPostID(getAdapterPosition()));
            mOnClickListener.onListItemClick(postIDIntent);
        }
    }
}
