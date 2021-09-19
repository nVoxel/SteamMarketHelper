package com.voxeldev.steammarkethelper;

import android.content.Intent;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.gson.Gson;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.inventory.InventoryManager;
import com.voxeldev.steammarkethelper.models.inventory.InventoryModel;
import com.voxeldev.steammarkethelper.models.market.MarketModel;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

public class MainActivity extends AppCompatActivity {

    public InventoryModel loadedInventory;
    public Parcelable inventoryRecyclerViewSavedState;
    public MarketModel loadedMarket;
    public Parcelable marketRecyclerViewSavedState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        BottomNavigationView navView = findViewById(R.id.nav_view);
        AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(R.id.navigation_inventory, R.id.navigation_market, R.id.navigation_settings).build();
        NavController navController = Navigation.findNavController(this, R.id.nav_host_fragment);
        NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
        NavigationUI.setupWithNavController(navView, navController);

        if (savedInstanceState != null) {
            loadedInventory = new Gson().fromJson(savedInstanceState.getString("inventorySerialized"), InventoryModel.class);
            inventoryRecyclerViewSavedState = savedInstanceState.getParcelable("inventoryRecyclerSavedState");
            loadedMarket = new Gson().fromJson(savedInstanceState.getString("marketSerialized"), MarketModel.class);
            marketRecyclerViewSavedState = savedInstanceState.getParcelable("marketRecyclerSavedState");
        }

        Thread authTherad = new Thread(() -> {
            try{
                AuthModel authModel = new AuthModel(getApplicationContext());
                String cookie = authModel.loadCookie();

                if (cookie == null || authModel.checkAuth(cookie)){
                    startActivity(new Intent(getApplicationContext(), AuthActivity.class));
                }
            }
            catch (Exception e){
                Log.e("SMH", e.getMessage());
                startActivity(new Intent(getApplicationContext(), AuthActivity.class));
            }
        });
        authTherad.start();
    }

    /*
    TODO:Fix swiperefresh is search mode
    TODO:Datetime in y axis of charts, charts dark theme
    TODO:Inventory search, inventory filters (Chips?) (show only Tradeable & Marketable)
    */

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("inventorySerialized", new Gson().toJson(loadedInventory));
        outState.putParcelable("inventoryRecyclerSavedState", inventoryRecyclerViewSavedState);
        outState.putString("marketSerialized", new Gson().toJson(loadedMarket));
        outState.putParcelable("marketRecyclerSavedState", marketRecyclerViewSavedState);
    }
}