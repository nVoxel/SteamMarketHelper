package com.voxeldev.steammarkethelper.models.adapters;

import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.MarketActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.common.RequestManager;
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
    private final int columnsCount;

    public MarketRecyclerViewAdapter(Context context, Activity activity,
                                     FragmentManager fragmentManager, RecyclerView recyclerView,
                                     MarketModel model, int columnsCount) {
        this.context = context;
        this.model = model;
        this.recyclerView = recyclerView;
        this.fragmentManager = fragmentManager;
        this.gameId = ((MarketActivity) activity).gameId;
        this.columnsCount = columnsCount;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private ImageView imageView;
        private TextView nameTextView;
        private final TextView priceTextView;
        private TextView countTextView;

        public ViewHolder(View view, int columnsCount) {
            super(view);

            if (columnsCount == 2) {
                imageView = view.findViewById(R.id.marketrecyclerview_imageview);
                nameTextView = view.findViewById(R.id.marketrecyclerview_nametextview);
                countTextView = view.findViewById(R.id.marketrecyclerview_counttextview);
            }
            priceTextView = view.findViewById(R.id.marketrecyclerview_pricetextview);

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
                .inflate((columnsCount == 2) ? R.layout.item_marketrecyclerview :
                        R.layout.item_marketrecyclerview_small, parent, false);

        view.setOnClickListener(clickView -> {
            if (fragmentManager.getFragments().size() > 0) {
                return;
            }

            try {
                ItemInfoDialog itemInfoDialog = ItemInfoDialog
                        .getInstance(model.results.get(
                                recyclerView.getChildLayoutPosition(clickView)));
                itemInfoDialog.showNow(fragmentManager, "marketItemInfoDialog");
            }
            catch (Exception e) { Log.e(MainActivity.LOG_TAG, e.toString()); }
        });

        view.setOnLongClickListener(clickView -> {
            if (fragmentManager.getFragments().size() > 0) {
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
        return new ViewHolder(view, columnsCount);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        if (columnsCount == 2) {
            ((ViewHolder)holder).getNameTextView().setText(model.results.get(position).name);
            ((ViewHolder)holder).getCountTextView().setText(
                    String.format(context.getResources().getString(R.string.market_count),
                            model.results.get(position).sell_listings));
            Glide.with(context)
                    .load(RequestManager.ICON_URL_PREFIX +
                            model.results.get(position).asset_description.icon_url)
                    .into(((ViewHolder)holder).getImageView());
        }

        ((ViewHolder)holder).getPriceTextView().setText(model.results.get(position).sell_price_text);

        if (columnsCount != 2) {
            int width = (int) TypedValue.applyDimension(
                    TypedValue.COMPLEX_UNIT_DIP, (columnsCount == 3) ? 100 : 65,
                    context.getResources().getDisplayMetrics());

            //noinspection SuspiciousNameCombination
            Glide.with(context)
                    .load(RequestManager.ICON_URL_PREFIX +
                            model.results.get(position).asset_description.icon_url)
                    .into(new CustomTarget<Drawable>(width, width) {
                        @Override
                        public void onResourceReady(@NonNull @NotNull Drawable resource,
                                                    @Nullable @org.jetbrains.annotations.Nullable Transition<? super Drawable> transition) {
                            ((ViewHolder) holder).getPriceTextView()
                                    .setCompoundDrawablesWithIntrinsicBounds(
                                            null, resource, null, null);
                        }

                        @Override
                        public void onLoadCleared(@Nullable @org.jetbrains.annotations.Nullable Drawable placeholder) {
                            ((ViewHolder) holder).getPriceTextView()
                                    .setCompoundDrawablesWithIntrinsicBounds(
                                            null, placeholder, null, null);
                        }
                    });
        }

    }

    @Override
    public int getItemCount() {
        return ((model != null && model.results != null) ? model.results.size() : 0);
    }
}
