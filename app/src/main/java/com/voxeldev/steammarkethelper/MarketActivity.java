package com.voxeldev.steammarkethelper;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.MenuItem;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.NavigationUI;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.voxeldev.steammarkethelper.models.inventory.InventoryModel;
import com.voxeldev.steammarkethelper.models.listings.ListingModel;
import com.voxeldev.steammarkethelper.models.market.MarketModel;

import java.util.List;

public class MarketActivity extends AppCompatActivity {

    public int gameId;
    public String gameName;
    public InventoryModel loadedInventory;
    public Parcelable inventoryRecyclerViewSavedState;
    public MarketModel loadedMarket;
    public Parcelable marketRecyclerViewSavedState;
    public List<ListingModel> loadedSellListings;
    public List<ListingModel> loadedBuyOrders;
    public Parcelable sellRecyclerViewSavedState;
    public Parcelable buyRecyclerViewSavedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameId = getIntent().getIntExtra("gameId", 0);
        gameName = getIntent().getStringExtra("gameName");

        setContentView(R.layout.activity_market);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation
                .findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupWithNavController(navView, navController);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setTitle(gameName);
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            Gson gson = new Gson();

            loadedInventory = gson.fromJson(
                    savedInstanceState.getString("inventorySerialized"), InventoryModel.class);
            inventoryRecyclerViewSavedState = savedInstanceState
                    .getParcelable("inventoryRecyclerSavedState");
            loadedMarket = gson.fromJson(
                    savedInstanceState.getString("marketSerialized"), MarketModel.class);
            marketRecyclerViewSavedState = savedInstanceState
                    .getParcelable("marketRecyclerSavedState");
            loadedSellListings = gson.fromJson(
                    savedInstanceState.getString("sellListingsSerialized"),
                    new TypeToken<List<ListingModel>>(){}.getType());
            loadedBuyOrders = gson.fromJson(
                    savedInstanceState.getString("buyOrdersSerialized"),
                    new TypeToken<List<ListingModel>>(){}.getType());
            sellRecyclerViewSavedState = savedInstanceState
                    .getParcelable("sellRecyclerSavedState");
            buyRecyclerViewSavedState = savedInstanceState
                    .getParcelable("buyRecyclerSavedState");
        }
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home){
            finish();
            overridePendingTransition(R.anim.left_in, R.anim.right_out);
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.left_in, R.anim.right_out);
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Gson gson = new Gson();

        outState.putString("inventorySerialized", gson.toJson(loadedInventory));
        outState.putParcelable("inventoryRecyclerSavedState", inventoryRecyclerViewSavedState);
        outState.putString("marketSerialized", gson.toJson(loadedMarket));
        outState.putParcelable("marketRecyclerSavedState", marketRecyclerViewSavedState);
        outState.putString("sellListingsSerialized", gson.toJson(loadedSellListings));
        outState.putString("buyOrdersSerialized", gson.toJson(loadedBuyOrders));
        outState.putParcelable("sellRecyclerSavedState", sellRecyclerViewSavedState);
        outState.putParcelable("buyRecyclerSavedState", buyRecyclerViewSavedState);
    }
}