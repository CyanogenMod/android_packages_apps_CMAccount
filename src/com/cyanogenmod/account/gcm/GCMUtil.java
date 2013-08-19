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

package com.cyanogenmod.account.gcm;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.api.DeviceFinderService;
import com.cyanogenmod.account.util.CMAccountUtils;

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

    public static final String COMMAND_KEY_EXCHANGE = "key_exchange";
    public static final String COMMAND_KEY_EXCHANGE_FAILED = "key_exchange_failed";
    public static final String COMMAND_SECURE_MESSAGE = "secure_message";

    public static final String COMMAND_PASSWORD_RESET = "password_reset";
    public static final String COMMAND_LOCATE = "begin_locate";
    public static final String COMMAND_WIPE = "begin_wipe";


    static void reportLocation(Context context, String sessionId) {
        Account account = CMAccountUtils.getCMAccountAccount(context);
        if (account != null) {
            DeviceFinderService.reportLocation(context, account, sessionId);
        }
    }

    public static void registerForGCM(Context context) {
        CMAccountUtils.resetBackoff(GCMUtil.getGCMPreferences(context));
        GCMService.registerClient(context);
    }

     public static String getRegistrationId(Context context) {
        final SharedPreferences prefs = getGCMPreferences(context);
        String registrationId = prefs.getString(PROPERTY_REG_ID, "");
        if (registrationId.length() == 0) {
            if (CMAccount.DEBUG) Log.d(TAG, "Registration not found.");
            return "";
        }
        int registeredVersion = prefs.getInt(PROPERTY_APP_VERSION, Integer.MIN_VALUE);
        int currentVersion = getAppVersion(context);
        if (registeredVersion != currentVersion) {
            if (CMAccount.DEBUG) Log.d(TAG, "App version changed");
            return "";
        }
        return registrationId;
    }

    static void setRegistrationId(Context context, String regId) {
        final SharedPreferences prefs = getGCMPreferences(context);
        int appVersion = getAppVersion(context);
        if (CMAccount.DEBUG) Log.d(TAG, "Saving regId on app version " + appVersion + " regId " + regId);
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString(PROPERTY_REG_ID, regId);
        editor.putInt(PROPERTY_APP_VERSION, appVersion);
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
        return context.getSharedPreferences(CMAccount.GCM_PREFERENCES,
                Context.MODE_PRIVATE);
    }

    static void scheduleGCMReRegister(Context context, Intent intent) {
        if (CMAccount.DEBUG) Log.d(TAG, "Scheduling re gcm register, starting = " +
                new Timestamp(SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_DAY) + " interval (" + AlarmManager.INTERVAL_DAY + ")" + intent.getAction());
        PendingIntent reRegisterPendingIntent = PendingIntent.getService(context, 0, intent, PendingIntent.FLAG_CANCEL_CURRENT);
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.setInexactRepeating(AlarmManager.ELAPSED_REALTIME,
                SystemClock.elapsedRealtime() + AlarmManager.INTERVAL_DAY, AlarmManager.INTERVAL_DAY,
                reRegisterPendingIntent);
    }

    static void cancelGCMReRegister(Context context, Intent intent) {
        if (CMAccount.DEBUG) Log.d(TAG, "Canceling gcm register " + intent.getAction());
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
