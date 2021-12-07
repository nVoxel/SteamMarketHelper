package com.voxeldev.steammarkethelper.ui.listings;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.voxeldev.steammarkethelper.MarketActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.adapters.ListingsRecyclerViewAdapter;
import com.voxeldev.steammarkethelper.models.listings.ListingModel;
import com.voxeldev.steammarkethelper.models.listings.ListingsManager;

import java.util.List;

public class ListingsFragment extends Fragment {

    private List<ListingModel> sellListings;
    private List<ListingModel> buyOrders;
    private RecyclerView sellRecyclerView;
    private RecyclerView buyRecyclerView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_listings, container, false);

        sellRecyclerView = root.findViewById(R.id.listings_sell_recyclerview);
        buyRecyclerView = root.findViewById(R.id.listings_buy_recyclerview);

        sellRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        buyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        if (savedInstanceState != null){
            Gson gson = new Gson();

            sellListings = gson.fromJson(
                    savedInstanceState.getString("sellListingsSerialized"),
                    new TypeToken<List<ListingModel>>(){}.getType());
            buyOrders = gson.fromJson(
                    savedInstanceState.getString("buyOrdersSerialized"),
                    new TypeToken<List<ListingModel>>(){}.getType());

            setAdapters();
            return root;
        }

        MarketActivity marketActivity = (MarketActivity) requireActivity();
        if (marketActivity.loadedSellListings != null &&
                marketActivity.loadedBuyOrders != null &&
                marketActivity.sellRecyclerViewSavedState != null &&
                marketActivity.buyRecyclerViewSavedState != null) {
            sellListings = marketActivity.loadedSellListings;
            buyOrders = marketActivity.loadedBuyOrders;

            setAdapters();

            sellRecyclerView.getLayoutManager().onRestoreInstanceState(
                    marketActivity.sellRecyclerViewSavedState);
            buyRecyclerView.getLayoutManager().onRestoreInstanceState(
                    marketActivity.buyRecyclerViewSavedState
            );

            return root;
        }

        loadRecyclerViews();

        return root;
    }

    private void loadRecyclerViews() {
        new Thread(() -> {
            ListingsManager listingsManager = new ListingsManager(requireContext(),
                    ((MarketActivity)requireActivity()).gameName);

            sellListings = listingsManager.getSellListings();
            buyOrders = listingsManager.getBuyOrders();

            requireActivity().runOnUiThread(this::setAdapters);
        }).start();
    }

    private void setAdapters() {
        sellRecyclerView.setAdapter(new ListingsRecyclerViewAdapter(
                requireContext(),
                sellRecyclerView,
                getChildFragmentManager(),
                sellListings));

        buyRecyclerView.setAdapter(new ListingsRecyclerViewAdapter(
                requireContext(),
                buyRecyclerView,
                getChildFragmentManager(),
                buyOrders));
    }

    @Override
    public void onPause() {
        super.onPause();
        MarketActivity marketActivity = (MarketActivity) requireActivity();
        marketActivity.loadedSellListings = sellListings;
        marketActivity.loadedBuyOrders = buyOrders;
        marketActivity.sellRecyclerViewSavedState = sellRecyclerView.getLayoutManager().onSaveInstanceState();
        marketActivity.buyRecyclerViewSavedState = buyRecyclerView.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        Gson gson = new Gson();
        outState.putString("sellListingsSerialized", gson.toJson(sellListings));
        outState.putString("buyOrdersSerialized", gson.toJson(buyOrders));
    }
}
