package com.voxeldev.steammarkethelper.ui.dialogs;

import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.bottomsheet.BottomSheetDialogFragment;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.common.RequestManager;
import com.voxeldev.steammarkethelper.models.inventory.InventoryManager;
import com.voxeldev.steammarkethelper.models.market.MarketCurrencyModel;
import com.voxeldev.steammarkethelper.models.market.SellResponseModel;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.List;
import java.util.Objects;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class ItemSellDialog extends BottomSheetDialogFragment {

    private int currencyCode;
    private double marketFee;
    private String currencyString;
    private String currentReceiveText;
    private String currentPaysText;

    public static ItemSellDialog getInstance(int appId, String assetId,
                                             String itemName, String itemIconUrl) {
        Bundle args = new Bundle();
        args.putInt("appId", appId);
        args.putString("assetId", assetId);
        args.putString("itemName", itemName);
        args.putString("itemIconUrl", itemIconUrl);
        ItemSellDialog itemSellDialog = new ItemSellDialog();
        itemSellDialog.setArguments(args);
        return itemSellDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_itemsell, container, false);
        Bundle args = getArguments();

        MaterialTextView nameTextView = root.findViewById(R.id.itemsell_nametextview);
        nameTextView.setText(args.getString("itemName"));

        Integer width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 80,
                requireContext().getResources().getDisplayMetrics());
        //noinspection SuspiciousNameCombination
        Glide.with(requireContext())
                .load(ItemInfoDialog.iconUrlPrefix + args.getString("itemIconUrl"))
                .into(new CustomTarget<Drawable>(width, width) {
                    @Override
                    public void onResourceReady(@NonNull @NotNull Drawable resource,
                                                @Nullable @org.jetbrains.annotations.Nullable Transition<? super Drawable> transition) {
                        nameTextView.setCompoundDrawablesWithIntrinsicBounds(
                                        resource, null, null, null);
                    }

                    @Override
                    public void onLoadCleared(@Nullable @org.jetbrains.annotations.Nullable Drawable placeholder) {
                        nameTextView.setCompoundDrawablesWithIntrinsicBounds(
                                        placeholder, null, null, null);
                    }
                });

        TextInputEditText receiveEditText = root.findViewById(R.id.itemsell_receive);
        TextInputEditText paysEditText = root.findViewById(R.id.itemsell_pays);

        ConstraintLayout mainConstraintLayout = root.findViewById(R.id.itemsell_main);
        CircularProgressIndicator loader = root.findViewById(R.id.itemsell_loader);

        MaterialButton sellButton = root.findViewById(R.id.itemsell_sellbutton);
        sellButton.setOnClickListener(v -> {
            mainConstraintLayout.setVisibility(View.INVISIBLE);
            loader.setVisibility(View.VISIBLE);

            new Thread(() ->
                    makeSellRequest(args.getInt("appId"), args.getString("assetId")))
                    .start();
        });

        if (savedInstanceState != null) {
            marketFee = savedInstanceState.getDouble("marketFee");
            currencyString = savedInstanceState.getString("currencyString");
            currentReceiveText = savedInstanceState.getString("currentReceiveText");
            currentPaysText = savedInstanceState.getString("currentPaysText");

            setTextChangedListeners(receiveEditText, paysEditText);
            receiveEditText.setText(currentReceiveText);
            paysEditText.setText(currentPaysText);

            loader.setVisibility(View.GONE);
            mainConstraintLayout.setVisibility(View.VISIBLE);

            return root;
        }

        new Thread(() -> {
            try {
                currencyString = getCurrencyString();

                requireActivity().runOnUiThread(() -> {
                    setTextChangedListeners(receiveEditText, paysEditText);

                    loader.setVisibility(View.GONE);
                    mainConstraintLayout.setVisibility(View.VISIBLE);
                });
            }
            catch (Exception e) {
                Log.e(MainActivity.LOG_TAG, "Failed to get currency string: " + e.getMessage());
            }
        }).start();

        return root;
    }

    private void makeSellRequest(int appId, String assetId) {
        if (currentReceiveText == null || currentReceiveText.equals("") ||
                currentReceiveText.replaceAll("[^1-9]+", "").equals("")) {
            requireActivity().runOnUiThread(() ->
                    Toast.makeText(requireContext(), R.string.incorrect_price, Toast.LENGTH_LONG)
                            .show());
        }

        try {
            RequestManager requestManager = new RequestManager(new AuthModel(requireContext()));
            Request request = buildSellRequest(requestManager.getAuthModel().loadCookie(),
                    String.valueOf(appId), assetId, "1");
            Response response = requestManager.getClient().newCall(request).execute();

            if (response.code() != 200 || response.body() == null) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), R.string.failed_sell, Toast.LENGTH_LONG)
                                .show());
                return;
            }

            SellResponseModel sellResponseModel = new Gson().fromJson(response.body().string(),
                    SellResponseModel.class);

            requireActivity().runOnUiThread(() -> {
                if (!sellResponseModel.success) {
                    Toast.makeText(requireContext(), R.string.failed_sell, Toast.LENGTH_LONG)
                            .show();
                }
                else if (sellResponseModel.needs_mobile_confirmation) {
                    Toast.makeText(requireContext(), R.string.sell_mobile_confirmation, Toast.LENGTH_LONG)
                            .show();
                }
                else if (sellResponseModel.needs_email_confirmation) {
                    Toast.makeText(requireContext(), R.string.sell_email_confirmation, Toast.LENGTH_LONG)
                            .show();
                }
                else {
                    Toast.makeText(requireContext(), R.string.sell_success, Toast.LENGTH_LONG)
                            .show();
                }
            });

            dismiss();
        } catch (Exception e) {
            Log.e(MainActivity.LOG_TAG, "Failed to make sell request: " + e.getMessage());
        }
    }

    private Request buildSellRequest(String cookie, String appId, String assetId, String amount) {
        return new Request.Builder()
                .url("https://steamcommunity.com/market/sellitem")
                .addHeader("User-Agent", RequestManager.defaultUserAgent)
                .addHeader("Cookie", cookie)
                .addHeader("Referer", "https://steamcommunity.com/id/smh/inventory")
                .post(new FormBody.Builder()
                        .add("sessionid",
                                getSessionId(cookie))
                        .add("appid", appId)
                        .add("contextid", "2") // ???
                        .add("assetid", assetId)
                        .add("amount", amount)
                        .add("price",
                                currentReceiveText.replaceAll("[^0-9]+", ""))
                        .build())
                .build();
    }

    private String getSessionId(String cookie) {
        String findString = "sessionid=";
        int findStringIndex = cookie.indexOf(findString);

        return cookie.substring(
                findStringIndex + findString.length(),
                cookie.indexOf(";", findStringIndex)
        );
    }

    private String getCurrencyString() {
        getSellData();

        if (currencyCode == -1) {
            return requireContext().getString(R.string.not_available);
        }

        String currencyData = getCurrencyData();
        if (currencyData.equals("")){
            return requireContext().getString(R.string.not_available);
        }

        try {
            List<MarketCurrencyModel> currencyList = new Gson().fromJson(currencyData,
                    new TypeToken<List<MarketCurrencyModel>>(){}.getType());

            return Objects.requireNonNull(currencyList.stream()
                    .filter(i -> i.eCurrencyCode == currencyCode)
                    .findFirst().orElse(null)).strSymbol;
        }
        catch (Exception e) {
            Log.e(MainActivity.LOG_TAG, "Failed to get currency string: " + e.getMessage());
        }

        return requireContext().getString(R.string.not_available);
    }

    private void getSellData() { // gets currency code and fee
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
        InputStream inputStream = getResources().openRawResource(R.raw.market_currency_data);
        Writer writer = new StringWriter();
        char[] buffer = new char[1024];

        try {
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

    private void setTextChangedListeners(TextInputEditText receiveEditText, TextInputEditText paysEditText) {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(currencyString + " ");
        ((DecimalFormat) numberFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        receiveEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (currencyString != null && !charSequence.toString().equals(currentReceiveText)) {
                    receiveEditText.removeTextChangedListener(this);

                    Double inputDouble;
                    try {
                        inputDouble = Double.parseDouble(
                                charSequence.toString().replaceAll("[^0-9]+", ""))/100;
                    }
                    catch (Exception ignored) {
                        inputDouble = .0;
                    }

                    String formattedReceive = numberFormat.format(inputDouble);
                    currentReceiveText = formattedReceive;

                    String formattedPays = numberFormat.format(inputDouble * marketFee);
                    currentPaysText = formattedPays;

                    receiveEditText.setText(formattedReceive);
                    receiveEditText.setSelection(formattedReceive.length());

                    paysEditText.setText(formattedPays);
                    paysEditText.setSelection(formattedPays.length());

                    receiveEditText.addTextChangedListener(this);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) { }
        });

        paysEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (currencyString != null && !charSequence.toString().equals(currentPaysText)) {
                    paysEditText.removeTextChangedListener(this);

                    Double inputDouble;
                    try {
                        inputDouble = Double.parseDouble(
                                charSequence.toString().replaceAll("[^0-9]+", ""))/100;
                    }
                    catch (Exception ignored) {
                        inputDouble = .0;
                    }

                    String formattedPays = numberFormat.format(inputDouble);
                    currentPaysText = formattedPays;

                    String formattedReceive = numberFormat.format(inputDouble / marketFee);
                    currentReceiveText = formattedReceive;

                    paysEditText.setText(formattedPays);
                    paysEditText.setSelection(formattedPays.length());

                    receiveEditText.setText(formattedReceive);
                    receiveEditText.setSelection(formattedReceive.length());

                    paysEditText.addTextChangedListener(this);
                }
            }
            @Override
            public void afterTextChanged(Editable editable) { }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putDouble("marketFee", marketFee);
        outState.putString("currencyString", currencyString);
        outState.putString("currentReceiveText", currentReceiveText);
        outState.putString("currentPaysText", currentPaysText);
    }
}
