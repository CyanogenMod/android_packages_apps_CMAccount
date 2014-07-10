/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.account.util;

import android.os.Bundle;
import android.os.UserManager;
import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.R;
import com.cyanogenmod.account.auth.AuthClient;
import com.cyanogenmod.account.auth.UpdateRequiredActivity;
import com.cyanogenmod.account.ui.WebViewDialogFragment;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.Fragment;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.util.Base64;
import android.util.Log;
import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.spongycastle.math.ec.ECFieldElement;

import java.math.BigInteger;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.security.MessageDigest;
import java.sql.Timestamp;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import org.apache.commons.codec.binary.Hex;

public class CMAccountUtils {

    private static final String TAG = CMAccountUtils.class.getSimpleName();
    private static final Random sRandom = new Random();
    private static final Long INTERVAL_WEEK = 604800000L;

    public static final Pattern EMAIL_ADDRESS
            = Pattern.compile(
            "[a-zA-Z0-9\\+\\.\\_\\%\\-\\+]{1,256}" +
                    "\\@" +
                    "[a-zA-Z0-9][a-zA-Z0-9\\-]{0,64}" +
                    "(" +
                    "\\." +
                    "[A-Za-z]{2,6}" +
                    ")+"
    );

    private static final String KEY_UDID = "udid";

    private CMAccountUtils(){}

    public static void resetBackoff(SharedPreferences prefs) {
        if (CMAccount.DEBUG) Log.d(TAG, "Resetting backoff");
        setBackoff(prefs, CMAccount.DEFAULT_BACKOFF_MS);
    }

    private static int getBackoff(SharedPreferences prefs) {
        return prefs.getInt(CMAccount.BACKOFF_MS, CMAccount.DEFAULT_BACKOFF_MS);
    }

    private static void setBackoff(SharedPreferences prefs, int backoff) {
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(CMAccount.BACKOFF_MS, backoff);
        editor.commit();
    }

    public static void scheduleRetry(Context context, SharedPreferences prefs, Intent intent) {
        int backoffTimeMs = getBackoff(prefs);
        int nextAttempt = backoffTimeMs / 2 + sRandom.nextInt(backoffTimeMs);
        if (CMAccount.DEBUG) Log.d(TAG, "Scheduling retry, backoff = " +
                nextAttempt + " (" + backoffTimeMs + ") for " + intent.getAction());
        PendingIntent retryPendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.set(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + nextAttempt,
                retryPendingIntent);
        if (backoffTimeMs < CMAccount.MAX_BACKOFF_MS) {
            setBackoff(prefs, backoffTimeMs * 2);
        }
    }

