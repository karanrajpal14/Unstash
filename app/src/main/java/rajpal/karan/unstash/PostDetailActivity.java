package rajpal.karan.unstash;

import android.content.Intent;
import android.database.Cursor;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_AUTHOR;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_CREATED_TIME;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_DOMAIN;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_PERMALINK;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_POST_HINT;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_SCORE;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_SUBREDDIT_NAME;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_THUMBNAIL;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_TITLE;
import static rajpal.karan.unstash.SavedPostContract.SavedPostEntry.INDEX_URL;

public class PostDetailActivity extends AppCompatActivity {

	@BindView(R.id.title_post_detail_tv)
	TextView titleDetailTv;
	@BindView(R.id.author_post_detail_tv)
	TextView authorDetailTv;
	@BindView(R.id.created_time_post_detail_tv)
	TextView createdTimeDetailTv;
	@BindView(R.id.sub_name_post_detail_tv)
	TextView subNameDetailTv;
	@BindView(R.id.domain_post_detail_tv)
	TextView domainDetailTv;
	@BindView(R.id.post_hint_post_detail_tv)
	TextView postHintDetailTv;
	@BindView(R.id.permalink_post_detail_tv)
	TextView permalinkDetailTv;
	@BindView(R.id.url_post_detail_tv)
	TextView urlDetailTv;
	@BindView(R.id.score_post_detail_tv)
	TextView scoreDetailTv;
	@BindView(R.id.toolbar2)
	Toolbar toolbar;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_post_detail);
		ButterKnife.bind(this);

		setSupportActionBar(toolbar);
		getSupportActionBar().setTitle(R.string.details_toolbar_title);
		getSupportActionBar().setDisplayHomeAsUpEnabled(true);

		Intent receivedPostIntent = getIntent();
		String postID = receivedPostIntent.getStringExtra("intentClickedPostID");
		Cursor clickedPostCursor = getContentResolver().query(
				SavedPostContract.SavedPostEntry.CONTENT_URI,
				null,
				SavedPostProvider.postWithID,
				new String[] {postID},
				null
		);

		assert clickedPostCursor != null;
		clickedPostCursor.moveToNext();

		String title = clickedPostCursor.getString(INDEX_TITLE);
		String author = clickedPostCursor.getString(INDEX_AUTHOR);
		String thumbnailURL = clickedPostCursor.getString(INDEX_THUMBNAIL);
		String createdTime = Utils.getRelativeTime(clickedPostCursor.getLong(INDEX_CREATED_TIME));
		String subName = clickedPostCursor.getString(INDEX_SUBREDDIT_NAME);
		String domain = clickedPostCursor.getString(INDEX_DOMAIN);
		String postHint = clickedPostCursor.getString(INDEX_POST_HINT);
		String permalink = clickedPostCursor.getString(INDEX_PERMALINK);
		String url = clickedPostCursor.getString(INDEX_URL);
		int score = clickedPostCursor.getInt(INDEX_SCORE);

		titleDetailTv.setText(title);
		authorDetailTv.setText(author);
		createdTimeDetailTv.setText(String.valueOf(createdTime));
		subNameDetailTv.setText(subName);
		domainDetailTv.setText(domain);
		postHintDetailTv.setText(postHint);
		permalinkDetailTv.setText(permalink);
		urlDetailTv.setText(url);
		scoreDetailTv.setText(String.valueOf(score));

		clickedPostCursor.close();
	}
}
