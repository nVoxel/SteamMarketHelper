package com.voxeldev.steammarkethelper;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
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

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.voxeldev.steammarkethelper.models.adapters.GamesRecyclerViewAdapter;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.common.RequestManager;
import com.voxeldev.steammarkethelper.ui.settings.SettingsActivity;

import org.jetbrains.annotations.NotNull;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import okhttp3.Response;

public class MainActivity extends AppCompatActivity {

    private TextView profileCardTextView;
    private CircularProgressIndicator profileCardLoader;
    private String profileImageUrl;
    private String profileName;
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

                if (cookie == null || authModel.checkAuth(cookie)) {
                    startActivity(new Intent(getApplicationContext(), AuthActivity.class));
                    finish();
                }
            }
            catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
                startActivity(new Intent(getApplicationContext(), AuthActivity.class));
                finish();
            }
        }).start();

        profileCardTextView = findViewById(R.id.profile_card_textview);
        profileCardLoader = findViewById(R.id.profile_card_loader);

        gamesRecyclerView = findViewById(R.id.games_recyclerview);
        gamesRecyclerView.setLayoutManager(new LinearLayoutManager(getApplicationContext()));

        if (savedInstanceState != null) {
            try{
                profileImageUrl = savedInstanceState.getString("profileImageUrl");
                profileName = savedInstanceState.getString("profileName");
                setProfileCard();

                games = Jsoup.parse(savedInstanceState.getString("gamesSerialized"))
                        .select("a.game_button");
                setAdapter();
                return;
            }
            catch (Exception e) {
                Log.e(LOG_TAG, "Failed to load instanceState: " + e.getMessage());
            }
        }

        loadViews();
    }

    private void loadViews() {
        new Thread(() -> {
            try{
                RequestManager requestManager = new RequestManager(
                        new AuthModel(getApplicationContext()));

                Response response = requestManager.getClient()
                        .newCall(requestManager.buildRequest(
                                "https://steamcommunity.com/market/",
                                AuthModel.necessaryMarketCookie +
                                        requestManager.getAuthModel().loadCookie())).execute();

                Document document = Jsoup.parse(response.body().string());

                profileImageUrl = document.selectFirst("span.avatarIcon img")
                        .attr("src");
                profileName = document.selectFirst("span#account_pulldown")
                        .text().replace(" ", "");
                games = document.select("a.game_button");

                runOnUiThread(() -> {
                    setProfileCard();
                    setAdapter();
                });
            }
            catch (Exception e) {
                Log.e(LOG_TAG, e.getMessage());
            }
        }).start();
    }

    private void setProfileCard() {
        profileCardTextView.setText(profileName == null ?
                getString(R.string.app_name) : profileName);

        if (profileImageUrl == null) {
            return;
        }

        int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 40,
                getResources().getDisplayMetrics());

        //noinspection SuspiciousNameCombination
        Glide.with(getApplicationContext())
                .load(profileImageUrl)
                .into(new CustomTarget<Drawable>(width, width) {
                    @Override
                    public void onResourceReady(@NonNull @NotNull Drawable resource,
                                                @Nullable com.bumptech.glide.request.transition.Transition<? super Drawable> transition) {
                        profileCardTextView.setCompoundDrawablesWithIntrinsicBounds(resource, null, null, null);
                    }

                    @Override
                    public void onLoadCleared(@Nullable @org.jetbrains.annotations.Nullable Drawable placeholder) {
                        profileCardTextView.setCompoundDrawablesWithIntrinsicBounds(placeholder, null, null, null);
                    }
                });

        profileCardLoader.setVisibility(View.GONE);
        makeTransition(Gravity.START, 500, findViewById(R.id.profile_cardview), profileCardTextView);
        profileCardTextView.setVisibility(View.VISIBLE);
    }

    private void setAdapter() {
        gamesRecyclerView.setAdapter(new GamesRecyclerViewAdapter(
                this, games, gamesRecyclerView));

        CircularProgressIndicator gamesLoader = findViewById(R.id.loader_games);
        ConstraintLayout gamesLayout = findViewById(R.id.layout_games);

        gamesLoader.setVisibility(View.GONE);
        makeTransition(Gravity.BOTTOM, 300, findViewById(R.id.root), gamesLayout);
        gamesLayout.setVisibility(View.VISIBLE);
    }

    private void makeTransition(int slideEdge, int duration, View root, View target) {
        Transition transition = new Slide(slideEdge);
        transition.setDuration(duration);
        transition.addTarget(target);

        TransitionManager.beginDelayedTransition((ViewGroup) root, transition);
    }

    public static void setTheme(String theme) {
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
            outState.putString("profileImageUrl", profileImageUrl);
            outState.putString("profileName", profileName);
            outState.putString("gamesSerialized", games.outerHtml());
            outState.putParcelable("gamesRecyclerSavedState", gamesRecyclerView
                    .getLayoutManager().onSaveInstanceState());
        }
        catch (Exception e) {
            Log.e(LOG_TAG, "Failed onSaveInstanceState in MainActivity: " + e.getMessage());
        }
    }
}