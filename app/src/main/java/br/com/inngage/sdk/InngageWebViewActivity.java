package br.com.inngage.sdk;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class InngageWebViewActivity extends AppCompatActivity {

    private static final String TAG = InngageConstants.TAG;
    private String name;
    private WebView webView;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        String url ="";
        Bundle bundle = getIntent().getExtras();

        if (getIntent().hasExtra("EXTRA_URL")) {

            url = bundle.getString("EXTRA_URL");
        }


        webView = new WebView(this);
        webView.getSettings().setJavaScriptEnabled(true);
        webView.setWebViewClient(new WebViewClient());
        webView.loadUrl(url);
        setContentView(webView);
    }

    @Override
    protected void onDestroy(){
        super.onDestroy();

        if (webView != null) {
            webView.destroy();
        }
    }
//    @Override
//    protected void onCreate(Bundle savedInstanceState) {
//        super.onCreate(savedInstanceState);
//        setContentView(R.layout.web_view);
//
//        Log.d(TAG, "Calling InngageWebViewActivity...");
//
//        //Intent intent = getIntent();
//        //Bundle bundle  = intent.getExtras();
//
//
//        //Log.d(TAG, "Calling InngageWebViewActivity to open URL: " + bundle.getString("URL"));
//
//
//        String url = null;
//
//        Intent intent = getIntent();
//        Bundle bundle  = intent.getExtras();
//
//        if (bundle.getString("url") != null) {
//
//            Log.d(TAG, "Calling InngageWebViewActivity to open URL: " + bundle.getString("url"));
//        }
//        if (getIntent().hasExtra("URL")) {
//          url = getIntent().getStringExtra("URL");
//            Log.d(TAG, "Calling InngageWebViewActivity to open URL: " + name);
//        } else {
//            Log.d(TAG, "Activity cannot find  extras ");
//        }
//
//        //String url = "";
//
//        //Intent intent = getIntent();
//        //Bundle bundle  = intent.getExtras();
//
//        Log.d(TAG, "Opening WebView component");
//
//        if ((bundle != null) && (bundle.getString("URL") != null)) {
//
//            url = bundle.getString("URL");
//
//            Log.d(TAG, "Opening URL: " + url);
//
//            WebView myWebView = findViewById(R.id.webview);
//
//            if (myWebView != null) {
//
//                myWebView.loadUrl(url);
//
//            } else {
//
//                Log.d(TAG, "WebView object no has a instance");
//            }
//        }
//
//    }
}
