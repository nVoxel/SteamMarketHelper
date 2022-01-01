package com.voxeldev.steammarkethelper.ui.dialogs;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import com.voxeldev.steammarkethelper.R;

public class MarketActionWarningDialog {

    private final SharedPreferences sharedPreferences;
    private final AlertDialog alertDialog;
    private final Thread continueThread;

    public MarketActionWarningDialog(Context context, Thread continueThread, Thread cancelThread) {
        this.sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        this.continueThread = continueThread;

        alertDialog = new AlertDialog.Builder(context)
                .setTitle(R.string.market_action_warning_title)
                .setMessage(R.string.market_action_warning)
                .setPositiveButton(R.string.market_action_continue,
                        (dialog, which) -> {
                            continueThread.start();
                            sharedPreferences.edit().putBoolean("marketActionWarning", false)
                                    .apply();
                        })
                .setNegativeButton(R.string.market_action_cancel,
                        (dialog, which) -> {
                            cancelThread.start();
                            Toast.makeText(context,
                                    R.string.market_action_canceled, Toast.LENGTH_LONG).show();
                        })
                .setCancelable(false)
                .create();
    }

    public void show() {
        boolean marketActionWarning = sharedPreferences
                .getBoolean("marketActionWarning", true);

        if (marketActionWarning) {
            alertDialog.show();
        }
        else {
            continueThread.start();
        }
    }
}
