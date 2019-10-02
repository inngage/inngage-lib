package br.com.inngage.sdk;


import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class InngageWebViewActivity extends AppCompatActivity {
    private class MyWebViewClient extends WebViewClient {
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return false;
        }
    }

    private static final String TAG = InngageConstants.TAG;
    private String name;
    private WebView webview;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.web_view);
        webview = findViewById(R.id.inn_webview);


    }

    public void web(String url) {


        Log.d(TAG, "Opening WebView component");


        Log.d(TAG, "Opening URL: " + url);
        try {

            webview.setWebViewClient(new MyWebViewClient());
            webview.loadUrl(url);
            webview.requestFocus();
            webview.canGoBack();
        } catch (Exception ex) {
            Log.d(TAG, "couldn't open the link due to :  " + ex);
        }
    }


    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (webview != null) {
            webview.destroy();
        }
    }
}
