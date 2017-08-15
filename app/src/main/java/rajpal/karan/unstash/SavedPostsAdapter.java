package rajpal.karan.unstash;

import android.content.Context;
import android.database.Cursor;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_AUTHOR;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_CREATED_TIME;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_DOMAIN;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_IS_NSFW;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_IS_SAVED;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_PERMALINK;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_POST_HINT;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_POST_ID;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_SCORE;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_SUBREDDIT_NAME;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_TITLE;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_URL;

/**
 * {@link SavedPostsAdapter} exposes a list of the user's saved posts on reddit
 * from a {@link android.database.Cursor} to a {@link android.support.v7.widget.RecyclerView}.
 */
public class SavedPostsAdapter extends RecyclerView.Adapter<SavedPostsAdapter.SavedPostsViewHolder> {

	private static final String TAG = SavedPostsAdapter.class.getSimpleName();

	private Context context;
	private Cursor cursor;

	/*
     * An on-click handler that we've defined to make it easy for an Activity to interface with
     * our RecyclerView
     */
	final private ListItemClickListener mOnClickListener;

	/**
	 * The interface that receives onClick messages.
	 */
	public interface ListItemClickListener {
		void onListItemClick(int clickedItemIndex);
	}

	/**
	 * Constructor for SavedPostsAdapter that accepts a context and the specification
	 * for the ListItemClickListener.
	 */
	public SavedPostsAdapter(Context context, ListItemClickListener listener) {
		this.context = context;
		mOnClickListener = listener;
	}

	/**
	 *
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

		cursor.moveToPosition(position);
		String postID = cursor.getString(INDEX_POST_ID);
		String title = cursor.getString(INDEX_TITLE);
		String author = cursor.getString(INDEX_AUTHOR);
		int createdTime = cursor.getInt(INDEX_CREATED_TIME);
		String subName = cursor.getString(INDEX_SUBREDDIT_NAME);
		String domain = cursor.getString(INDEX_DOMAIN);
		String postHint = cursor.getString(INDEX_POST_HINT);
		String permalink = cursor.getString(INDEX_PERMALINK);
		String url = cursor.getString(INDEX_URL);
		int score = cursor.getInt(INDEX_SCORE);
		int isNSFW  = cursor.getInt(INDEX_IS_NSFW);
		int isSaved = cursor.getInt(INDEX_IS_SAVED);

		holder.titleTV.setText(title);
		holder.authorTV.setText(author);
		holder.createdTimeTV.setText(String.valueOf(createdTime));
		holder.subNameTV.setText(subName);

//		holder.bind(position);

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
	 * Cache of the children views for a list item.
	 */
	class SavedPostsViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener{

		@BindView(R.id.post_title_text_view)
		TextView titleTV;
		@BindView(R.id.created_time_text_view)
		TextView createdTimeTV;
		@BindView(R.id.author_text_view)
		TextView authorTV;
		@BindView(R.id.subreddit_name_text_view)
		TextView subNameTV;

		/**
		 * Constructor for our ViewHolder. Within this constructor, we get a reference to our
		 * TextViews and set an onClickListener to listen for clicks. Those will be handled in the
		 * onClick method below.
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
		 * use that integer to display the appropriate text within a list item.
		 * @param listIndex Position of the item in the list
		 */
		void bind(int listIndex) {
//			listItemNumberView.setText(String.valueOf(listIndex));
		}

		/**
		 * Called whenever a user clicks on an item in the list.
		 * @param v The View that was clicked
		 */
		@Override
		public void onClick(View v) {
			mOnClickListener.onListItemClick(112143);
		}
	}
}