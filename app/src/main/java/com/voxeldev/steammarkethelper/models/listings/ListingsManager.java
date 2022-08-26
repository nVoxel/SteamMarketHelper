package com.voxeldev.steammarkethelper.models.listings;

import android.content.Context;
import android.util.Log;

import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.common.RequestManager;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

import okhttp3.Request;
import okhttp3.Response;

public class ListingsManager extends RequestManager {

    private final Context context;
    private final String gameName;
    private Document marketPage;

    public ListingsManager(Context context, String gameName) {
        super(new AuthModel(context));

        this.context = context;
        this.gameName = gameName;

        try {
            loadMarketPage();
        } catch (Exception e) {
            Log.e(MainActivity.LOG_TAG, "Failed to load marketPage in ListingsManager: " + e.getMessage());
        }
    }

    public List<ListingModel> getSellListings() {
        if (marketPage == null) {
            return null;
        }

        Elements allListings = marketPage.select(
                "#tabContentsMyActiveMarketListingsRows div.market_listing_row");

        return selectGameListings(allListings, true);
    }

    public List<ListingModel> getBuyOrders() {
        if (marketPage == null) {
            return null;
        }

        Elements allOrders = marketPage.select(
                "#tabContentsMyListings div.my_listing_section:nth-child(2) div.market_listing_row");

        return selectGameListings(allOrders, false);
    }

    private List<ListingModel> selectGameListings(Elements allElements, boolean isSellListings) {
        List<ListingModel> gameListings = new ArrayList<>();

        for (Element element : allElements) {
            String elementGameName = element.selectFirst("span.market_listing_game_name").text();

            String id = element.id().contains("mylisting_") ?
                    element.id().replace("mylisting_", "") :
                    element.id().replace("mybuyorder_", "");

            if (elementGameName != null && elementGameName.equals(gameName)) {
                element.select("span.market_listing_inline_buyorder_qty").remove();
                gameListings.add(new ListingModel(
                        id,
                        element.selectFirst("img").attr("src"),
                        element.selectFirst(
                                "div.market_listing_item_name_block a.market_listing_item_name_link")
                                .text(),
                        element.selectFirst(
                                "div.market_listing_item_name_block a.market_listing_item_name_link")
                                .attr("href"),
                        isSellListings ?
                                String.format(context.getString(R.string.listed_on),
                                        element.selectFirst("div.market_listing_listed_date").text()) :
                                String.format(context.getString(R.string.quantity),
                                        element.selectFirst("div.market_listing_buyorder_qty").text()),
                        element.selectFirst("div.market_listing_my_price").text()
                ));
            }
        }

        return gameListings;
    }

    private void loadMarketPage() throws Exception {
        Request marketRequest = buildRequest(
                "https://steamcommunity.com/market/",
                AuthModel.NECESSARY_MARKET_COOKIE + getAuthModel().loadCookie());

        Response marketResponse = getClient().newCall(marketRequest).execute();

        marketPage = Jsoup.parse(marketResponse.body().string());
    }
}
