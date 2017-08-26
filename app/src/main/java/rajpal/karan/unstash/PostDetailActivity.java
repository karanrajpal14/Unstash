package rajpal.karan.unstash;

import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import com.bachors.wordtospan.WordToSpan;

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
    @BindView(R.id.post_details_tv)
    TextView postDetailsTv;
    @BindView(R.id.domain_post_detail_tv)
    TextView domainDetailTv;
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
                new String[]{postID},
                null
        );

        assert clickedPostCursor != null;
        clickedPostCursor.moveToNext();

        WordToSpan wordToSpan = new WordToSpan();
        wordToSpan.setColorURL(getResources().getColor(R.color.colorAccent));
        wordToSpan.setUnderlineURL(true);

        String title = clickedPostCursor.getString(INDEX_TITLE);
        String author = clickedPostCursor.getString(INDEX_AUTHOR);
        String createdTime = Utils.getRelativeTime(clickedPostCursor.getLong(INDEX_CREATED_TIME));
        String subName = clickedPostCursor.getString(INDEX_SUBREDDIT_NAME);
        String thumbnailURL = clickedPostCursor.getString(INDEX_THUMBNAIL);
        String domain = clickedPostCursor.getString(INDEX_DOMAIN);
        String postHint = clickedPostCursor.getString(INDEX_POST_HINT);
        String permalink = clickedPostCursor.getString(INDEX_PERMALINK);
        String url = clickedPostCursor.getString(INDEX_URL);
        String score = String.valueOf(clickedPostCursor.getInt(INDEX_SCORE));

        titleDetailTv.setText(title);
        postDetailsTv.setText(
                getResources().getString(R.string.post_details_textview,
                        author,
                        createdTime,
                        subName
                )
        );
        domainDetailTv.setText(getResources().getString(R.string.domain_detail_textview,domain));
        wordToSpan.setLink(url, urlDetailTv);
        scoreDetailTv.setText(getResources().getString(R.string.score_detail_textview,score));

        wordToSpan.setClickListener(new WordToSpan.ClickListener() {
            @Override
            public void onClick(String type, String text) {
                if (type.equals("url")) {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(text)));
                }
            }
        });

        clickedPostCursor.close();
    }
}
