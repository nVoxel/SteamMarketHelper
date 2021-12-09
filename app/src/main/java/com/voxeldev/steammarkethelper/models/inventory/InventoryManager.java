package com.voxeldev.steammarkethelper.models.inventory;

import android.util.Log;

import com.google.gson.Gson;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.common.RequestManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

public class InventoryManager extends RequestManager {

    public InventoryManager(AuthModel authModel) {
        super(authModel);
    }

    public InventoryModel getInventoryModel(int gameId){
        try{
            Request request = buildRequest(String.format(Locale.getDefault(),
                    "https://steamcommunity.com/inventory/%s/%d/2?l=english&count=5000&norender=1",
                    getSteamId(), gameId),
                    getAuthModel().loadCookie());

            Response response = getClient().newCall(request).execute();

            return new Gson().fromJson(response.body().string(), InventoryModel.class);
        }
        catch (Exception e){
            Log.e(MainActivity.LOG_TAG, e.toString());
            return null;
        }
    }

    public String getWalletBalance(){
        try {
            Request request = buildRequest(
                    "https://store.steampowered.com/account/history",
                    getAuthModel().loadCookie());

            Response response = getClient().newCall(request).execute();

            Document document = Jsoup.parse(response.body().string());

            Element balance = document.selectFirst("a#header_wallet_balance");

            return balance.text();
        }
        catch (Exception e){ Log.e(MainActivity.LOG_TAG, e.toString()); }
        return null;
    }

    public String getSteamId(){
        try {
            String cookie = getAuthModel().loadCookie();

            if (cookie == null || cookie.equals("")){ return null; }

            String findStr = "steamLoginSecure=";
            int index1 = cookie.indexOf(findStr);
            int index2 = cookie.indexOf("%7C%7C", index1);

            return cookie.substring(index1 + findStr.length(), index2);
        }
        catch (Exception e){
            Log.e(MainActivity.LOG_TAG, e.toString());
            return null;
        }
    }
}
