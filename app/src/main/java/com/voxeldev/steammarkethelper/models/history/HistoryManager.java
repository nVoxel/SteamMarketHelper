package com.voxeldev.steammarkethelper.models.history;

import android.util.Log;

import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.common.RequestManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;

import kotlin.NotImplementedError;

public class HistoryManager extends RequestManager {

    private static final int EVENTS_COUNT = 25;

    public HistoryManager(AuthModel authModel) {
        super(authModel);
    }

    // I had to partially refuse GSON here, because it doesn't work with array-like objects
    public HistoryModel getHistoryModel() {
        try {
            JSONObject history = new JSONObject("history json here...");

            JSONObject historyAssetsRoot = history.getJSONObject("assets");
            Iterator<String> rootKeys = historyAssetsRoot.keys();

            while (rootKeys.hasNext()) {
                JSONObject historyAssetsGame = historyAssetsRoot.getJSONObject(rootKeys.next());
                Iterator<String> gameKeys = historyAssetsGame.keys();

                while (gameKeys.hasNext()) {
                    JSONObject historyAssetsCategory = historyAssetsGame
                            .getJSONObject(gameKeys.next());
                    Iterator<String> categoryKeys = historyAssetsCategory.keys();

                    while (categoryKeys.hasNext()) {
                        String asset = historyAssetsCategory.getJSONObject(categoryKeys.next()).toString();

                        System.out.println(asset);
                    }
                }
            }
        } catch (Exception e) {
            Log.e(MainActivity.LOG_TAG, "Failed to getHistoryModel: " + e.getMessage());
        }

        return null;
    }

    private String loadHistory(int start) {
        throw new NotImplementedError();
    }
}