    public static void scheduleCMAccountPing(Context context, Intent intent) {
        if (CMAccount.DEBUG) Log.d(TAG, "Scheduling CMAccount ping, starting = " +
                new Timestamp(SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_DAY) + " interval (" + AlarmManager.INTERVAL_DAY + ")");
        PendingIntent reRegisterPendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY,
                reRegisterPendingIntent);
    }

    public static void cancelCMAccountPing(Context context, Intent intent) {
        if (CMAccount.DEBUG) Log.d(TAG, "Canceling CMAccount ping");
        PendingIntent reRegisterPendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(reRegisterPendingIntent);
    }

    public static Account getCMAccountAccount(Context context) {
        final AccountManager am = AccountManager.get(context);
        Account[] accounts = am.getAccountsByType(CMAccount.ACCOUNT_TYPE_CMAccount);
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

    public static void showLearnMoreDialog(Activity context) {
        WebViewDialogFragment.newInstance().setUri(AuthClient.LEARN_MORE_URI).show(context.getFragmentManager(), WebViewDialogFragment.TAG);
    }

    public static void tryEnablingWifi(Context context) {
        WifiManager wifiManager = (WifiManager)context.getSystemService(Context.WIFI_SERVICE);
        if (!wifiManager.isWifiEnabled()) {
            wifiManager.setWifiEnabled(true);
        }
    }

    private static Intent getWifiSetupIntent(Context context) {
        Intent intent = new Intent(CMAccount.ACTION_SETUP_WIFI);
        intent.putExtra(CMAccount.EXTRA_FIRST_RUN, true);
        intent.putExtra(CMAccount.EXTRA_ALLOW_SKIP, true);
        intent.putExtra(CMAccount.EXTRA_SHOW_BUTTON_BAR, true);
        intent.putExtra(CMAccount.EXTRA_ONLY_ACCESS_POINTS, true);
        intent.putExtra(CMAccount.EXTRA_SHOW_SKIP, true);
        intent.putExtra(CMAccount.EXTRA_AUTO_FINISH, true);
        intent.putExtra(CMAccount.EXTRA_PREF_BACK_TEXT, context.getString(R.string.skip));
        return intent;
    }

    public static void launchWifiSetup(Activity context) {
        CMAccountUtils.tryEnablingWifi(context);
        Intent intent = getWifiSetupIntent(context);
        context.startActivityForResult(intent, CMAccount.REQUEST_CODE_SETUP_WIFI);
    }

    public static void launchWifiSetup(Fragment fragment) {
        final Context context = fragment.getActivity();
        CMAccountUtils.tryEnablingWifi(context);
        Intent intent = getWifiSetupIntent(context);
        fragment.startActivityForResult(intent, CMAccount.REQUEST_CODE_SETUP_WIFI);
    }

    public static boolean isNetworkConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        return networkInfo != null && networkInfo.isConnected();
    }

    public static boolean isWifiConnected(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo mWifi = connectivityManager.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
        return mWifi != null && mWifi.isConnected();
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

    public static boolean isUnableToModifyAccounts(Context context) {
        UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);
        Bundle restrictions = um.getUserRestrictions();
        return restrictions.getBoolean(UserManager.DISALLOW_MODIFY_ACCOUNTS, false);
    }

    public static String getDisplayVersion() {
        return SystemProperties.get("ro.cm.display.version");
    }

    public static String getUniqueDeviceId(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CMAccount.SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
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
                            if (CMAccount.DEBUG) Log.d(TAG, "using wifi mac for id : " + buf.toString());
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

    public static byte[] digestBytes(String algorithm, byte[] bytes) {
        try {
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(bytes);
            return digest;
        } catch (Exception e) {
            return null;
        }
    }

    public static String digest(String algorithm, String id) {
        byte[] digestBytes = digestBytes(algorithm, id.getBytes());
        return encodeHex(digestBytes).toLowerCase().trim();
    }

    public static String encodeHex(byte[] bytes) {
        return new String(Hex.encodeHex(bytes));
    }

    public static String encodeHex(BigInteger integer) {
        return encodeHex(integer.toByteArray());
    }

    public static byte[] decodeHex(String hex) {
        try {
            return Hex.decodeHex(hex.toCharArray());
        } catch (DecoderException e) {
            Log.e(TAG, "Unable to decode hex string", e);
            throw new AssertionError(e);
        }
    }

    public static String getDeviceSalt(AccountManager accountManager, Account account) {
        if (account == null) {
            if (CMAccount.DEBUG) Log.d(TAG, "No CMAccount configured!");
            return null;
        }
        return accountManager.getUserData(account, CMAccount.ACCOUNT_EXTRA_DEVICE_SALT);
    }

    public static String getDeviceSalt(Context context) {
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account account = getCMAccountAccount(context);
        return getDeviceSalt(accountManager, account);
    }

    public static byte[] getHmacSecret(AccountManager accountManager, Account account) {
        if (account == null) {
            if (CMAccount.DEBUG) Log.d(TAG, "No CMAccount configured!");
            return null;
        }
        return Base64.decode(accountManager.getUserData(account,
                CMAccount.ACCOUNT_EXTRA_HMAC_SECRET), Base64.NO_WRAP);
    }

    public static byte[] getHmacSecret(Context context) {
        AccountManager accountManager = (AccountManager) context.getSystemService(Context.ACCOUNT_SERVICE);
        Account account = getCMAccountAccount(context);
        return getHmacSecret(accountManager, account);
    }

    public static void setMinimumAppVersion(Context context, int minimumVersion) {
        SharedPreferences prefs = context.getSharedPreferences(CMAccount.SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putInt(CMAccount.MINIMUM_APP_VERSION, minimumVersion);
        editor.commit();
    }

    public static int getMinimumAppVersion(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(CMAccount.SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        return prefs.getInt(CMAccount.MINIMUM_APP_VERSION, -1);
    }

    public static int getApplicationVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    public static void showIncompatibleVersionNotification(Context context) {
        Intent intent = new Intent(context, UpdateRequiredActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(context, 0, intent, 0);
        Notification notification = new Notification.Builder(context)
                .setContentTitle(context.getText(R.string.cmaccount_update_required_title))
                .setContentText(context.getText(R.string.cmaccount_update_required_short))
                .setSmallIcon(R.drawable.ic_dialog_alert)
                .setLargeIcon(((BitmapDrawable) context.getResources().getDrawable(R.drawable.icon)).getBitmap())
                .setContentIntent(contentIntent)
                .build();
        CMAccountUtils.showNotification(context, CMAccount.NOTIFICATION_ID_INCOMPATIBLE_VERSION, notification);
    }
}
