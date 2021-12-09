package com.voxeldev.steammarkethelper.models.adapters;

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
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.listings.ListingModel;
import com.voxeldev.steammarkethelper.ui.dialogs.ItemInfoDialog;

import java.util.List;

public class ListingsRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final RecyclerView recyclerView;
    private final FragmentManager fragmentManager;
    private final List<ListingModel> listings;

    public ListingsRecyclerViewAdapter(Context context, RecyclerView recyclerView, FragmentManager fragmentManager, List<ListingModel> listings) {
        this.context = context;
        this.recyclerView = recyclerView;
        this.fragmentManager = fragmentManager;
        this.listings = listings;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView iconImageView;
        private final TextView nameTextView;
        private final TextView additionalTextView;
        private final TextView priceTextView;

        public ViewHolder(View view) {
            super(view);

            iconImageView = view.findViewById(R.id.listingsrecyclerview_iconimageview);
            nameTextView = view.findViewById(R.id.listingsrecyclerview_nametextview);
            additionalTextView = view.findViewById(R.id.listingsrecyclerview_additionaltextview);
            priceTextView = view.findViewById(R.id.listingsrecyclerview_pricetextview);
        }

        public ImageView getIconImageView() {
            return iconImageView;
        }

        public TextView getNameTextView() {
            return nameTextView;
        }

        public TextView getAdditionalTextView() {
            return additionalTextView;
        }

        public TextView getPriceTextView() {
            return priceTextView;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_listingsrecyclerview, parent, false);

        view.setOnClickListener(clickView -> {
            if (fragmentManager.getFragments().size() < 1){
                try {
                    ItemInfoDialog itemInfoDialog = ItemInfoDialog.newListingsInstance(
                            listings.get(recyclerView.getChildLayoutPosition(clickView)));
                    itemInfoDialog.showNow(fragmentManager, "listingsItemInfoDialog");
                }
                catch (Exception e){ Log.e(MainActivity.LOG_TAG, e.toString()); }
            }
        });

        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder)holder).getNameTextView().setText(listings.get(position).name);
        ((ViewHolder)holder).getAdditionalTextView().setText(listings.get(position).additional);
        ((ViewHolder)holder).getPriceTextView().setText(listings.get(position).price);
        Glide.with(context)
                .load(listings.get(position).iconUrl)
                .into(((ViewHolder)holder).getIconImageView());
    }

    @Override
    public int getItemCount() {
        if (listings == null) {
            return 0;
        }

        return listings.size();
    }
}
