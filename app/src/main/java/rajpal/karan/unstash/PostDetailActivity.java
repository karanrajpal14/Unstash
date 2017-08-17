package rajpal.karan.unstash;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.widget.TextView;

import butterknife.BindView;
import butterknife.ButterKnife;

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

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_post_detail);

		Toolbar myToolbar = (Toolbar) findViewById(R.id.toolbar);
		setSupportActionBar(myToolbar);

		ButterKnife.bind(this);

		Intent receivedPostIntent = getIntent();
		String title = receivedPostIntent.getStringExtra("intentTitle");
		String author = receivedPostIntent.getStringExtra("intentAuthor");
		long createdTime = receivedPostIntent.getLongExtra("intentCreatedTime",0);
		String subName = receivedPostIntent.getStringExtra("intentSubName");
		String domain = receivedPostIntent.getStringExtra("intentSubName");
		String postHint = receivedPostIntent.getStringExtra("intentPostHint");
		String permalink = receivedPostIntent.getStringExtra("intentPermalink");
		String url = receivedPostIntent.getStringExtra("intentUrl");
		int score = receivedPostIntent.getIntExtra("intentScore", 0);

		titleDetailTv.setText(title);
		authorDetailTv.setText(author);
		createdTimeDetailTv.setText(String.valueOf(createdTime));
		subNameDetailTv.setText(subName);
		domainDetailTv.setText(domain);
		postHintDetailTv.setText(postHint);
		permalinkDetailTv.setText(permalink);
		urlDetailTv.setText(url);
		scoreDetailTv.setText(String.valueOf(score));
	}
}
