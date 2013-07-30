package com.cyanogenmod.id.gcm;

import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.OnAccountsUpdateListener;
import android.app.admin.DeviceAdminReceiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;

public class AccountsReceiver extends BroadcastReceiver {

    private static final String TAG = AccountsReceiver.class.getSimpleName();

    public void onReceive(Context context, Intent intent) {
        final String action = intent.getAction();
        if (CMID.DEBUG) Log.d(TAG, "action = " + action);
        if (CMIDUtils.getCMIDAccount(context) == null) {
            if (CMID.DEBUG) Log.d(TAG, "No CMID Configured!");
            return;
        }

        if (action.equals(Intent.ACTION_PACKAGE_ADDED)) {
            Bundle b = intent.getExtras();
            if (b != null) {
                int uid = b.getInt(Intent.EXTRA_UID);
                boolean playServicesAdded = false;
                String[] packages = context.getPackageManager().getPackagesForUid(uid);
                for (String app : packages) {
                    if (app.equals("com.google.android.gms")) {
                        if (CMID.DEBUG) Log.d(TAG, "Play Services package added! Registering for GCM");
                        playServicesAdded = true;
                    }
                }
                if (playServicesAdded) {
                    registerForGCM(context);
                }
            }
            return;
        }

        if (GCMUtil.isRegistrationExpired(context)) {
            registerForGCM(context);
        }
    }


    private void registerForGCM(Context context) {
        CMIDUtils.resetBackoff(GCMUtil.getGCMPreferences(context));
        GCMService.registerClient(context);
    }

    public static class CMIDAdminReceiver extends DeviceAdminReceiver {}
}
