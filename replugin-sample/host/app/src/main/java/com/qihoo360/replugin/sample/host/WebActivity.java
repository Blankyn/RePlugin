package com.qihoo360.replugin.sample.host;

import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.os.Bundle;
import android.webkit.WebView;

import com.github.lzyzsd.jsbridge.BridgeWebView;

public class WebActivity extends Activity {


    WebView webView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);

        webView = findViewById(R.id.webView);

        webView.loadUrl("http://cloud.blankm.top:2013/");
    }
}