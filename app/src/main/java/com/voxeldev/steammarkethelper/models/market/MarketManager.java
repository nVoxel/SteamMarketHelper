package com.voxeldev.steammarkethelper.models.market;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import androidx.room.Room;

import com.google.gson.Gson;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.common.RequestManager;
import com.voxeldev.steammarkethelper.models.db.NameIdDao;
import com.voxeldev.steammarkethelper.models.db.NameIdDb;
import com.voxeldev.steammarkethelper.models.db.NameIdPair;

import java.io.IOException;
import java.util.Locale;

import okhttp3.Request;
import okhttp3.Response;

public class MarketManager extends RequestManager {

    NameIdDb database;

    public MarketManager(Context context) {
        super(new AuthModel(context));
        database = Room.databaseBuilder(context, NameIdDb.class, "nameIdDb").build();
    }

    public MarketModel getMarketModel(int start, int count, int appId, String searchQuery){
        try {
            Request request = buildRequest(String.format(Locale.getDefault(),
                    "https://steamcommunity.com/market/search/render/?query=%s&start=%d&count=%d&search_descriptions=0&sort_column=popular&sort_dir=desc&appid=%d&norender=1",
                    searchQuery, start, count, appId),
                    getAuthModel().loadCookie());

            Response response = getClient().newCall(request).execute();

            Gson gson = new Gson();

            return gson.fromJson(response.body().string(), MarketModel.class);
        }
        catch (Exception e){
            Log.d("SMH", e.toString());
            return null;
        }
    }

    public MarketItemCommodityModel getItemCommodity(String name){
        try{
            NameIdDao nameIdDao = database.nameIdDao();

            NameIdPair nameIdPair = nameIdDao.getNameId(name);
            String id;

            if (nameIdPair == null || nameIdPair.nameId == null || nameIdPair.nameId.equals("")){
                id = loadItemId(name);

                nameIdPair = new NameIdPair(name, id);
                nameIdDao.insertNameId(nameIdPair);
            }
            else{
                id = nameIdPair.nameId;
            }

            MarketItemCommodityModel marketItemCommodityModel = loadItemCommodity(id);

            if (marketItemCommodityModel == null){ //Seems itemId in our db is not valid
                Log.e("SMH", "itemId is not valid: " + name);
                id = loadItemId(name);
                nameIdPair.nameId = id;
                nameIdDao.updateNameId(nameIdPair);
                database.close();
                return loadItemCommodity(id);
            }

            database.close();
            return marketItemCommodityModel;
        } catch (Exception e) {
            database.close();
            Log.e("SMH", e.toString());
            return null;
        }
    }

    private MarketItemCommodityModel loadItemCommodity(String id){
        try{
            Request request = buildRequest(String.format(Locale.getDefault(),
                    "https://steamcommunity.com/market/itemordershistogram?country=RU&language=english&currency=5&item_nameid=%s&two_factor=0&norender=1", id),
                    "webTradeEligibility=%7B%22allowed%22%3A1%2C%22allowed_at_time%22%3A0%2C%22steamguard_required_days%22%3A15%2C%22new_device_cooldown_days%22%3A7%2C%22time_checked%22%3A1620583359%7D;" + getAuthModel().loadCookie());
            Response response = getClient().newCall(request).execute();

            return new Gson().fromJson(response.body().string(), MarketItemCommodityModel.class);
        } catch (Exception e) {
            Log.e("SMH", e.toString());
            return null;
        }
    }

    public MarketItemPriceHistory loadItemPriceHistory(String name){
        try{
            Request request = buildRequest(
                    "https://steamcommunity.com/market/pricehistory/?country=RU&currency=3&appid=252490&market_hash_name=" + name,
                    getAuthModel().loadCookie()
            );
            Response response = getClient().newCall(request).execute();

            return new Gson().fromJson(response.body().string(), MarketItemPriceHistory.class);
        }
        catch (Exception e){
            Log.e("SMH", e.toString());
            return null;
        }
    }

    private String loadItemId(String name) throws Exception {
        Request idRequest = buildRequest(String.format(Locale.getDefault(),
                "https://steamcommunity.com/market/listings/252490/%s", name),
                "webTradeEligibility=%7B%22allowed%22%3A1%2C%22allowed_at_time%22%3A0%2C%22steamguard_required_days%22%3A15%2C%22new_device_cooldown_days%22%3A7%2C%22time_checked%22%3A1620583359%7D;" + getAuthModel().loadCookie());
        Response idResponse = getClient().newCall(idRequest).execute();

        String body = idResponse.body().string();
        int before = body.indexOf("Market_LoadOrderSpread(");
        int after = body.indexOf(")", before);

        return body.substring(before + "Market_LoadOrderSpread(".length(), after).replace(" ", "");
    }
}
