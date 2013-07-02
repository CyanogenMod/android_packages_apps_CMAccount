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
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;

import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class CMIDUtils {

    private static final String TAG = CMIDUtils.class.getSimpleName();
    private static final Random sRandom = new Random();

    private static final String KEY_UDID = "udid";

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

    public static String getUniqueDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(Constants.SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        String udid = prefs.getString(KEY_UDID, null);
        if (udid != null) return udid;
        String wifiInterface = SystemProperties.get("wifi.interface");
        if (wifiInterface != null) {
            try {
                List<NetworkInterface> interfaces = Collections.list(NetworkInterface.getNetworkInterfaces());
                for (NetworkInterface networkInterface : interfaces) {
                    if (wifiInterface.equals(networkInterface.getDisplayName())) {
                        byte[] mac = networkInterface.getHardwareAddress();
                        if (mac != null) {
                            StringBuilder buf = new StringBuilder();
                            for (int i=0; i < mac.length; i++)
                                buf.append(String.format("%02X:", mac[i]));
                            if (buf.length()>0) buf.deleteCharAt(buf.length()-1);
                            if (Constants.DEBUG) Log.d(TAG, "using wifi mac for id : " + buf.toString());
                            return digest(prefs, context.getPackageName() + buf.toString());
                        }
                    }

                }
            } catch (SocketException e) {
                Log.e(TAG, "Unable to get wifi mac address", e);
            }
        }
        //If we fail, just use android id.
        return digest(prefs, context.getPackageName() + Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID));
    }


    private static String digest(SharedPreferences prefs, String input) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            String id = new BigInteger(1, md.digest(input.getBytes())).toString(16).toUpperCase();
            prefs.edit().putString(KEY_UDID, id).commit();
            return id;
        } catch (Exception e) {
            return null;
        }
    }
}
