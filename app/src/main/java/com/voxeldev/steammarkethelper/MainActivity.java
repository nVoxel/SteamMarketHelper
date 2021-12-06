package com.voxeldev.steammarkethelper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.voxeldev.steammarkethelper.models.adapters.GamesRecyclerViewAdapter;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.common.RequestManager;
import com.voxeldev.steammarkethelper.models.inventory.InventoryManager;
import com.voxeldev.steammarkethelper.ui.settings.SettingsActivity;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private RecyclerView gamesRecyclerView;
    private Elements games;
    public static final String LOG_TAG = "SMH";

    /*
    TODO:Encryption for saved cookies
    TODO:Fix swiperefresh is search mode
    TODO:Datetime in y axis of charts, charts dark theme
    TODO:Inventory search, inventory filters (Chips?) (show only Tradeable & Marketable)
    */

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(getApplicationContext());
        String theme = sharedPreferences.getString("theme", "system");
        setTheme(theme);

        setContentView(R.layout.activity_main);

        MaterialButton settingsButton = findViewById(R.id.settings_button);
        settingsButton.setOnClickListener(view -> {
            startActivity(new Intent(
                    getApplicationContext(), SettingsActivity.class));
            overridePendingTransition(R.anim.left_in, R.anim.right_out);
        });

        new Thread(() -> {
            try{
                AuthModel authModel = new AuthModel(getApplicationContext());
                String cookie = authModel.loadCookie();

                if (cookie == null || authModel.checkAuth(cookie)){
                    startActivity(new Intent(getApplicationContext(), AuthActivity.class));
                }
            }
            catch (Exception e){
                Log.e(LOG_TAG, e.getMessage());
                startActivity(new Intent(getApplicationContext(), AuthActivity.class));
            }
        }).start();

        CircularProgressIndicator inventoryBalanceLoader = findViewById(R.id.balance_loader);
        TextView inventoryBalanceTextView = findViewById(R.id.balance_textview);
        findViewById(R.id.balance_cardview).setOnClickListener(v -> {
            inventoryBalanceTextView.setText(getResources().getString(R.string.wallet_balance_not_set));
            inventoryBalanceLoader.setVisibility(View.VISIBLE);
            loadWalletBalance(inventoryBalanceLoader, inventoryBalanceTextView);
        });

        loadWalletBalance(inventoryBalanceLoader, inventoryBalanceTextView);

        gamesRecyclerView = findViewById(R.id.games_recyclerview);
        gamesRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        if (savedInstanceState != null){
            try{
                games = Jsoup.parse(savedInstanceState.getString("gamesSerialized"))
                        .select("a.game_button");
                setAdapter();
                return;
            }
            catch (Exception e){
                Log.e(LOG_TAG, "Failed to load instanceState: " + e.getMessage());
            }
        }

        loadGames();
    }

    private void loadGames() {
        new Thread(() -> {
            try{
                RequestManager requestManager = new RequestManager(
                        new AuthModel(getApplicationContext()));

                Response response = requestManager.getClient()
                        .newCall(requestManager.buildRequest(
                                "https://steamcommunity.com/market/",
                                "")).execute();

                Document document = Jsoup.parse(response.body().string());

                games = document.select("a.game_button");

                runOnUiThread(this::setAdapter);
            }
            catch (Exception e){
                Log.e(LOG_TAG, e.getMessage());
            }
        }).start();
    }

    private void setAdapter() {
        gamesRecyclerView.setAdapter(new GamesRecyclerViewAdapter(
                this, games, gamesRecyclerView));

        CircularProgressIndicator gamesLoader = findViewById(R.id.loader_games);
        ConstraintLayout gamesLayout = findViewById(R.id.layout_games);

        gamesLoader.setVisibility(View.GONE);

        Transition transition = new Slide(Gravity.BOTTOM);
        transition.setDuration(300);
        transition.addTarget(gamesLayout);

        TransitionManager.beginDelayedTransition(findViewById(R.id.root), transition);
        gamesLayout.setVisibility(View.VISIBLE);
    }

    private void loadWalletBalance(CircularProgressIndicator balanceLoader, TextView inventoryBalanceTextView) {
        new Thread(() -> {
            InventoryManager inventoryManager = new InventoryManager(new AuthModel(getApplicationContext()));
            String balance = inventoryManager.getWalletBalance();

            try{
                if (balance == null || balance.equals("")){
                    runOnUiThread(() -> {
                        balanceLoader.setVisibility(View.GONE);
                        inventoryBalanceTextView.setText(String.format(getResources().getString(R.string.wallet_balance), "null"));
                    });
                    return;
                }

                runOnUiThread(() -> {
                    balanceLoader.setVisibility(View.GONE);
                    inventoryBalanceTextView.setText(String.format(getResources().getString(R.string.wallet_balance), balance));
                });
            }
            catch (Exception ignored){}
        }).start();
    }

    public static void setTheme(String theme){
        switch (theme) {
            case "light":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case "dark":
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
            default:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        try{
            outState.putString("gamesSerialized", games.outerHtml());
            outState.putParcelable("gamesRecyclerSavedState", gamesRecyclerView
                    .getLayoutManager().onSaveInstanceState());
        }
        catch (Exception e){
            Log.e(LOG_TAG, "Failed onSaveInstanceState in MainActivity: " + e.getMessage());
        }
    }
}
