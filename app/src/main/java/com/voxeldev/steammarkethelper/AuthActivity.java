package com.voxeldev.steammarkethelper;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.webkit.CookieManager;
import android.webkit.WebView;
import android.webkit.WebViewClient;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.voxeldev.steammarkethelper.models.auth.AuthModel;

public class AuthActivity extends AppCompatActivity {
    @SuppressLint("SetJavaScriptEnabled")
    @Override
    protected void onCreate(@Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_auth);

        WebView authWebView = findViewById(R.id.auth_webview);
        authWebView.getSettings().setJavaScriptEnabled(true);
        authWebView.setWebViewClient(new WebViewClient(){
            @Override
            public void onPageFinished(WebView view, String url) {
                if (!(url.contains("https://steamcommunity.com/id/") || url.contains("https://steamcommunity.com/profiles/"))){ return; }

                AuthModel authModel = new AuthModel(getApplicationContext());
                String cookie = CookieManager.getInstance().getCookie(url);

                if (cookie == null || cookie.equals("")){ return; }

                new Thread(() -> {
                    if (authModel.checkAuth(cookie)) {
                        authWebView.reload();
                        return;
                    }

                    try {
                        authModel.saveCookie(cookie);
                        finish();
                    }
                    catch (Exception e){ authWebView.reload(); }
                }).start();
            }
        });

        authWebView.loadUrl("https://steamcommunity.com/login/home");
    }
}
