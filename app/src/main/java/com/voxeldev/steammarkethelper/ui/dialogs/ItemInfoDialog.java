package com.voxeldev.steammarkethelper.ui.dialogs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.gson.Gson;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.MarketActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.common.ActionModel;
import com.voxeldev.steammarkethelper.models.inventory.InventoryItemModel;
import com.voxeldev.steammarkethelper.models.listings.ListingModel;
import com.voxeldev.steammarkethelper.models.market.MarketItemCommodityModel;
import com.voxeldev.steammarkethelper.models.market.MarketItemModel;
import com.voxeldev.steammarkethelper.models.market.MarketItemPriceHistory;
import com.voxeldev.steammarkethelper.models.market.MarketManager;
import com.voxeldev.steammarkethelper.models.market.MarketOrderModel;
import com.voxeldev.steammarkethelper.ui.misc.ChartMarkerView;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class ItemInfoDialog extends BottomSheetDialogFragment {

    private MarketItemCommodityModel model;
    private String iconUrl;
    private List<ActionModel> actions;
    private String workshopLink;
    private String name;
    public static final String iconUrlPrefix = "https://community.akamai.steamstatic.com/economy/image/";

    public static ItemInfoDialog getInstance(InventoryItemModel inventoryItem) {
        Bundle args = new Bundle();
        args.putInt("type", 0);
        args.putString("item", new Gson().toJson(inventoryItem));

        ItemInfoDialog itemInfoDialog = new ItemInfoDialog();
        itemInfoDialog.setArguments(args);

        return itemInfoDialog;
    }

    public static ItemInfoDialog getInstance(MarketItemModel marketItem) {
        Bundle args = new Bundle();
        args.putInt("type", 1);
        args.putString("item", new Gson().toJson(marketItem));

        ItemInfoDialog itemInfoDialog = new ItemInfoDialog();
        itemInfoDialog.setArguments(args);

        return itemInfoDialog;
    }

    public static ItemInfoDialog getInstance(ListingModel listingModel) {
        Bundle args = new Bundle();
        args.putInt("type", 2);
        args.putString("item", new Gson().toJson(listingModel));

        ItemInfoDialog itemInfoDialog = new ItemInfoDialog();
        itemInfoDialog.setArguments(args);

        return itemInfoDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull @NotNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_iteminfo, container, false);
        Bundle args = getArguments();

        MaterialButton workshopButton = root.findViewById(R.id.iteminfo_workshopbutton);

        if (savedInstanceState != null){
            String modelSerialized = savedInstanceState.getString("commodity");
            iconUrl = savedInstanceState.getString("iconUrl");
            name = savedInstanceState.getString("name");
            workshopLink = savedInstanceState.getString("workshopLink");

            if (!modelSerialized.contentEquals("null") && !iconUrl.contentEquals("null") && !name.contentEquals("null")){
                model = new Gson().fromJson(modelSerialized, MarketItemCommodityModel.class);
                Glide.with(requireContext())
                        .load(iconUrl)
                        .into((ImageView)root.findViewById(R.id.iteminfo_imageview));
                ((TextView)root.findViewById(R.id.iteminfo_itemtitle)).setText(name);
                if (workshopLink != null){ setWorkshopButtonLink(workshopButton); }
                setCommodity(root);
                return root;
            }
        }

        switch (args.getInt("type", 0)){
            case 0:
                InventoryItemModel inventoryItem = new Gson().fromJson(
                        getArguments().getString("item"),
                        InventoryItemModel.class);
                iconUrl = iconUrlPrefix + inventoryItem.icon_url;
                name = inventoryItem.name;
                actions = inventoryItem.actions;
                break;
            case 1:
                MarketItemModel marketItem = new Gson().fromJson(
                        getArguments().getString("item"),
                        MarketItemModel.class);
                iconUrl = iconUrlPrefix + marketItem.asset_description.icon_url;
                name = marketItem.name;
                actions = marketItem.asset_description.actions;
                break;
            case 2:
                ListingModel listingModel = new Gson().fromJson(
                        getArguments().getString("item"),
                        ListingModel.class);
                iconUrl = listingModel.iconUrl;
                name = listingModel.name;
                break;
        }

        Glide.with(requireContext())
                .load(iconUrl)
                .into((ImageView)root.findViewById(R.id.iteminfo_imageview));
        ((TextView)root.findViewById(R.id.iteminfo_itemtitle)).setText(name);

        getCommodity(root);

        checkWorkshopButton(actions, workshopButton);

        LineChart priceChart = root.findViewById(R.id.iteminfo_pricechart);
        priceChart.setDoubleTapToZoomEnabled(false);
        priceChart.setScaleYEnabled(false);
        getPriceChart(priceChart);

        return root;
    }

    private void getPriceChart(LineChart priceChart){
        new Thread(() -> {
            try {
                MarketManager marketManager = new MarketManager(requireContext(),
                        ((MarketActivity)requireActivity()).gameId);
            MarketItemPriceHistory priceHistory = marketManager.loadItemPriceHistory(name);

            if (priceHistory == null || priceHistory.prices == null || priceHistory.prices.size() < 1){
                priceChart.setData(new LineData(new LineDataSet(new ArrayList<>(), "Prices")));
                priceChart.invalidate();
                return;
            }

            List<Entry> priceList = new ArrayList<>();

            for (int i = 0; i < priceHistory.prices.size(); i++) {
                priceList.add(new Entry(i, Float.parseFloat(priceHistory.prices.get(i).get(1))));
            }

            LineDataSet priceDataSet = new LineDataSet(priceList, "Prices");
            priceDataSet.setDrawCircles(false);
            priceDataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
            priceDataSet.setColor(getResources().getColor(R.color.purple_500, requireActivity().getTheme()));

            int textColor = getResources().getColor(R.color.textColor, requireContext().getTheme());
            priceChart.getXAxis().setTextColor(textColor);
            priceChart.getAxisLeft().setTextColor(textColor);
            priceChart.getAxisRight().setTextColor(textColor);
            //priceDataSet.setValueTextColor(textColor);

            priceChart.setData(new LineData(priceDataSet));
            priceChart.setMarker(new ChartMarkerView(requireContext(), R.layout.markerview,
                    priceHistory, priceChart));
            priceChart.invalidate();
            }
            catch (Exception e) {
                Log.e(MainActivity.LOG_TAG, "Failed to get priceChart: " + e.getMessage());
            }
        }).start();
    }

    private void getCommodity(View root){
        new Thread(() -> {
            MarketManager marketManager = new MarketManager(requireContext(),
                    ((MarketActivity)requireActivity()).gameId);
            model = marketManager.getItemCommodity(name);
            setCommodity(root);
        }).start();
    }

    private void checkWorkshopButton(List<ActionModel> actions, MaterialButton workshopButton){
        if (actions == null || actions.size() < 1){ return; }

        for (ActionModel model : actions){
            if (model.link != null){
                setWorkshopButtonLink(workshopButton);
                workshopLink = model.link;
                break;
            }
        }
    }

    private void setWorkshopButtonLink(MaterialButton button){
        button.setVisibility(View.VISIBLE);
        button.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(workshopLink));
            try{
                startActivity(intent);
            }
            catch (Exception e){
                Log.e(MainActivity.LOG_TAG, "Failed to open workshop link: " + e.getMessage());
            }
        });
    }

    private void setCommodity(View root){
        if (model == null){
            try{
                requireActivity().runOnUiThread(() -> {
                    ((ConstraintLayout)root.findViewById(R.id.iteminfo_main)).setLayoutTransition(null);
                    root.findViewById(R.id.iteminfo_loader).setVisibility(View.GONE);
                    root.findViewById(R.id.iteminfo_error).setVisibility(View.VISIBLE);
                });
            }
            catch (Exception ignored){}
            return;
        }

        try{
            Activity activity = requireActivity();

            activity.runOnUiThread(() -> {
                ((TextView)root.findViewById(R.id.iteminfo_selllistings))
                        .setText(String.format(getResources().getString(R.string.sell_listings_placeholder), model.sell_order_count));
                ((TextView)root.findViewById(R.id.iteminfo_buyorders))
                        .setText(String.format(getResources().getString(R.string.buy_orders_placeholder), model.buy_order_count));
            });

            addOrdersToTable(activity, root.findViewById(R.id.iteminfo_selltablelayout), model.sell_order_table);
            addOrdersToTable(activity, root.findViewById(R.id.iteminfo_buyTableLayout), model.buy_order_table);

            activity.runOnUiThread(() -> {
                root.findViewById(R.id.iteminfo_loader).setVisibility(View.GONE);
                root.findViewById(R.id.iteminfo_listings).setVisibility(View.VISIBLE);
            });
        }
        catch (Exception e) { Log.e(MainActivity.LOG_TAG, e.toString()); }
    }

    private void addOrdersToTable(Activity activity, TableLayout tableLayout, List<MarketOrderModel> orders){
        if (orders == null || orders.size() < 1){ return; }

        Context context = requireContext();
        Resources resources = getResources();
        Resources.Theme theme = context.getTheme();
        boolean color = true;
        for (MarketOrderModel marketOrderModel : orders){
            TableRow tableRow = new TableRow(context);
            if (color) {tableRow.setBackgroundColor(resources.getColor(R.color.TableRowAltColor, theme));}
            color = !color;
            tableRow.addView(getTextView(context, resources, theme, marketOrderModel.price));
            tableRow.addView(getTextView(context, resources, theme, marketOrderModel.quantity));
            try{
                activity.runOnUiThread(() -> tableLayout.addView(tableRow));
            }
            catch (Exception ignored){}
        }
    }

    private TextView getTextView(Context context, Resources resources, Resources.Theme theme, String text){
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 20);
        textView.setTextColor(resources.getColor(R.color.textColor, theme));
        textView.setLayoutParams(new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT,
                TableRow.LayoutParams.WRAP_CONTENT, 0.5f));
        textView.setPadding(5, 5, 5, 5);
        return textView;
    }

    @Override
    public void onSaveInstanceState(@NonNull @NotNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("commodity", new Gson().toJson(model));
        outState.putString("iconUrl", iconUrl);
        outState.putString("name", name);
        if (workshopLink == null){return;}
        outState.putString("workshopLink", workshopLink);
    }
}
