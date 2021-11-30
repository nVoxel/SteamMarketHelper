package com.voxeldev.steammarkethelper.models.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.textview.MaterialTextView;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.MarketActivity;
import com.voxeldev.steammarkethelper.R;

import org.jetbrains.annotations.NotNull;
import org.jsoup.select.Elements;

public class GamesRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final Activity activity;
    private final Elements games;
    private final RecyclerView recyclerView;

    public GamesRecyclerViewAdapter(Activity activity, Elements games, RecyclerView recyclerView){
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.games = games;
        this.recyclerView = recyclerView;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialTextView gameTextView;

        public ViewHolder(View view) {
            super(view);

            gameTextView = view.findViewById(R.id.gamesrecyclerview_gametextview);
        }

        public TextView getGameTextView() {
            return gameTextView;
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gamesrecyclerview, parent, false);
        view.setOnClickListener(clickView -> {
            try{
                String url = games.get(recyclerView.getChildLayoutPosition(clickView))
                        .attr("href");
                String keyString = "appid=";

                int gameId = Integer.parseInt(
                        url.substring(url.indexOf(keyString) + keyString.length()));

                Intent intent = new Intent(context, MarketActivity.class)
                        .putExtra("gameId", gameId)
                        .setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
                activity.overridePendingTransition(R.anim.left_in, R.anim.right_out);
            }
            catch (Exception e){
                Log.e(MainActivity.LOG_TAG, "Failed to start MarketActivity: " + e.getMessage());
            }
        });
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        ((ViewHolder)holder).getGameTextView().setText(games.get(position).text());

        try{
            Glide.with(context)
                    .load(games.get(position).selectFirst("span.game_button_game_icon img")
                            .attr("src"))
                    .into(new CustomTarget<Drawable>(100,100) {

                        @Override
                        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition)
                        {
                            ((ViewHolder) holder).getGameTextView().setCompoundDrawablesWithIntrinsicBounds(resource, null, null, null);
                        }

                        @Override
                        public void onLoadCleared(@Nullable Drawable placeholder)
                        {
                            ((ViewHolder) holder).getGameTextView().setCompoundDrawablesWithIntrinsicBounds(placeholder, null, null, null);
                        }
                    });
        }
        catch (Exception e){
            Log.e(MainActivity.LOG_TAG, "Failed to load game icon: " + games.get(position).text());
        }
    }

    @Override
    public int getItemCount() {
        return ((games != null) ? games.size() : 0);
    }
}
