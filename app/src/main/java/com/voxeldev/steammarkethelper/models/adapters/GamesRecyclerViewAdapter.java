package com.voxeldev.steammarkethelper.models.adapters;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

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
import com.voxeldev.steammarkethelper.models.common.FavoriteGamesModel;

import org.jetbrains.annotations.NotNull;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.HashMap;
import java.util.Map;

public class GamesRecyclerViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private final Context context;
    private final Activity activity;
    private Elements games;
    private FavoriteGamesModel favoriteGamesModel;
    private Map<String, Integer> positionsBeforeMove;
    private final RecyclerView recyclerView;

    public GamesRecyclerViewAdapter(Activity activity, Elements games, RecyclerView recyclerView){
        this.context = activity.getApplicationContext();
        this.activity = activity;
        this.recyclerView = recyclerView;

        this.games = games;

        if (games == null || games.size() < 1){
            return;
        }

        positionsBeforeMove = new HashMap<>();
        favoriteGamesModel = new FavoriteGamesModel(context);

        int beginBoundary = 0;
        int i = games.size() - 1;

        while (i >= beginBoundary){
            String name = games.get(i).text();
            if (favoriteGamesModel.isInFavorites(name)){
                positionsBeforeMove.put(name, i);
                moveElementsItem(i, 0);
                beginBoundary++;
                continue;
            }
            i--;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final MaterialTextView gameTextView;
        private final ImageView starImageView;

        public ViewHolder(View view) {
            super(view);

            gameTextView = view.findViewById(R.id.gamesrecyclerview_gametextview);
            starImageView = view.findViewById(R.id.gamesrecyclerview_starimageview);
        }

        public TextView getGameTextView() {
            return gameTextView;
        }

        public ImageView getStarImageView() {
            return starImageView;
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
        view.setOnLongClickListener(clickView -> {
            changeFavorite(clickView);
            return true;
        });
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull @NotNull RecyclerView.ViewHolder holder, int position) {
        String name = games.get(position).text();
        ((ViewHolder)holder).getGameTextView().setText(name);

        if (favoriteGamesModel.isInFavorites(name)){
            ((ViewHolder) holder).getStarImageView().setVisibility(View.VISIBLE);
        }
        else{
            ((ViewHolder) holder).getStarImageView().setVisibility(View.GONE);
        }

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

    private void changeFavorite(View view) {
        int itemPos = recyclerView.getChildLayoutPosition(view);
        String name = games.get(itemPos).text();

        if (favoriteGamesModel.isInFavorites(name)){
            favoriteGamesModel.removeFromFavorites(name);

            if (positionsBeforeMove.containsKey(name)){
                int toPos = positionsBeforeMove.get(name);
                moveElementsItem(itemPos, toPos);
                notifyItemMoved(itemPos, toPos);
            }

            view.findViewById(R.id.gamesrecyclerview_starimageview).setVisibility(View.GONE);

            Toast.makeText(context,
                    String.format(context.getString(R.string.remove_favorites), name),
                    Toast.LENGTH_SHORT).show();
        }
        else{
            favoriteGamesModel.addToFavorites(name);

            positionsBeforeMove.put(name, itemPos);
            moveElementsItem(itemPos, 0);
            notifyItemMoved(itemPos, 0);

            view.findViewById(R.id.gamesrecyclerview_starimageview).setVisibility(View.VISIBLE);

            Toast.makeText(context,
                    String.format(context.getString(R.string.add_favorites), name),
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void moveElementsItem(int fromPos, int toPos){
        Element itemToMove = games.get(fromPos);
        games.remove(itemToMove);
        games.add(toPos, itemToMove);
    }
}
