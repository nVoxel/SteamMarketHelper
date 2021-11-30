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
import com.voxeldev.steammarkethelper.models.inventory.InventoryModel;
import com.voxeldev.steammarkethelper.models.market.MarketModel;

public class MarketActivity extends AppCompatActivity {

    public int gameId;
    public InventoryModel loadedInventory;
    public Parcelable inventoryRecyclerViewSavedState;
    public MarketModel loadedMarket;
    public Parcelable marketRecyclerViewSavedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        gameId = getIntent().getIntExtra("gameId", 0);

        setContentView(R.layout.activity_market);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        NavController navController = Navigation
                .findNavController(this, R.id.nav_host_fragment);

        NavigationUI.setupWithNavController(navView, navController);

        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null){
            actionBar.setDisplayHomeAsUpEnabled(true);
        }

        if (savedInstanceState != null) {
            loadedInventory = new Gson().fromJson(
                    savedInstanceState.getString("inventorySerialized"), InventoryModel.class);
            inventoryRecyclerViewSavedState = savedInstanceState
                    .getParcelable("inventoryRecyclerSavedState");
            loadedMarket = new Gson().fromJson(
                    savedInstanceState.getString("marketSerialized"), MarketModel.class);
            marketRecyclerViewSavedState = savedInstanceState
                    .getParcelable("marketRecyclerSavedState");
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
        outState.putString("inventorySerialized", new Gson().toJson(loadedInventory));
        outState.putParcelable("inventoryRecyclerSavedState", inventoryRecyclerViewSavedState);
        outState.putString("marketSerialized", new Gson().toJson(loadedMarket));
        outState.putParcelable("marketRecyclerSavedState", marketRecyclerViewSavedState);
    }
}