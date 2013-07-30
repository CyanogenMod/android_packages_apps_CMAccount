package com.cyanogenmod.id.gcm;

import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.util.CMIDUtils;

import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.util.Log;

public class GCMPackageReceiver extends BroadcastReceiver {

    private static final String TAG = GCMPackageReceiver.class.getSimpleName();

    public void onReceive(Context context, Intent intent) {
        if (!GCMUtil.googleServicesExist(context)) {
            if (CMID.DEBUG) Log.d(TAG, "Google services not installed. skipping...");
            return;
        }
        final boolean needsUpgrade = GCMUtil.playServicesUpdateRequired(context);
        if (CMID.DEBUG) Log.d(TAG, "Play Services needs upgrade = " + needsUpgrade);
        if (!needsUpgrade) {
            if (CMIDUtils.getCMIDAccount(context) != null) {
                GCMUtil.registerForGCM(context);
            }
            if (CMID.DEBUG) Log.d(TAG, "Play Services upgraded, disabling receiver");
            final PackageManager pm = context.getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(context, GCMPackageReceiver.this.getClass()), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

}
