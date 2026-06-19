package com.hermes.webui;

import android.annotation.SuppressLint;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {
    private static final String TAG = "HermesWebUI";
    private static final String DEFAULT_URL = "http://localhost:8787";

    private WebView webView;
    private EditText urlInput;
    private TextView statusText;
    private TextView titleText;
    private ImageButton btnBack, btnRefresh, btnGo;

    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // 初始化控件
        webView = findViewById(R.id.webView);
        urlInput = findViewById(R.id.urlInput);
        statusText = findViewById(R.id.statusText);
        titleText = findViewById(R.id.titleText);
        btnBack = findViewById(R.id.btnBack);
        btnRefresh = findViewById(R.id.btnRefresh);
        btnGo = findViewById(R.id.btnGo);

        // 配置 WebView
        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setDomStorageEnabled(true);
        settings.setAllowFileAccess(true);
        settings.setAllowContentAccess(true);
        settings.setMediaPlaybackRequiresUserGesture(false);
        settings.setCacheMode(WebSettings.LOAD_DEFAULT);
        settings.setMixedContentMode(WebSettings.MIXED_CONTENT_ALWAYS_ALLOW);
        settings.setUseWideViewPort(true);
        settings.setLoadWithOverviewMode(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setDisplayZoomControls(false);

        // 允许文件上传
        webView.setWebChromeClient(new WebChromeClient());

        // WebView 客户端
        webView.setWebViewClient(new WebViewClient() {
            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
                statusText.setText("加载中: " + url);
                statusText.setVisibility(View.VISIBLE);
                urlInput.setText(url);
                Log.d(TAG, "开始加载: " + url);
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                super.onPageFinished(view, url);
                statusText.setVisibility(View.GONE);
                titleText.setText(view.getTitle() != null ? view.getTitle() : "Hermes WebUI");
                Log.d(TAG, "加载完成: " + url);
            }

            @Override
            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
                super.onReceivedError(view, request, error);
                if (request.isForMainFrame()) {
                    String url = request.getUrl().toString();
                    statusText.setText("连接失败，请检查服务是否运行");
                    statusText.setVisibility(View.VISIBLE);
                    Log.e(TAG, "加载错误: " + url);
                }
            }

            @Override
            public boolean shouldOverrideUrlLoading(WebView view, WebResourceRequest request) {
                // 允许在 WebView 内加载所有链接
                return false;
            }
        });

        // 返回按钮 (WebView 后退)
        btnBack.setOnClickListener(v -> {
            if (webView.canGoBack()) {
                webView.goBack();
            } else {
                Toast.makeText(this, "已是最后一页", Toast.LENGTH_SHORT).show();
            }
        });

        // 长按返回 = 刷新
        btnBack.setOnLongClickListener(v -> {
            webView.reload();
            return true;
        });

        // 刷新按钮
        btnRefresh.setOnClickListener(v -> webView.reload());

        // 连接按钮
        btnGo.setOnClickListener(v -> loadUrl());

        // URL 输入框回车
        urlInput.setOnEditorActionListener((v, actionId, event) -> {
            loadUrl();
            return true;
        });

        // 加载默认地址
        urlInput.setText(DEFAULT_URL);
        webView.loadUrl(DEFAULT_URL);
    }

    private void loadUrl() {
        String url = urlInput.getText().toString().trim();
        if (url.isEmpty()) {
            url = DEFAULT_URL;
            urlInput.setText(url);
        }
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            url = "http://" + url;
        }
        webView.loadUrl(url);
    }

    @Override
    public void onBackPressed() {
        if (webView.canGoBack()) {
            webView.goBack();
        } else {
            super.onBackPressed();
        }
    }
}
