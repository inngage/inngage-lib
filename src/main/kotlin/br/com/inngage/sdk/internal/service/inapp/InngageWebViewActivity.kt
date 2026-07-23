package br.com.inngage.sdk.internal.service.inapp

import android.os.Bundle
import android.util.Log
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.appcompat.app.AppCompatActivity
import br.com.inngage.sdk.R
import br.com.inngage.sdk.internal.core.config.InngageConfig

/**
 * Displays a URL inside a WebView.
 * Replaces InngageWebViewActivity.java — pure Kotlin, no behaviour change.
 */
internal class InngageWebViewActivity : AppCompatActivity() {

    private val tag = InngageConfig.TAG_NOTIFY
    private var webView: WebView? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.web_view)
        webView = findViewById(R.id.inn_webview)

        intent.getStringExtra(EXTRA_URL)?.let { loadUrl(it) }
    }

    private fun loadUrl(url: String) {
        Log.d(tag, "Opening WebView URL: $url")
        webView?.apply {
            webViewClient = object : WebViewClient() {
                override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
                    view.loadUrl(url)
                    return false
                }
            }
            loadUrl(url)
            requestFocus()
        }
    }

    override fun onDestroy() {
        webView?.destroy()
        webView = null
        super.onDestroy()
    }

    companion object {
        const val EXTRA_URL = "extra_url"
    }
}

