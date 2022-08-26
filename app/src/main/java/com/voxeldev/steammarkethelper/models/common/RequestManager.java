package com.voxeldev.steammarkethelper.models.common;

import com.voxeldev.steammarkethelper.models.auth.AuthModel;

import okhttp3.OkHttpClient;
import okhttp3.Request;

public class RequestManager {

    public static final String DEFAULT_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36";
    public static final String ICON_URL_PREFIX = "https://community.akamai.steamstatic.com/economy/image/";
    private final OkHttpClient client;
    private final AuthModel authModel;

    public RequestManager(AuthModel authModel) {
        client = new OkHttpClient();
        this.authModel = authModel;
    }

    public OkHttpClient getClient() { return client; }
    public AuthModel getAuthModel() { return authModel; }

    public Request buildRequest(String url, String cookie) {
        return new Request.Builder()
                .url(url)
                .addHeader("Cookie", cookie)
                .addHeader("User-Agent", DEFAULT_USER_AGENT)
                .build();
    }
}
