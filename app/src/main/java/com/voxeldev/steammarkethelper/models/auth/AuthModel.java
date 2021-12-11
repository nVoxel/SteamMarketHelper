package com.voxeldev.steammarkethelper.models.auth;

import android.content.Context;
import android.util.Log;

import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.models.common.CacheModel;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class AuthModel {

    private final OkHttpClient client;
    private CacheModel cacheModel;
    public static final String necessaryMarketCookie = "webTradeEligibility=%7B%22allowed%22%3A1%2C%22allowed_at_time%22%3A0%2C%22steamguard_required_days%22%3A15%2C%22new_device_cooldown_days%22%3A7%2C%22time_checked%22%3A1620583359%7D;";

    public AuthModel(Context context) {
        client = new OkHttpClient();
        cacheModel = new CacheModel(context);
    }

    public boolean checkAuth(String cookie) { // Returns false if auth is success!
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/90.0.4430.93 Safari/537.36";
        Request request = new Request.Builder()
                .url("https://steamcommunity.com/")
                .addHeader("Cookie", cookie)
                .addHeader("User-Agent", userAgent)
                .build();

        try{
            Response response = client.newCall(request).execute();

            return response.body().string().contains("https://steamcommunity.com/login/home");
        }
        catch (Exception e) { Log.e(MainActivity.LOG_TAG, e.toString()); }

        return true;
    }

    public void saveCookie(String cookie) throws Exception {
        cacheModel.writeToFile("cookie.smh", cookie);
    }

    public String loadCookie() throws Exception {
        return cacheModel.readFile("cookie.smh");
    }
}
