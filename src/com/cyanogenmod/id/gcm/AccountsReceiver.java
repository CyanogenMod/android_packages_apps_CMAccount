package com.cyanogenmod.id.gcm;

import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.admin.DeviceAdminReceiver;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class AccountsReceiver extends BroadcastReceiver {

    private static final String TAG = AccountsReceiver.class.getSimpleName();

    public void onReceive(Context context, Intent intent) {

        if (CMID.DEBUG) Log.d(TAG, "action = " + intent.getAction());

        final AccountManager am = AccountManager.get(context);

        if (!CMIDUtils.getCMIDAccountAdded(context)) {
            if (am.getAccountsByType(CMID.ACCOUNT_TYPE_CMID).length > 0) {
                CMIDUtils.setCMIDAccountAdded(context, true);
                final Account account = CMIDUtils.getCMIDAccount(context);
                CMIDUtils.resetBackoff(GCMUtil.getGCMPreferences(context));
                GCMService.registerClient(context, account);
            }
        } else {
            if (am.getAccountsByType(CMID.ACCOUNT_TYPE_CMID).length == 0) {
                CMIDUtils.setCMIDAccountAdded(context, false);
                CMIDUtils.resetBackoff(AuthClient.getInstance(context).getAuthPreferences());
            }
        }
        if (!CMIDUtils.getGoogleAccountAdded(context)) {
            if (am.getAccountsByType(CMID.ACCOUNT_TYPE_GOOGLE).length > 0) {
                CMIDUtils.setGoogleAccountAdded(context, true);
                final Account account = CMIDUtils.getCMIDAccount(context);
                CMIDUtils.resetBackoff(GCMUtil.getGCMPreferences(context));
                GCMService.registerClient(context, account);
            }
        } else {
            if (am.getAccountsByType(CMID.ACCOUNT_TYPE_GOOGLE).length == 0) {
                CMIDUtils.setGoogleAccountAdded(context, false);
            }
        }

    }

    public static class CMIDAdminReceiver extends DeviceAdminReceiver {}
}
