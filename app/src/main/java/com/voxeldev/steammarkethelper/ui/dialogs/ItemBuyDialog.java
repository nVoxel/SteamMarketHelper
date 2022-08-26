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

import com.airbnb.lottie.LottieAnimationView;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textview.MaterialTextView;
import com.google.gson.Gson;
import com.voxeldev.steammarkethelper.MainActivity;
import com.voxeldev.steammarkethelper.R;
import com.voxeldev.steammarkethelper.models.auth.AuthModel;
import com.voxeldev.steammarkethelper.models.common.RequestManager;
import com.voxeldev.steammarkethelper.models.market.BuyResponseModel;
import com.voxeldev.steammarkethelper.ui.misc.DismissAnimatorListener;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class ItemBuyDialog extends MarketActionDialog {

    private String currentPayText;
    private String currentAmountText;
    private String currentMaxPriceText;
    private ConstraintLayout mainConstraintLayout;
    private CircularProgressIndicator loader;
    private LottieAnimationView animationView;

    public static ItemBuyDialog getInstance(int appId, String itemName,
                                            String marketHashName, String itemIconUrl) {
        Bundle args = new Bundle();
        args.putInt("appId", appId);
        args.putString("itemName", itemName);
        args.putString("marketHashName", marketHashName);
        args.putString("itemIconUrl", itemIconUrl);

        ItemBuyDialog itemBuyDialog = new ItemBuyDialog();
        itemBuyDialog.setArguments(args);

        return itemBuyDialog;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.dialog_itembuy, container, false);
        Bundle args = getArguments();

        MaterialTextView nameTextView = root.findViewById(R.id.itembuy_nametextview);
        nameTextView.setText(args.getString("itemName"));

        int width = (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 80,
                requireContext().getResources().getDisplayMetrics());
        //noinspection SuspiciousNameCombination
        Glide.with(requireContext())
                .load(RequestManager.ICON_URL_PREFIX + args.getString("itemIconUrl"))
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

        TextInputEditText payEditText = root.findViewById(R.id.itembuy_pay);
        TextInputEditText amountEditText = root.findViewById(R.id.itembuy_amount);
        MaterialTextView maxPriceTextView = root.findViewById(R.id.itembuy_maxprice);

        mainConstraintLayout = root.findViewById(R.id.itembuy_main);
        loader = root.findViewById(R.id.itembuy_loader);

        animationView = root.findViewById(R.id.itembuy_animationview);

        MaterialButton buyButton = root.findViewById(R.id.itembuy_buybutton);
        buyButton.setOnClickListener(v -> {
            mainConstraintLayout.setVisibility(View.INVISIBLE);
            loader.setVisibility(View.VISIBLE);

            new MarketActionWarningDialog(requireContext(),
                    new Thread(() ->
                            makeBuyRequest(args.getInt("appId"),
                                    args.getString("marketHashName"))),
                    new Thread(() ->
                            requireActivity().runOnUiThread(() -> {
                                mainConstraintLayout.setVisibility(View.VISIBLE);
                                loader.setVisibility(View.GONE);
                            }))
            ).show();
        });

        if (savedInstanceState != null) {
            int savedCurrencyCode = savedInstanceState.getInt("currencyCode", -2);
            double savedMarketFee = savedInstanceState.getDouble("marketFee");
            String savedCurrencyString = savedInstanceState.getString("currencyString");

            if (savedCurrencyCode != -2 && savedMarketFee != 0 && savedCurrencyString != null) {
                initializeMarket(savedCurrencyCode, savedMarketFee, savedCurrencyString);
                currentPayText = savedInstanceState.getString("currentPayText");
                currentAmountText = savedInstanceState.getString("currentAmountText");
                currentMaxPriceText = savedInstanceState.getString("currentMaxPriceText");

                try {
                    setTextChangedListeners(payEditText, amountEditText, maxPriceTextView);
                } catch (Exception e) {
                    Log.e(MainActivity.LOG_TAG,
                            "Failed to set text changed listeners: " + e.getMessage());
                }
                payEditText.setText(currentPayText);
                amountEditText.setText(currentAmountText);
                if (currentMaxPriceText != null) {
                    maxPriceTextView.setVisibility(View.VISIBLE);
                    maxPriceTextView.setText(currentMaxPriceText);
                }

                loader.setVisibility(View.GONE);
                mainConstraintLayout.setVisibility(View.VISIBLE);

                return root;
            }
        }

        new Thread(() -> {
            initializeMarket();

            try {
                requireActivity().runOnUiThread(() -> {
                    try {
                        setTextChangedListeners(payEditText, amountEditText, maxPriceTextView);
                    }
                    catch (Exception e) {
                        Log.e(MainActivity.LOG_TAG,
                                "Failed to set text changed listeners: " + e.getMessage());
                    }

                    loader.setVisibility(View.GONE);
                    mainConstraintLayout.setVisibility(View.VISIBLE);
                });
            }
            catch (Exception e) {
                Log.e(MainActivity.LOG_TAG,
                        "Failed to load ItemSellDialog: " + e.getMessage());
            }
        }).start();

        return root;
    }

    private void makeBuyRequest(int appId, String marketHashName) {
        if (currentPayText == null || currentPayText.equals("") ||
                currentPayText.replaceAll("[^1-9]+", "").equals("")) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), R.string.incorrect_price, Toast.LENGTH_LONG).show();
                loader.setVisibility(View.GONE);
                mainConstraintLayout.setVisibility(View.VISIBLE);
            });
            return;
        }

        if (currentAmountText == null || currentAmountText.equals("")) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), R.string.incorrect_amount, Toast.LENGTH_LONG).show();
                loader.setVisibility(View.GONE);
                mainConstraintLayout.setVisibility(View.VISIBLE);
            });
            return;
        }

        int amount;
        try {
            amount = Integer.parseInt(currentAmountText);
        }
        catch (Exception ignored) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), R.string.incorrect_amount, Toast.LENGTH_LONG).show();
                loader.setVisibility(View.GONE);
                mainConstraintLayout.setVisibility(View.VISIBLE);
            });
            return;
        }

        if (amount < 1) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), R.string.amount_zero, Toast.LENGTH_LONG).show();
                loader.setVisibility(View.GONE);
                mainConstraintLayout.setVisibility(View.VISIBLE);
            });
            return;
        }

        try {
            RequestManager requestManager = new RequestManager(new AuthModel(requireContext()));
            Request request = buildBuyRequest(requestManager.getAuthModel().loadCookie(),
                    String.valueOf(appId), marketHashName, currentAmountText);
            Response response = requestManager.getClient().newCall(request).execute();

            if (response.code() != 200 || response.body() == null) {
                requireActivity().runOnUiThread(() ->{
                    Toast.makeText(requireContext(), R.string.failed_buy, Toast.LENGTH_LONG)
                            .show();
                    loader.setVisibility(View.GONE);
                    mainConstraintLayout.setVisibility(View.VISIBLE);
                });
                return;
            }

            BuyResponseModel buyResponseModel = new Gson().fromJson(response.body().string(),
                    BuyResponseModel.class);

            requireActivity().runOnUiThread(() -> {
                loader.setVisibility(View.GONE);
                animationView.setVisibility(View.VISIBLE);

                if (buyResponseModel.success != 1) {
                    animationView.setAnimation(R.raw.market_action_failed);
                    animationView.addAnimatorListener(new DismissAnimatorListener(this,
                            getString(R.string.failed_buy)));
                }
                else {
                    animationView.setAnimation(R.raw.market_action_success);
                    animationView.addAnimatorListener(new DismissAnimatorListener(this,
                            getString(R.string.buy_success)));
                }

                animationView.playAnimation();
            });
        }
        catch (Exception e) {
            Log.e(MainActivity.LOG_TAG, "Failed to make buy request: " + e.getMessage());
        }
    }

    private Request buildBuyRequest(String cookie, String appId, String marketHashName, String amount)
            throws Exception {
        String pt = currentMaxPriceText.replaceAll("[^0-9]+", "");

        return new Request.Builder()
                .url("https://steamcommunity.com/market/createbuyorder")
                .addHeader("User-Agent", RequestManager.DEFAULT_USER_AGENT)
                .addHeader("Cookie", cookie)
                .addHeader("Referer", String.format(
                        "https://steamcommunity.com/market/listings/%s/%s",
                        appId, marketHashName))
                .post(new FormBody.Builder()
                        .add("sessionid",
                                getSessionId(cookie))
                        .add("currency", String.valueOf(getCurrencyCode()))
                        .add("appid", appId)
                        .add("market_hash_name", marketHashName)
                        .add("price_total", pt)
                        .add("quantity", amount)
                        .add("billing_state", "") // ???
                        .add("save_my_address", "0") // ???
                        .build())
                .build();
    }

    private void setTextChangedListeners(TextInputEditText payEditText,
                                         TextInputEditText amountEditText,
                                         MaterialTextView maxPriceTextView) throws Exception {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(getCurrencyString() + " ");
        ((DecimalFormat) numberFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        payEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                try {
                    if (!charSequence.toString().equals(currentPayText)) {
                        payEditText.removeTextChangedListener(this);

                        Double inputDouble;
                        try {
                            inputDouble = Double.parseDouble(
                                    charSequence.toString().replaceAll("[^0-9]+", ""))/100;
                        }
                        catch (Exception e) {
                            Log.e(MainActivity.LOG_TAG,
                                    "Failed to parse inputDouble: " + e.getMessage());
                            inputDouble = .0;
                        }

                        String formattedPay = numberFormat.format(inputDouble);
                        currentPayText = formattedPay;

                        payEditText.setText(formattedPay);
                        payEditText.setSelection(formattedPay.length());

                        if (maxPriceTextView.getVisibility() == View.GONE) {
                            maxPriceTextView.setVisibility(View.VISIBLE);
                        }

                        Editable amountEditable = amountEditText.getText();
                        String maxPriceText;

                        if (amountEditable != null && !amountEditable.toString().equals("")) {
                            maxPriceText = String.format(getString(R.string.max_price),
                                    getCurrencyString(), inputDouble *
                                            Double.parseDouble(amountEditable.toString()));
                            currentAmountText = amountEditable.toString();
                        }
                        else {
                            maxPriceText = String.format(getString(R.string.max_price),
                                    getCurrencyString(), inputDouble);
                        }

                        maxPriceTextView.setText(maxPriceText);
                        currentMaxPriceText = maxPriceText;

                        payEditText.addTextChangedListener(this);
                    }
                }
                catch (Exception e) {
                    Log.e(MainActivity.LOG_TAG,
                            "Failed to process textChanged: " + e.getMessage());
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });

        amountEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                try {
                    double inputDouble;
                    try {
                        inputDouble = Double.parseDouble(
                                currentPayText.replaceAll("[^0-9]+", ""))/100;
                    }
                    catch (Exception e) {
                        Log.e(MainActivity.LOG_TAG,
                                "Failed to parse inputDouble: " + e.getMessage());
                        inputDouble = .0;
                    }

                    if (maxPriceTextView.getVisibility() == View.GONE) {
                        maxPriceTextView.setVisibility(View.VISIBLE);
                    }

                    Editable amountEditable = amountEditText.getText();
                    String maxPriceText;

                    if (amountEditable != null && !amountEditable.toString().equals("")) {
                        maxPriceText = String.format(getString(R.string.max_price),
                                getCurrencyString(), inputDouble *
                                        Double.parseDouble(amountEditable.toString()));

                        currentAmountText = amountEditable.toString();
                    }
                    else {
                        maxPriceText = String.format(getString(R.string.max_price),
                                getCurrencyString(), inputDouble);
                    }

                    maxPriceTextView.setText(maxPriceText);
                    currentMaxPriceText = maxPriceText;
                }
                catch (Exception e) {
                    Log.e(MainActivity.LOG_TAG,
                            "Failed to process textChanged: " + e.getMessage());
                }
            }

            @Override
            public void afterTextChanged(Editable s) { }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        try {
            outState.putInt("currencyCode", getCurrencyCode());
            outState.putDouble("marketFee", getMarketFee());
            outState.putString("currencyString", getCurrencyString());
        } catch (Exception ignored) { }

        outState.putString("currentPayText", currentPayText);
        outState.putString("currentAmountText", currentAmountText);
        outState.putString("currentMaxPriceText", currentMaxPriceText);
    }
}