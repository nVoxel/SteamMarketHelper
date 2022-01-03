package com.voxeldev.steammarkethelper.ui.inventory;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.transition.Slide;
import androidx.transition.Transition;
import androidx.transition.TransitionManager;

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
    private CircularProgressIndicator inventoryLoader;
    private SwipeRefreshLayout swipeRefreshLayout;
    private int columnsCount;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_inventory, container, false);

        SharedPreferences sharedPreferences = PreferenceManager
                .getDefaultSharedPreferences(requireContext());
        columnsCount = Integer
                .parseInt(sharedPreferences.getString("inventory_columns", "2"));

        inventoryRecyclerView = root.findViewById(R.id.inventory_recyclerview);
        inventoryRecyclerView.setLayoutManager(
                new StaggeredGridLayoutManager(columnsCount, StaggeredGridLayoutManager.VERTICAL));

        inventoryLoader = root.findViewById(R.id.inventory_loader);

        swipeRefreshLayout = root.findViewById(R.id.inventory_swiperefresh);
        swipeRefreshLayout.setOnRefreshListener(this::reloadInventory);

        CircularProgressIndicator inventoryBalanceLoader = root
                .findViewById(R.id.inventory_balance_loader);
        TextView inventoryBalanceTextView = root.findViewById(R.id.inventory_balance_textview);
        root.findViewById(R.id.inventory_balance_cardview).setOnClickListener(v -> {
            inventoryBalanceTextView.setText(getResources().getString(R.string.wallet_balance_not_set));
            inventoryBalanceLoader.setVisibility(View.VISIBLE);
            loadWalletBalance(inventoryBalanceLoader, inventoryBalanceTextView);
        });

        loadWalletBalance(inventoryBalanceLoader, inventoryBalanceTextView);

        FloatingActionButton marketGoTopButton = root.findViewById(R.id.inventory_goTopButton);
        marketGoTopButton.setOnClickListener(v ->
                ((StaggeredGridLayoutManager)inventoryRecyclerView.getLayoutManager())
                        .scrollToPositionWithOffset(0, 0));

        if (savedInstanceState != null) {
            loadedInventory = new Gson().fromJson(savedInstanceState
                            .getString("inventorySerialized", ""),
                    InventoryModel.class);
            setAdapter(inventoryRecyclerView);
            return root;
        }

        MarketActivity marketActivity = (MarketActivity)requireActivity();
        if (marketActivity.loadedInventory != null && marketActivity.inventoryRecyclerViewSavedState != null) {
            loadedInventory = marketActivity.loadedInventory;
            setAdapter(inventoryRecyclerView);
            inventoryRecyclerView.getLayoutManager()
                    .onRestoreInstanceState(marketActivity.inventoryRecyclerViewSavedState);
            return root;
        }

        loadInventory();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull @NotNull View view, @Nullable @org.jetbrains.annotations.Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
    }

    private void loadInventory() {
        new Thread(() -> {
            try{
                InventoryManager inventoryManager = new InventoryManager(new AuthModel(requireContext()));
                loadedInventory = inventoryManager.getInventoryModel(((MarketActivity)requireActivity()).gameId);

                setAdapter(inventoryRecyclerView);
            }
            catch (Exception e) { Log.e(MainActivity.LOG_TAG, e.toString()); }
        }).start();
    }

    public void reloadInventory() {
        inventoryRecyclerView.setVisibility(View.GONE);
        inventoryLoader.setVisibility(View.VISIBLE);
        Parcelable state = inventoryRecyclerView.getLayoutManager().onSaveInstanceState();
        loadInventory();
        inventoryRecyclerView.getLayoutManager().onRestoreInstanceState(state);
        swipeRefreshLayout.setRefreshing(false);
    }

    private void setAdapter(RecyclerView inventoryRecyclerView) {
        new Thread(() -> {
            try {
                InventoryRecyclerViewAdapter adapter = new InventoryRecyclerViewAdapter(
                        requireContext(), requireActivity(), this,
                        inventoryRecyclerView, loadedInventory, columnsCount);

                requireActivity().runOnUiThread(() -> {
                    inventoryRecyclerView.setAdapter(adapter);

                    inventoryLoader.setVisibility(View.GONE);

                    Transition transition = new Slide(Gravity.BOTTOM);
                    transition.setDuration(300);
                    transition.addTarget(inventoryRecyclerView);

                    TransitionManager.beginDelayedTransition(swipeRefreshLayout, transition);

                    inventoryRecyclerView.setVisibility(View.VISIBLE);
                });
            }
            catch (Exception e) {
                Log.e(MainActivity.LOG_TAG, "Failed to set inventory adapter: " + e.getMessage());
            }
        }).start();
    }

    private void loadWalletBalance(CircularProgressIndicator balanceLoader, TextView inventoryBalanceTextView) {
        new Thread(() -> {
            InventoryManager inventoryManager = new InventoryManager(new AuthModel(requireContext()));
            String balance = inventoryManager.getWalletBalance();

            try{
                if (balance == null || balance.equals("")) {
                    requireActivity().runOnUiThread(() -> {
                        balanceLoader.setVisibility(View.GONE);
                        inventoryBalanceTextView.setText(String.format(getResources().getString(R.string.wallet_balance), "null"));
                    });
                    return;
                }

                requireActivity().runOnUiThread(() -> {
                    balanceLoader.setVisibility(View.GONE);
                    inventoryBalanceTextView.setText(String.format(getResources().getString(R.string.wallet_balance), balance));
                });
            }
            catch (Exception ignored) {}
        }).start();
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