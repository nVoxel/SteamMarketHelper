package com.voxeldev.steammarkethelper.models.common;

import android.content.Context;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.voxeldev.steammarkethelper.MainActivity;

import java.util.ArrayList;
import java.util.List;

public class FavoriteGamesModel {

    private final Gson gson;
    private final CacheModel cacheModel;
    private List<String> favorites;

    public FavoriteGamesModel(Context context) {
        gson = new Gson();
        cacheModel = new CacheModel(context);
        favorites = loadFavorites();
    }

    public boolean isInFavorites(String name) {
        return favorites.contains(name);
    }

    public void addToFavorites(String name) {
        favorites.add(name);
        saveFavorites();
    }

    public void removeFromFavorites(String name) {
        favorites.remove(name);
        saveFavorites();
    }

    private List<String> loadFavorites() {
        try{
            List<String> loadedFavorites = gson.fromJson(
                    cacheModel.readFile("favorites.smh"),
                    new TypeToken<List<String>>(){}.getType()
            );

            return (loadedFavorites == null) ? new ArrayList<>() : loadedFavorites;
        }
        catch (Exception e){
            Log.e(MainActivity.LOG_TAG, "Failed to load favorites: " + e.getMessage());
        }

        return new ArrayList<>();
    }

    private void saveFavorites() {
        try{
            cacheModel.writeToFile("favorites.smh",
                    gson.toJson(favorites));
        }
        catch (Exception e){
            Log.e(MainActivity.LOG_TAG, "Failed to save favorites: " + e.getMessage());
        }
    }
}
