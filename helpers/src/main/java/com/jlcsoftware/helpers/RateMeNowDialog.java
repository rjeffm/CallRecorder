package com.jlcsoftware.helpers;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;


/**
 * Created by Jeff on 10-Apr-16.
 */
public class RateMeNowDialog {
    /**
     * Show the RateMeNow Dialot
     * @param context
     * @param numberOfRuns number of runs before showing the dialog
     */
    public static void showRateDialog(final Context context, int numberOfRuns) {
        if (context == null) return;

        final SharedPreferences prefs = context.getSharedPreferences("RateDialog", 0);
        if (prefs.getBoolean("dont_show_again", false)) {
            return;
        }

        int count = prefs.getInt("launch_count", numberOfRuns);
        if (0 < count) {
            prefs.edit().putInt("launch_count", --count).commit();
            return;
        }
        prefs.edit().putInt("launch_count", numberOfRuns).commit();

        final AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle(R.string.rate_me_title);
        builder.setMessage(R.string.rate_me_message);
        builder.setPositiveButton(R.string.rate_me_positive_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String link = "market://details?id=";
                try {
                    // play market available
                    context.getPackageManager()
                            .getPackageInfo("com.android.vending", 0);
                    // not available
                } catch (PackageManager.NameNotFoundException e) {
                    e.printStackTrace();
                    // should use browser
                    link = "https://play.google.com/store/apps/details?id=";
                }
                // starts external action
                context.startActivity(new Intent(Intent.ACTION_VIEW,
                        Uri.parse(link + context.getPackageName())));

                prefs.edit().putBoolean("dont_show_again", true).commit();
            }
        });

        builder.setNegativeButton(R.string.rate_me_negative_btn, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                prefs.edit().putBoolean("dont_show_again", true).commit();
                dialog.dismiss();
            }
        });

        builder.setNeutralButton(R.string.rate_me_neutral_btn, null);
        builder.show();

    }

}
