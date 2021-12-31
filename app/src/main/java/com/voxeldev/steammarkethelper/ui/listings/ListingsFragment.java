package com.voxeldev.steammarkethelper.ui.listings;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Canvas;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.MarketActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.adapters.ListingsRecyclerViewAdapter;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.common.RequestManager;
import com.voxeldev.steammarkethelper.models.listings.ListingModel;
import com.voxeldev.steammarkethelper.models.listings.ListingsManager;
import com.voxeldev.steammarkethelper.ui.dialogs.MarketActionDialog;

import java.util.List;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;
import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class ListingsFragment extends Fragment {

    private List<ListingModel> sellListings;
    private List<ListingModel> buyOrders;
    private RecyclerView sellRecyclerView;
    private RecyclerView buyRecyclerView;
    private TextView sellTextView;
    private TextView buyTextView;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_listings, container, false);

        sellRecyclerView = root.findViewById(R.id.listings_sell_recyclerview);
        buyRecyclerView = root.findViewById(R.id.listings_buy_recyclerview);

        sellRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        buyRecyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        sellTextView = root.findViewById(R.id.listings_sell_textview);
        buyTextView = root.findViewById(R.id.listings_buy_textview);

        if (savedInstanceState != null){
            Gson gson = new Gson();

            sellListings = gson.fromJson(
                    savedInstanceState.getString("sellListingsSerialized"),
                    new TypeToken<List<ListingModel>>(){}.getType());
            buyOrders = gson.fromJson(
                    savedInstanceState.getString("buyOrdersSerialized"),
                    new TypeToken<List<ListingModel>>(){}.getType());

            setViews();
            return root;
        }

        MarketActivity marketActivity = (MarketActivity) requireActivity();
        if (marketActivity.loadedSellListings != null &&
                marketActivity.loadedBuyOrders != null &&
                marketActivity.sellRecyclerViewSavedState != null &&
                marketActivity.buyRecyclerViewSavedState != null) {
            sellListings = marketActivity.loadedSellListings;
            buyOrders = marketActivity.loadedBuyOrders;

            setViews();

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
            MarketActivity marketActivity = (MarketActivity) requireActivity();

            ListingsManager listingsManager = new ListingsManager(requireContext(),
                    marketActivity.gameName);

            sellListings = listingsManager.getSellListings();
            buyOrders = listingsManager.getBuyOrders();

            marketActivity.runOnUiThread(this::setViews);
        }).start();
    }

    private void setViews() {
        ListingsRecyclerViewAdapter sellListingsAdapter = new ListingsRecyclerViewAdapter(
                requireContext(),
                sellRecyclerView,
                getChildFragmentManager(),
                sellListings);
        ListingsRecyclerViewAdapter buyOrdersAdapter = new ListingsRecyclerViewAdapter(
                requireContext(),
                buyRecyclerView,
                getChildFragmentManager(),
                buyOrders);

        sellRecyclerView.setAdapter(sellListingsAdapter);
        buyRecyclerView.setAdapter(buyOrdersAdapter);

        ListingsRecyclerCallback sellCallback = new ListingsRecyclerCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                requireActivity(), true, sellListingsAdapter, sellTextView);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(sellCallback);
        itemTouchHelper.attachToRecyclerView(sellRecyclerView);
        ListingsRecyclerCallback buyCallback = new ListingsRecyclerCallback(0,
                ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT,
                requireActivity(), false, buyOrdersAdapter, buyTextView);
        itemTouchHelper = new ItemTouchHelper(buyCallback);
        itemTouchHelper.attachToRecyclerView(buyRecyclerView);

        sellTextView.setText(String.format(getString(R.string.my_sell_listings_placeholder),
                (sellListings == null) ? 0 : sellListings.size()));
        buyTextView.setText(String.format(getString(R.string.my_buy_orders_placeholder),
                (buyOrders == null) ? 0 : buyOrders.size()));
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

class ListingsRecyclerCallback extends ItemTouchHelper.SimpleCallback {

    private final Activity activity;
    private final boolean isForSell;
    private final ListingsRecyclerViewAdapter adapter;
    private TextView countTextView;

    public ListingsRecyclerCallback(int dragDirs, int swipeDirs,
                                    Activity activity, boolean isForSell,
                                    ListingsRecyclerViewAdapter adapter, TextView countTextView) {
        super(dragDirs, swipeDirs);
        this.activity = activity;
        this.isForSell = isForSell;
        this.adapter = adapter;
        this.countTextView = countTextView;
    }

