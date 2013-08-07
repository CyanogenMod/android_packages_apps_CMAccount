package com.cyanogenmod.id.util;

import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
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
        setBackoff(prefs, CMID.DEFAULT_BACKOFF_MS);
    }

    private static int getBackoff(SharedPreferences prefs) {
        return prefs.getInt(CMID.BACKOFF_MS, CMID.DEFAULT_BACKOFF_MS);
    }

    private static void setBackoff(SharedPreferences prefs, int backoff) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(CMID.BACKOFF_MS, backoff);
        editor.commit();
    }

    public static void scheduleRetry(Context context, SharedPreferences prefs, Intent intent) {
        int backoffTimeMs = getBackoff(prefs);
        int nextAttempt = backoffTimeMs / 2 + sRandom.nextInt(backoffTimeMs);
        if (CMID.DEBUG) Log.d(TAG, "Scheduling retry, backoff = " +
                nextAttempt + " (" + backoffTimeMs + ") for " + intent.getAction());
        PendingIntent retryPendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + nextAttempt,
                retryPendingIntent);
        if (backoffTimeMs < CMID.MAX_BACKOFF_MS) {
            setBackoff(prefs, backoffTimeMs * 2);
        }
    }

    public static Account getCMIDAccount(Context context) {
        final AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(CMID.ACCOUNT_TYPE_CMID);
        return accounts.length > 0 ? accounts[0] : null;
    }

    public static void showNotification(Context context, int id, Notification notification) {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(id, notification);
    }

    public static void hideNotification(Context context, int id) {
        NotificationManager notificationManager = (NotificationManager)context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(id);
    }

    public static void tryEnablingWifi(Context context) {
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }

    public static void launchWifiSetup(Activity context) {
        CMIDUtils.tryEnablingWifi(context);
        Intent intent = new Intent(CMID.ACTION_SETUP_WIFI);
        intent.putExtra(CMID.EXTRA_FIRST_RUN, true);
        intent.putExtra(CMID.EXTRA_ALLOW_SKIP, true);
        intent.putExtra(CMID.EXTRA_SHOW_BUTTON_BAR, true);
        intent.putExtra(CMID.EXTRA_ONLY_ACCESS_POINTS, true);
        intent.putExtra(CMID.EXTRA_SHOW_SKIP, true);
        intent.putExtra(CMID.EXTRA_AUTO_FINISH, true);
        intent.putExtra(CMID.EXTRA_PREF_BACK_TEXT, context.getString(R.string.skip));
        context.startActivityForResult(intent, CMID.REQUEST_CODE_SETUP_WIFI);
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean isGSMPhone(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int phoneType = telephonyManager.getPhoneType();
        return phoneType == TelephonyManager.PHONE_TYPE_GSM;
    }

    public static boolean isSimMissing(Context context) {
        TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        int simState = telephonyManager.getSimState();
        return simState == TelephonyManager.SIM_STATE_ABSENT || simState == TelephonyManager.SIM_STATE_UNKNOWN;
    }

    public static String getModVersion() {
        return SystemProperties.get("ro.cm.version");
    }

    public static String getUniqueDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CMID.SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
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
                            if (CMID.DEBUG) Log.d(TAG, "using wifi mac for id : " + buf.toString());
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
            String id = digest("MD5", input);
            prefs.edit().putString(KEY_UDID, id).commit();
            return id;
        } catch (Exception e) {
            return null;
        }
    }

    public static String digest(String algorithm, String id) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            String hash = new BigInteger(1, md.digest(id.getBytes())).toString(16);
            return hash;
        } catch (Exception e) {
            return null;
        }
    }
}
