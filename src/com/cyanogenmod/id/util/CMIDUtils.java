package com.cyanogenmod.id.util;

import com.cyanogenmod.id.Constants;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.SystemClock;
import android.util.Log;

import java.util.Random;

public class CMIDUtils {

    private static final String TAG = CMIDUtils.class.getSimpleName();
    private static final Random sRandom = new Random();

    private CMIDUtils(){}

    public static void resetBackoff(SharedPreferences prefs) {
        setBackoff(prefs, Constants.DEFAULT_BACKOFF_MS);
    }

    private static int getBackoff(SharedPreferences prefs) {
        return prefs.getInt(Constants.BACKOFF_MS, Constants.DEFAULT_BACKOFF_MS);
    }

    private static void setBackoff(SharedPreferences prefs, int backoff) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(Constants.BACKOFF_MS, backoff);
        editor.commit();
    }

    public static void scheduleRetry(Context context, SharedPreferences prefs, Intent intent) {
        int backoffTimeMs = getBackoff(prefs);
        int nextAttempt = backoffTimeMs / 2 + sRandom.nextInt(backoffTimeMs);
        if (Constants.DEBUG) Log.d(TAG, "Scheduling retry, backoff = " +
                nextAttempt + " (" + backoffTimeMs + ") for " + intent.getAction());
        PendingIntent retryPendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + nextAttempt,
                retryPendingIntent);
        if (backoffTimeMs < Constants.MAX_BACKOFF_MS) {
            setBackoff(prefs, backoffTimeMs * 2);
        }
    }

    public static Account getAccountByName(Context context, String name) {
        final AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(Constants.ACCOUNT_TYPE);
        for (Account account : accounts) {
            if (account.name.equals(name)) {
                return account;
            }
        }
        return null;
    }
}
