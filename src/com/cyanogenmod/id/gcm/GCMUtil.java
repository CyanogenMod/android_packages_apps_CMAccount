package com.cyanogenmod.id.gcm;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.api.DeviceFinderService;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.Account;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.SystemClock;
import android.os.SystemProperties;
import android.util.Log;

import java.sql.Timestamp;

public class GCMUtil {

    private static final String TAG = GCMUtil.class.getSimpleName();

    public static final String SENDER_ID = "935225172180";

    private static final String PROPERTY_REG_ID = "registration_id";
    private static final String PROPERTY_APP_VERSION = "appVersion";
    private static final String PROPERTY_ON_SERVER_EXPIRATION_TIME = "onServerExpirationTimeMs";
    // 2 days
    private static final long REGISTRATION_EXPIRY_TIME_MS = 1000 * 3600 * 24 * 2;
    // 1 day
    private static final long RE_REGISTRATION_INTERVAL = 1000 * 3600 * 24;

    public static final String COMMAND_KEY_EXCHANGE = "key_exchange";


    static void reportLocation(Context context) {
        Account account = CMIDUtils.getCMIDAccount(context);
        if (account != null) {
            DeviceFinderService.reportLocation(context, account);
        }
    }

    public static void registerForGCM(Context context) {
        CMIDUtils.resetBackoff(GCMUtil.getGCMPreferences(context));
        GCMService.registerClient(context);
    }

    public static boolean isRegistrationExpired(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        long expirationTime =
                prefs.getLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, 0);
        return System.currentTimeMillis() > expirationTime;
    }

     public static String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.length() == 0) {
            if (CMID.DEBUG) Log.d(TAG, "Registration not found.");
            return "";
        }
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            if (CMID.DEBUG) Log.d(TAG, "App version changed");
            return "";
        }
        return registrationId;
    }

    static void setRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        if (CMID.DEBUG) Log.d(TAG, "Saving regId on app version " + appVersion + " regId " + regId);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
        long expirationTime = System.currentTimeMillis() + (regId.length() == 0 ? 0 : REGISTRATION_EXPIRY_TIME_MS);
        if (CMID.DEBUG) Log.d(TAG, "Setting registration expiry time to " + new Timestamp(expirationTime));
        editor.putLong(PROPERTY_ON_SERVER_EXPIRATION_TIME, expirationTime);
        editor.commit();
    }

    static void clearRegistrationId(Context context) {
        setRegistrationId(context, "");
    }

    private static int getAppVersion(Context context) {
        try {
            PackageInfo packageInfo = context.getPackageManager()
                    .getPackageInfo(context.getPackageName(), 0);
            return packageInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException("Could not get package name: " + e);
        }
    }

    static SharedPreferences getGCMPreferences(Context context) {
        return context.getSharedPreferences(CMID.GCM_PREFERENCES,
                Context.MODE_PRIVATE);
    }

    static void scheduleGCMReRegister(Context context, Intent intent) {
        if (CMID.DEBUG) Log.d(TAG, "Scheduling re gcm register, starting = " +
                new Timestamp(SystemClock.elapsedRealtime() + RE_REGISTRATION_INTERVAL) + " interval (" + RE_REGISTRATION_INTERVAL + ")" + intent.getAction());
        PendingIntent reRegisterPendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + RE_REGISTRATION_INTERVAL, RE_REGISTRATION_INTERVAL,
                reRegisterPendingIntent);
    }

    static void cancelGCMReRegister(Context context, Intent intent) {
        if (CMID.DEBUG) Log.d(TAG, "Canceling gcm register " + intent.getAction());
        PendingIntent reRegisterPendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(reRegisterPendingIntent);
    }

    public static boolean googleServicesExist(Context context) {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) != ConnectionResult.SERVICE_MISSING;
    }
    public static boolean playServicesUpdateRequired(Context context) {
        return GooglePlayServicesUtil.isGooglePlayServicesAvailable(context) == ConnectionResult.SERVICE_VERSION_UPDATE_REQUIRED;
    }
}
