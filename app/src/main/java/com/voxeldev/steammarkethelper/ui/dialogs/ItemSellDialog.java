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
import com.voxeldev.steammarkethelper.models.market.SellResponseModel;
import com.voxeldev.steammarkethelper.ui.misc.DismissAnimatorListener;

import org.jetbrains.annotations.NotNull;

import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;

import okhttp3.FormBody;
import okhttp3.Request;
import okhttp3.Response;

public class ItemSellDialog extends MarketActionDialog {

    private String currentReceiveText;
    private String currentPaysText;
    private ConstraintLayout mainConstraintLayout;
    private CircularProgressIndicator loader;
    private LottieAnimationView animationView;

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

        mainConstraintLayout = root.findViewById(R.id.itemsell_main);
        loader = root.findViewById(R.id.itemsell_loader);

        animationView = root.findViewById(R.id.itemsell_animationview);

        MaterialButton sellButton = root.findViewById(R.id.itemsell_sellbutton);
        sellButton.setOnClickListener(v -> {
            mainConstraintLayout.setVisibility(View.INVISIBLE);
            loader.setVisibility(View.VISIBLE);

            new Thread(() ->
                    makeSellRequest(args.getInt("appId"), args.getString("assetId")))
                    .start();
        });

        if (savedInstanceState != null) {
            double savedMarketFee = savedInstanceState.getDouble("marketFee");
            String savedCurrencyString = savedInstanceState.getString("currencyString");

            if (savedMarketFee != 0 && savedCurrencyString != null) {
                initializeMarket(savedMarketFee, savedCurrencyString);
                currentReceiveText = savedInstanceState.getString("currentReceiveText");
                currentPaysText = savedInstanceState.getString("currentPaysText");

                try {
                    setTextChangedListeners(receiveEditText, paysEditText);
                } catch (Exception e) {
                    Log.e(MainActivity.LOG_TAG,
                            "Failed to set text changed listeners: " + e.getMessage());
                }
                receiveEditText.setText(currentReceiveText);
                paysEditText.setText(currentPaysText);

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
                        setTextChangedListeners(receiveEditText, paysEditText);
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

    private void makeSellRequest(int appId, String assetId) {
        if (currentReceiveText == null || currentReceiveText.equals("") ||
                currentReceiveText.replaceAll("[^1-9]+", "").equals("")) {
            requireActivity().runOnUiThread(() -> {
                Toast.makeText(requireContext(), R.string.incorrect_price, Toast.LENGTH_LONG).show();
                loader.setVisibility(View.GONE);
                mainConstraintLayout.setVisibility(View.VISIBLE);
            });

            return;
        }

        try {
            RequestManager requestManager = new RequestManager(new AuthModel(requireContext()));
            Request request = buildSellRequest(requestManager.getAuthModel().loadCookie(),
                    String.valueOf(appId), assetId, "1");
            Response response = requestManager.getClient().newCall(request).execute();

            if (response.code() != 200 || response.body() == null) {
                requireActivity().runOnUiThread(() -> {
                    Toast.makeText(requireContext(), R.string.failed_sell, Toast.LENGTH_LONG).show();
                    loader.setVisibility(View.GONE);
                    mainConstraintLayout.setVisibility(View.VISIBLE);
                });
                return;
            }

            SellResponseModel sellResponseModel = new Gson().fromJson(response.body().string(),
                    SellResponseModel.class);

            requireActivity().runOnUiThread(() -> {
                loader.setVisibility(View.GONE);
                animationView.setVisibility(View.VISIBLE);

                if (!sellResponseModel.success) {
                    animationView.setAnimation(R.raw.market_action_failed);
                    animationView.addAnimatorListener(new DismissAnimatorListener(this,
                            getString(R.string.failed_sell)));
                }
                else if (sellResponseModel.needs_mobile_confirmation) {
                    animationView.setAnimation(R.raw.market_action_phone);
                    animationView.addAnimatorListener(new DismissAnimatorListener(this,
                            getString(R.string.sell_mobile_confirmation)));
                }
                else if (sellResponseModel.needs_email_confirmation) {
                    animationView.setAnimation(R.raw.market_action_phone);
                    animationView.addAnimatorListener(new DismissAnimatorListener(this,
                            getString(R.string.sell_email_confirmation)));
                }
                else {
                    animationView.setAnimation(R.raw.market_action_success);
                    animationView.addAnimatorListener(new DismissAnimatorListener(this,
                            getString(R.string.sell_success)));
                }

                animationView.playAnimation();
            });
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

    private void setTextChangedListeners(TextInputEditText receiveEditText, TextInputEditText paysEditText) throws Exception {
        NumberFormat numberFormat = NumberFormat.getCurrencyInstance();
        DecimalFormatSymbols decimalFormatSymbols = new DecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(getCurrencyString() + " ");
        ((DecimalFormat) numberFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        receiveEditText.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) { }
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                try {
                    if (!charSequence.toString().equals(currentReceiveText)) {
                        receiveEditText.removeTextChangedListener(this);

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

                        String formattedReceive = numberFormat.format(inputDouble);
                        currentReceiveText = formattedReceive;

                        String formattedPays = numberFormat.format(inputDouble * getMarketFee());
                        currentPaysText = formattedPays;

                        receiveEditText.setText(formattedReceive);
                        receiveEditText.setSelection(formattedReceive.length());

                        paysEditText.setText(formattedPays);
                        paysEditText.setSelection(formattedPays.length());

                        receiveEditText.addTextChangedListener(this);
                    }
                } catch (Exception e) {
                    Log.e(MainActivity.LOG_TAG,
                            "Failed to process textChanged: " + e.getMessage());
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
                try {
                    if (!charSequence.toString().equals(currentPaysText)) {
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

                        String formattedReceive = numberFormat.format(inputDouble / getMarketFee());
                        currentReceiveText = formattedReceive;

                        paysEditText.setText(formattedPays);
                        paysEditText.setSelection(formattedPays.length());

                        receiveEditText.setText(formattedReceive);
                        receiveEditText.setSelection(formattedReceive.length());

                        paysEditText.addTextChangedListener(this);
                    }
                } catch (Exception e) {
                    Log.e(MainActivity.LOG_TAG,
                            "Failed to process textChanged: " + e.getMessage());
                }
            }
            @Override
            public void afterTextChanged(Editable editable) { }
        });
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        try {
            outState.putDouble("marketFee", getMarketFee());
            outState.putString("currencyString", getCurrencyString());
        } catch (Exception ignored) { }

        outState.putString("currentReceiveText", currentReceiveText);
        outState.putString("currentPaysText", currentPaysText);
    }
}