    @Override
    public boolean onMove(@NonNull RecyclerView recyclerView,
                          @NonNull RecyclerView.ViewHolder viewHolder,
                          @NonNull RecyclerView.ViewHolder target) {
        return false;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
        try {
            RequestManager requestManager = new RequestManager(
                    new AuthModel(activity.getApplicationContext()));

            new AlertDialog.Builder(activity)
                    .setTitle(adapter.getListings().get(viewHolder.getLayoutPosition()).name)
                    .setMessage(R.string.listings_remove_confirmation)
                    .setPositiveButton(R.string.yes, (dialog, which) ->
                            processRemove(requestManager, viewHolder))
                    .setNegativeButton(R.string.no, (dialog, which) -> {
                        Log.d(MainActivity.LOG_TAG, "User cancelled listing removal");
                        adapter.notifyDataSetChanged();
                    })
                    .create()
                    .show();
        }
        catch (Exception e) {
            Log.e(MainActivity.LOG_TAG, "Failed to process onSwiped in listings");
        }
    }

    @Override
    public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
        String swipeLabel = activity.getString(isForSell ?
                R.string.sell_remove : R.string.buy_remove);
        int swipeLabelColor = activity.getColor(R.color.white);

        new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                .addBackgroundColor(activity.getColor(R.color.ChipColor))
                .addActionIcon(R.drawable.ic_remove)
                .addSwipeLeftLabel(swipeLabel)
                .addSwipeRightLabel(swipeLabel)
                .setSwipeLeftLabelColor(swipeLabelColor)
                .setSwipeRightLabelColor(swipeLabelColor)
                .create()
                .decorate();

        super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void processRemove(RequestManager requestManager, RecyclerView.ViewHolder viewHolder) {
        try {
            if (isForSell) {
                Request removeSellRequest = buildRemoveSellRequest(
                        requestManager.getAuthModel().loadCookie(),
                        adapter.getListings().get(viewHolder.getLayoutPosition()).id);
                new Thread(() -> {
                    Response response = null;
                    try {
                        response = requestManager.getClient().newCall(removeSellRequest).execute();
                    } catch (Exception e) {
                        Log.e(MainActivity.LOG_TAG, "Failed to execute removeSellRequest");
                    }

                    if (response != null && response.code() == 200) {
                        activity.runOnUiThread(() -> {
                            Toast.makeText(activity.getApplicationContext(),
                                    R.string.sell_remove_successful, Toast.LENGTH_LONG).show();
                            adapter.removeListing(viewHolder.getLayoutPosition());
                            countTextView.setText(String.format(
                                    activity.getString(R.string.my_sell_listings_placeholder),
                                    (adapter.getListings() == null) ? 0 :
                                            adapter.getListings().size()));
                        });
                        return;
                    }

                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity.getApplicationContext(),
                                R.string.sell_remove_failed, Toast.LENGTH_LONG).show();
                        adapter.notifyDataSetChanged();
                    });
                }).start();
                return;
            }

            Request removeBuyRequest = buildRemoveBuyRequest(
                    requestManager.getAuthModel().loadCookie(),
                    adapter.getListings().get(viewHolder.getLayoutPosition()).id);
            new Thread(() -> {
                Response response = null;
                try {
                    response = requestManager.getClient().newCall(removeBuyRequest).execute();
                } catch (Exception e) {
                    Log.e(MainActivity.LOG_TAG, "Failed to execute removeBuyRequest");
                }

                if (response != null && response.code() == 200) {
                    activity.runOnUiThread(() -> {
                        Toast.makeText(activity.getApplicationContext(),
                                R.string.buy_remove_successful, Toast.LENGTH_LONG).show();
                        adapter.removeListing(viewHolder.getLayoutPosition());
                        countTextView.setText(String.format(
                                activity.getString(R.string.my_buy_orders_placeholder),
                                (adapter.getListings() == null) ? 0 :
                                        adapter.getListings().size()));
                    });
                    return;
                }

                activity.runOnUiThread(() -> {
                    Toast.makeText(activity.getApplicationContext(),
                            R.string.buy_remove_failed, Toast.LENGTH_LONG).show();
                    adapter.notifyDataSetChanged();
                });
            }).start();
        }
        catch (Exception e) {
            Log.e(MainActivity.LOG_TAG, "Failed to processRemove in listings RecyclerView");
        }
    }

    private Request buildRemoveSellRequest(String cookie, String listingId) {
        return new Request.Builder()
                .url("https://steamcommunity.com/market/removelisting/" + listingId)
                .addHeader("User-Agent", RequestManager.defaultUserAgent)
                .addHeader("Cookie", cookie)
                .addHeader("Referer", "https://steamcommunity.com/market/")
                .post(new FormBody.Builder()
                        .add("sessionid",
                                MarketActionDialog.getSessionId(cookie))
                        .build())
                .build();
    }

    private Request buildRemoveBuyRequest(String cookie, String orderId) {
        return new Request.Builder()
                .url("https://steamcommunity.com/market/cancelbuyorder")
                .addHeader("User-Agent", RequestManager.defaultUserAgent)
                .addHeader("Cookie", cookie)
                .addHeader("Referer", "https://steamcommunity.com/market/")
                .post(new FormBody.Builder()
                        .add("sessionid",
                                MarketActionDialog.getSessionId(cookie))
                        .add("buy_orderid", orderId)
                        .build())
                .build();
    }
}