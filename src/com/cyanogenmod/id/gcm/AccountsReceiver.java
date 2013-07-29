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

        if (CMID.DEBUG) Log.d(TAG, "action = " + intent.getAction());

        Account account = CMIDUtils.getCMIDAccount(context);
        if (account == null) {
            if (CMID.DEBUG) Log.d(TAG, "No CMID Configured!");
            return;
        }
        if (GCMUtil.isRegistrationExpired(context)) {
            CMIDUtils.resetBackoff(GCMUtil.getGCMPreferences(context));
            GCMService.registerClient(context);
        }
    }

    public static class CMIDAdminReceiver extends DeviceAdminReceiver {}
}
