package com.voxeldev.steammarkethelper.ui.inventory;

import android.app.Activity;
import android.os.Bundle;
import android.os.Parcelable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.gson.Gson;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.adapters.InventoryRecyclerViewAdapter;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.inventory.InventoryItemModel;
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

        CircularProgressIndicator inventoryBalanceLoader = root.findViewById(R.id.inventory_balance_loader);
        TextView inventoryBalanceTextView = root.findViewById(R.id.inventory_balance_textview);
        root.findViewById(R.id.inventory_balance_cardview).setOnClickListener(v -> {
            inventoryBalanceTextView.setText(getResources().getString(R.string.wallet_balance_not_set));
            inventoryBalanceLoader.setVisibility(View.VISIBLE);
            loadWalletBalance(inventoryBalanceLoader, inventoryBalanceTextView);
        });

        loadWalletBalance(inventoryBalanceLoader, inventoryBalanceTextView);

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

        MainActivity mainActivity = (MainActivity)requireActivity();
        if (mainActivity.loadedInventory != null && mainActivity.inventoryRecyclerViewSavedState != null){
            loadedInventory = mainActivity.loadedInventory;
            setAdapter(inventoryRecyclerView);
            inventoryRecyclerView.getLayoutManager().onRestoreInstanceState(mainActivity.inventoryRecyclerViewSavedState);
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
        Thread inventoryThread = new Thread(() -> {
            try{
                InventoryManager inventoryManager = new InventoryManager(new AuthModel(requireContext()));
                loadedInventory = inventoryManager.getInventoryModel(252490);

                requireActivity().runOnUiThread(() -> {
                    loader.setVisibility(View.GONE);
                    inventoryRecyclerView.setVisibility(View.VISIBLE);
                    setAdapter(inventoryRecyclerView);
                });
            }
            catch (Exception e){ Log.e("SMH", e.toString()); }
        });
        inventoryThread.start();
    }

    private void loadWalletBalance(CircularProgressIndicator balanceLoader, TextView inventoryBalanceTextView){
        Thread balanceThread = new Thread(() -> {
            InventoryManager inventoryManager = new InventoryManager(new AuthModel(requireContext()));
            String balance = inventoryManager.getWalletBalance();

            try{
                if (balance == null || balance.equals("")){
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
            catch (Exception ignored){}
        });
        balanceThread.start();
    }

    private void setAdapter(RecyclerView inventoryRecyclerView){
        inventoryRecyclerView.setAdapter(new InventoryRecyclerViewAdapter(
                requireContext(), requireActivity(), inventoryRecyclerView, getChildFragmentManager(), loadedInventory));
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MainActivity)requireActivity()).loadedInventory = loadedInventory;
        ((MainActivity)requireActivity()).inventoryRecyclerViewSavedState = inventoryRecyclerView.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("inventorySerialized", new Gson().toJson(loadedInventory));
    }
}