package com.voxeldev.steammarkethelper.models.adapters;

import android.app.Activity;
import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.MarketActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.market.MarketItemModel;
import com.voxeldev.steammarkethelper.models.market.MarketModel;
import com.voxeldev.steammarkethelper.ui.dialogs.ItemBuyDialog;
import com.voxeldev.steammarkethelper.ui.dialogs.ItemInfoDialog;

import org.jetbrains.annotations.NotNull;

public class MarketRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final MarketModel model;
    private final RecyclerView recyclerView;
    private final FragmentManager fragmentManager;
    private final int gameId;

    public MarketRecyclerViewAdapter(Context context, Activity activity,
                                     FragmentManager fragmentManager, RecyclerView recyclerView,
                                     MarketModel model){
        this.context = context;
        this.model = model;
        this.recyclerView = recyclerView;
        this.fragmentManager = fragmentManager;
        this.gameId = ((MarketActivity) activity).gameId;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageView;
        private final TextView nameTextView;
        private final TextView priceTextView;
        private final TextView countTextView;

        public ViewHolder(View view) {
            super(view);

            imageView = view.findViewById(R.id.marketrecyclerview_imageview);
            nameTextView = view.findViewById(R.id.marketrecyclerview_nametextview);
            priceTextView = view.findViewById(R.id.marketrecyclerview_pricetextview);
            countTextView = view.findViewById(R.id.marketrecyclerview_counttextview);
        }

        public ImageView getImageView() {
            return imageView;
        }

        public TextView getNameTextView() {
            return nameTextView;
        }

        public TextView getPriceTextView() {
            return priceTextView;
        }

        public TextView getCountTextView() {
            return countTextView;
        }
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_marketrecyclerview, parent, false);

        view.setOnClickListener(clickView -> {
            if (fragmentManager.getFragments().size() > 0){
                return;
            }

            try {
                ItemInfoDialog itemInfoDialog = ItemInfoDialog
                        .getInstance(model.results.get(
                                recyclerView.getChildLayoutPosition(clickView)));
                itemInfoDialog.showNow(fragmentManager, "marketItemInfoDialog");
            }
            catch (Exception e){ Log.e(MainActivity.LOG_TAG, e.toString()); }
        });

        view.setOnLongClickListener(clickView -> {
            if (fragmentManager.getFragments().size() > 0){
                return true;
            }

            try {
                MarketItemModel marketItem = model.results.get(
                        recyclerView.getChildLayoutPosition(view));

                ItemBuyDialog itemBuyDialog = ItemBuyDialog.getInstance(gameId, marketItem.name,
                        marketItem.name, marketItem.asset_description.icon_url);
                itemBuyDialog.showNow(fragmentManager, "marketItemBuyDialog");
            }
            catch (Exception e) {
                Log.e(MainActivity.LOG_TAG, "Failed to show ItemBuyDialog: " + e.getMessage());
            }

            return true;
        });
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder)holder).getNameTextView().setText(model.results.get(position).name);
        ((ViewHolder)holder).getPriceTextView().setText(model.results.get(position).sell_price_text);
        ((ViewHolder)holder).getCountTextView().setText(
                String.format(context.getResources().getString(R.string.market_count), model.results.get(position).sell_listings));
        Glide.with(context)
                .load(ItemInfoDialog.iconUrlPrefix +
                        model.results.get(position).asset_description.icon_url)
                .into(((ViewHolder)holder).getImageView());
    }

    @Override
    public int getItemCount() {
        return ((model != null && model.results != null) ? model.results.size() : 0);
    }
}
