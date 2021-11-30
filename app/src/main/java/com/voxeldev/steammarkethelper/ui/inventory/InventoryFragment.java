package com.voxeldev.steammarkethelper.ui.inventory;

import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.MarketActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.adapters.InventoryRecyclerViewAdapter;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.inventory.InventoryManager;
import com.voxeldev.steammarkethelper.models.inventory.InventoryModel;

import org.jetbrains.annotations.NotNull;

public class InventoryFragment extends Fragment {

    private InventoryModel loadedInventory;
    private RecyclerView inventoryRecyclerView;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_inventory, container, false);

        inventoryRecyclerView = root.findViewById(R.id.inventory_recyclerview);
        inventoryRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));

        CircularProgressIndicator inventoryLoader = root.findViewById(R.id.inventory_loader);

        SwipeRefreshLayout swipeRefreshLayout = root.findViewById(R.id.inventory_swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(() -> {
            inventoryRecyclerView.setVisibility(View.GONE);
            inventoryLoader.setVisibility(View.VISIBLE);
            Parcelable state = inventoryRecyclerView.getLayoutManager().onSaveInstanceState();
            loadInventory(inventoryLoader);
            inventoryRecyclerView.getLayoutManager().onRestoreInstanceState(state);
            swipeRefreshLayout.setRefreshing(false);
        });

        FloatingActionButton marketGoTopButton = root.findViewById(R.id.inventory_goTopButton);
        marketGoTopButton.setOnClickListener(v -> ((StaggeredGridLayoutManager)inventoryRecyclerView.getLayoutManager()).scrollToPositionWithOffset(0, 0));

        if (savedInstanceState != null){
            loadedInventory = new Gson().fromJson(savedInstanceState.getString("inventorySerialized", ""),
                    InventoryModel.class);
            setAdapter(inventoryRecyclerView);
            inventoryLoader.setVisibility(View.GONE);
            inventoryRecyclerView.setVisibility(View.VISIBLE);
            return root;
        }

        MarketActivity marketActivity = (MarketActivity)requireActivity();
        if (marketActivity.loadedInventory != null && marketActivity.inventoryRecyclerViewSavedState != null){
            loadedInventory = marketActivity.loadedInventory;
            setAdapter(inventoryRecyclerView);
            inventoryRecyclerView.getLayoutManager().onRestoreInstanceState(marketActivity.inventoryRecyclerViewSavedState);
            inventoryLoader.setVisibility(View.GONE);
            inventoryRecyclerView.setVisibility(View.VISIBLE);
            return root;
        }

        loadInventory(inventoryLoader);

        return root;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void loadInventory(CircularProgressIndicator loader){
        new Thread(() -> {
            try{
                InventoryManager inventoryManager = new InventoryManager(new AuthModel(requireContext()));
                loadedInventory = inventoryManager.getInventoryModel(((MarketActivity)requireActivity()).gameId);

                requireActivity().runOnUiThread(() -> {
                    loader.setVisibility(View.GONE);
                    inventoryRecyclerView.setVisibility(View.VISIBLE);
                    setAdapter(inventoryRecyclerView);
                });
            }
            catch (Exception e){ Log.e(MainActivity.LOG_TAG, e.toString()); }
        }).start();
    }

    private void setAdapter(RecyclerView inventoryRecyclerView){
        inventoryRecyclerView.setAdapter(new InventoryRecyclerViewAdapter(
                requireContext(), requireActivity(), inventoryRecyclerView, getChildFragmentManager(), loadedInventory));
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MarketActivity)requireActivity()).loadedInventory = loadedInventory;
        ((MarketActivity)requireActivity()).inventoryRecyclerViewSavedState = inventoryRecyclerView.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("inventorySerialized", new Gson().toJson(loadedInventory));
    }
}