package com.voxeldev.steammarkethelper.ui.dialogs;

import android.util.Log;

import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.inventory.InventoryManager;
import com.voxeldev.steammarkethelper.models.market.MarketCurrencyModel;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;

import okhttp3.Request;
import okhttp3.Response;

public abstract class MarketActionDialog extends BottomSheetDialogFragment {

    private int currencyCode = -2;
    private double marketFee;
    private String currencyString;

    public void initializeMarket() {
        loadMarketData();
        loadCurrencyString();
    }

    public int getCurrencyCode() throws Exception {
        if (currencyCode == -2) {
            throw new Exception("MarketActionDialog is not initialized");
        }

        return currencyCode;
    }

    public double getMarketFee() throws Exception {
        if (marketFee == 0) {
            throw new Exception("MarketActionDialog is not initialized");
        }

        return marketFee;
    }

    public String getCurrencyString() throws Exception {
        if (currencyString == null) {
            throw new Exception("MarketActionDialog is not initialized");
        }

        return currencyString;
    }

    public void setMarketFee(double marketFee) {
        this.marketFee = marketFee;
    }

    public void setCurrencyString(String currencyString) {
        this.currencyString = currencyString;
    }

    private void loadCurrencyString() {

        if (currencyCode == -1) {
            currencyString = requireContext().getString(R.string.not_available);
            return;
        }

        String currencyData = getCurrencyData();
        if (currencyData.equals("")){
            try {
                currencyString = requireContext().getString(R.string.not_available);
            }
            catch (Exception ignored) {}
            return;
        }

        try {
            List<MarketCurrencyModel> currencyList = new Gson().fromJson(currencyData,
                    new TypeToken<List<MarketCurrencyModel>>(){}.getType());

            currencyString = Objects.requireNonNull(currencyList.stream()
                    .filter(i -> i.eCurrencyCode == currencyCode)
                    .findFirst().orElse(null)).strSymbol;
            return;
        }
        catch (Exception e) {
            Log.e(MainActivity.LOG_TAG, "Failed to get currency string: " + e.getMessage());
        }

        currencyString = requireContext().getString(R.string.not_available);
    }

    private void loadMarketData() { // gets currency code and fee
        try {
            InventoryManager inventoryManager = new InventoryManager(new AuthModel(requireContext()));

            Request request = inventoryManager.buildRequest(String.format(
                    "https://steamcommunity.com/profiles/%s/inventory/", inventoryManager.getSteamId()),
                    AuthModel.necessaryMarketCookie + inventoryManager.getAuthModel().loadCookie());

            Response response = inventoryManager.getClient().newCall(request).execute();

            String body = response.body().string();

            String findString = "\"wallet_currency\":";
            int findStringIndex = body.indexOf(findString);
            currencyCode = Integer.parseInt(body.substring(
                    findStringIndex + findString.length(),
                    body.indexOf(",", findStringIndex)));

            findString = "\"wallet_fee_percent\":\"";
            findStringIndex = body.indexOf(findString);
            marketFee += Double.parseDouble(body.substring(
                    findStringIndex + findString.length(),
                    body.indexOf("\",", findStringIndex)));

            findString = "\"wallet_publisher_fee_percent_default\":\"";
            findStringIndex = body.indexOf(findString);
            marketFee += Double.parseDouble(body.substring(
                    findStringIndex + findString.length(),
                    body.indexOf("\",", findStringIndex)));

            marketFee += 1;
            return;
        } catch (Exception e) {
            Log.e(MainActivity.LOG_TAG, "Failed to get currency code: " + e.getMessage());
        }

        currencyCode = -1;
        marketFee = 1;
    }

    private String getCurrencyData() {
        InputStream inputStream = null;
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];

        try {
            inputStream = getResources().openRawResource(R.raw.market_currency_data);
            Reader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
            int n;
            while ((n = reader.read(buffer)) != -1) {
                writer.write(buffer, 0, n);
            }
        }
        catch (Exception e) {
            Log.e(MainActivity.LOG_TAG, "Failed to get currency data: " + e.getMessage());
        }
        finally {
            try { inputStream.close(); }
            catch (Exception e) {
                Log.e(MainActivity.LOG_TAG,
                        "Failed to close currency data inputStream: " + e.getMessage());
            }
        }

        return writer.toString();
    }

    protected String getSessionId(String cookie) {
        String findString = "sessionid=";
        int findStringIndex = cookie.indexOf(findString);

        return cookie.substring(
                findStringIndex + findString.length(),
                cookie.indexOf(";", findStringIndex)
        );
    }
}
