package com.voxeldev.steammarkethelper.ui.market;

import android.os.Bundle;
import android.os.Parcelable;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;
import com.google.gson.Gson;
import com.voxeldev.customswiperefresh.CustomSwipeRefresh;
import com.voxeldev.customswiperefresh.CustomSwipeRefreshDirection;
import com.voxeldev.steammarkethelper.MarketActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.adapters.MarketRecyclerViewAdapter;
import com.voxeldev.steammarkethelper.models.market.MarketManager;
import com.voxeldev.steammarkethelper.models.market.MarketModel;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;

public class MarketFragment extends Fragment {

    private MarketModel loadedMarket;
    private RecyclerView marketRecyclerView;
    private CustomSwipeRefresh customSwipeRefresh;
    private boolean marketLoading;

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_market, container, false);
        setHasOptionsMenu(true);



        customSwipeRefresh = root.findViewById(R.id.market_swiperefresh);
        customSwipeRefresh.setOnRefreshListener(direction -> {
            loadedMarket = null;
            loadMarketToRecyclerView();
        });

        marketRecyclerView = root.findViewById(R.id.market_recyclerview);

        FloatingActionButton marketGoTopButton = root.findViewById(R.id.market_goTopButton);
        marketGoTopButton.setOnClickListener(v -> ((StaggeredGridLayoutManager)marketRecyclerView.getLayoutManager()).scrollToPositionWithOffset(0, 0));

        marketRecyclerView.setLayoutManager(new StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL));
        marketRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull @NotNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!recyclerView.canScrollVertically(1)){
                    if (loadedMarket.start >= loadedMarket.total_count){
                        Snackbar.make(root.findViewById(R.id.market_main), "Search completed", Snackbar.LENGTH_LONG).show();
                        return;
                    }
                    loadMarketToRecyclerView();
                }
            }

            @Override
            public void onScrollStateChanged(@NonNull @NotNull RecyclerView recyclerView, int newState) {
                super.onScrollStateChanged(recyclerView, newState);
            }
        });

        if (savedInstanceState != null){
            loadedMarket = new Gson().fromJson(savedInstanceState.getString("marketSerialized"), MarketModel.class);
            replaceAdapter();
            return root;
        }

        MarketActivity marketActivity = (MarketActivity)requireActivity();
        if (marketActivity.loadedMarket != null && marketActivity.marketRecyclerViewSavedState != null){
            loadedMarket = marketActivity.loadedMarket;
            replaceAdapter();
            marketRecyclerView.getLayoutManager().onRestoreInstanceState(marketActivity.marketRecyclerViewSavedState);
            return root;
        }

        loadMarketToRecyclerView();
        return root;
    }

    private void loadMarketToRecyclerView(){
        if (marketLoading) { return; }
        marketLoading = true;

        customSwipeRefresh.setRefreshing(true, CustomSwipeRefreshDirection.BOTTOM);

        new Thread(() -> {
            int gameId = ((MarketActivity)requireActivity()).gameId;
            MarketManager marketManager = new MarketManager(requireContext(), gameId);
            MarketModel model = marketManager.getMarketModel((loadedMarket == null) ? 0 : loadedMarket.start, 20, gameId, (loadedMarket == null || loadedMarket.query == null) ? "" : loadedMarket.query);
            if (model == null || model.results == null || model.results.size() == 0) { return; }

            if (loadedMarket == null){
                loadedMarket = model;
            }
            else{
                loadedMarket.total_count = model.total_count;
                loadedMarket.results.addAll(model.results);
            }
            loadedMarket.start += 20;

            try{
                requireActivity().runOnUiThread(() -> {
                    replaceAdapter();
                    customSwipeRefresh.setRefreshing(false, CustomSwipeRefreshDirection.TOP);
                });
            }
            catch (Exception ignored){}

            marketLoading = false;
        }).start();
    }

    private void replaceAdapter(){
        Parcelable state = marketRecyclerView.getLayoutManager().onSaveInstanceState();
        MarketRecyclerViewAdapter adapter = new MarketRecyclerViewAdapter(requireContext(), loadedMarket, marketRecyclerView, getChildFragmentManager());
        marketRecyclerView.setAdapter(adapter);
        marketRecyclerView.getLayoutManager().onRestoreInstanceState(state);
    }

    private void updateMarketQuery(String query){
        loadedMarket.query = query;
        loadedMarket.start = 0;
        loadedMarket.results = new ArrayList<>();
        loadMarketToRecyclerView();
    }

    @Override
    public void onCreateOptionsMenu(@NonNull @NotNull Menu menu, @NonNull @NotNull MenuInflater inflater) {
        super.onCreateOptionsMenu(menu, inflater);
        inflater.inflate(R.menu.options_menu, menu);

        SearchView searchView = (SearchView) menu.findItem(R.id.search).getActionView();
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                updateMarketQuery(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                return false;
            }
        });
        searchView.setOnCloseListener(() -> {
            updateMarketQuery(null);
            return false;
        });

        if (loadedMarket != null && loadedMarket.query != null){
            searchView.setQuery(loadedMarket.query, false);
            searchView.setIconified(false);
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        ((MarketActivity)requireActivity()).loadedMarket = loadedMarket;
        ((MarketActivity)requireActivity()).marketRecyclerViewSavedState = marketRecyclerView.getLayoutManager().onSaveInstanceState();
    }

    @Override
    public void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("marketSerialized", new Gson().toJson(loadedMarket));
    }
}