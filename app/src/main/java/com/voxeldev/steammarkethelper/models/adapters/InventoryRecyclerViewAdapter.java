package com.voxeldev.steammarkethelper.models.adapters;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.chip.Chip;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.inventory.InventoryAssetModel;
import com.voxeldev.steammarkethelper.models.inventory.InventoryItemModel;
import com.voxeldev.steammarkethelper.models.inventory.InventoryModel;
import com.voxeldev.steammarkethelper.models.inventory.InventoryOwnerDescription;
import com.voxeldev.steammarkethelper.ui.dialogs.ItemInfoDialog;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.TimeUnit;

public class InventoryRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final Activity activity;
    private final RecyclerView recyclerView;
    private final FragmentManager fragmentManager;
    private final InventoryModel model;

    public InventoryRecyclerViewAdapter(Context context, Activity activity,
                                        RecyclerView recyclerView, FragmentManager fragmentManager,
                                        InventoryModel model){
        this.context = context;
        this.activity = activity;
        this.recyclerView = recyclerView;
        this.fragmentManager = fragmentManager;
        this.model = model;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView nameTextView;
        private final Chip tradeBanChip;

        public ViewHolder(View view) {
            super(view);

            nameTextView = view.findViewById(R.id.inventoryrecyclerview_nametextview);
            tradeBanChip = view.findViewById(R.id.inventoryrecyclerview_tradebanchip);
        }

        public TextView getNameTextView() {
            return nameTextView;
        }

        public Chip getTradeBanChip() {
            return tradeBanChip;
        }
    }

    @NonNull
    @NotNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull @NotNull ViewGroup parent,
                                                      int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_inventoryrecyclerview, parent, false);
        view.setOnClickListener(clickView -> {
            int position = recyclerView.getChildLayoutPosition(clickView);

            if (fragmentManager.getFragments().size() < 1){
                try {
                    InventoryItemModel inventoryItem = model.descriptions.stream()
                            .filter(i -> i.classid.contentEquals(model.assets.get(position).classid))
                            .findFirst().orElse(null);
                    if (inventoryItem == null){
                        Log.e(MainActivity.LOG_TAG, "Cant display info dialog, item not found");
                        return;
                    }
                    ItemInfoDialog itemInfoDialog = ItemInfoDialog.newInventoryInstance(inventoryItem);
                    itemInfoDialog.showNow(fragmentManager, "inventoryItemInfoDialog");
                }
                catch (Exception e){ Log.e(MainActivity.LOG_TAG, e.toString()); }
            }
        });
        return new ViewHolder(view);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        InventoryAssetModel inventoryAsset = model.assets.get(position);
        InventoryItemModel inventoryItem = model.descriptions.stream()
                .filter(i -> i.classid.contentEquals(inventoryAsset.classid))
                .findFirst().orElse(null);

        if (inventoryItem == null){
            Log.e(MainActivity.LOG_TAG, "Cant find item to display in models");
            return;
        }

        int amount = Integer.parseInt(inventoryAsset.amount);
        ((ViewHolder)holder).getNameTextView().setText(
                inventoryItem.name + ((amount > 1) ? " x " + amount : "")
        );

        Integer width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 100,
                context.getResources().getDisplayMetrics());

        //noinspection SuspiciousNameCombination
        Glide.with(context)
                .load("https://community.akamai.steamstatic.com/economy/image/" + inventoryItem.icon_url)
                .into(new CustomTarget<Drawable>(width, width) {
                    @Override
                    public void onResourceReady(@NonNull @NotNull Drawable resource,
                                                @Nullable @org.jetbrains.annotations.Nullable Transition<? super Drawable> transition) {
                        ((ViewHolder) holder).getNameTextView()
                                .setCompoundDrawablesWithIntrinsicBounds(
                                        null, resource, null, null);
                    }

                    @Override
                    public void onLoadCleared(@Nullable @org.jetbrains.annotations.Nullable Drawable placeholder) {
                        ((ViewHolder) holder).getNameTextView()
                                .setCompoundDrawablesWithIntrinsicBounds(
                                        null, placeholder, null, null);
                    }
                });

        ((ViewHolder) holder).getTradeBanChip().setVisibility(View.GONE); //TODO: hack!
        if (inventoryItem.owner_descriptions != null && inventoryItem.owner_descriptions.size() > 0){
            for (InventoryOwnerDescription description : inventoryItem.owner_descriptions){
                if (description.value == null || !description.value.contains("date")){continue;}

                try {
                    String chipText = getTimeDifference(
                            description.value.substring(
                                    description.value.indexOf("[date]") + 6,
                                    description.value.indexOf("[/date]")
                            )
                    );

                    activity.runOnUiThread(() -> {
                        ((ViewHolder) holder).getTradeBanChip().setText(chipText);
                        ((ViewHolder) holder).getTradeBanChip().setVisibility(View.VISIBLE);
                    });
                }
                catch (Exception ignored){}

                return;
            }
        }
    }

    @Override
    public int getItemCount() {
        return ((model != null && model.assets != null) ? model.assets.size() : 0);
    }

    public String getTimeDifference(String date) {
        try{
            long difference = Math.abs(System.currentTimeMillis()/1000 - Long.parseLong(date));
            if (difference > 86400){
                return String.format(context.getResources().getString(R.string.tradeban_days),
                        TimeUnit.DAYS.convert(difference, TimeUnit.SECONDS));
            }
            else{
                return String.format(context.getResources().getString(R.string.tradeban_hours),
                        TimeUnit.HOURS.convert(difference, TimeUnit.SECONDS));
            }
        }
        catch (Exception e){
            Log.e(MainActivity.LOG_TAG, e.toString());
        }
        return "?";
    }

    public int getItemAmount(InventoryItemModel item){
        int amount = 0;
        try{
            for (InventoryAssetModel asset : model.assets){
                if (asset.classid.contentEquals(item.classid)){
                    amount += Integer.parseInt(asset.amount);
                }
            }
        }
        catch (Exception e){ Log.e(MainActivity.LOG_TAG, e.toString()); }
        return amount;
    }
}
