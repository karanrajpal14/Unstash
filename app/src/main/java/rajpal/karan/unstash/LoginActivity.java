package rajpal.karan.unstash;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Toast;

import net.dean.jraw.auth.AuthenticationManager;
import net.dean.jraw.http.oauth.Credentials;
import net.dean.jraw.http.oauth.OAuthHelper;

import java.net.URL;

import butterknife.BindView;
import butterknife.ButterKnife;

public class LoginActivity extends AppCompatActivity {

    @BindView(R.id.webview)
    WebView webView;
    @BindView(R.id.login_toolbar)
    Toolbar toolbar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        ButterKnife.bind(this);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            toolbar.setTitle("Log in to Reddit");
        }

        webView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                return false;
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                if (url.contains("code=")) {
                    webView.stopLoading();
                    setResult(RESULT_OK, new Intent().putExtra("RESULT_URL", url));
                    finish();
                } else if (url.contains("error=")) {
                    Toast.makeText(LoginActivity.this, "You must click on allow to complete the sign-in process", Toast.LENGTH_SHORT).show();
                    webView.loadUrl(getAuthorizationUrl().toExternalForm());
                }
            }
        });
        webView.loadUrl(getAuthorizationUrl().toExternalForm());
    }

    private URL getAuthorizationUrl() {
        OAuthHelper oAuthHelper = AuthenticationManager.get().getRedditClient().getOAuthHelper();
        Credentials credentials = ((App) getApplication()).getInstalledAppCredentials();
        // OAuth2 scopes to request. See https://www.reddit.com/dev/api/oauth for a full list
        String[] scopes = {"identity", "edit", "flair", "read", "vote",
                "submit", "history", "save"};
        return oAuthHelper.getAuthorizationUrl(credentials, true, true, scopes);
    }

    @Override
    protected void onDestroy() {
        webView.destroy();
        super.onDestroy();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
