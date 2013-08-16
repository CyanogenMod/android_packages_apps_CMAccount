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

import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.util.CMAccountUtils;

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
            if (CMAccount.DEBUG) Log.d(TAG, "Google services not installed. skipping...");
            return;
        }
        final boolean needsUpgrade = GCMUtil.playServicesUpdateRequired(context);
        if (CMAccount.DEBUG) Log.d(TAG, "Play Services needs upgrade = " + needsUpgrade);
        if (!needsUpgrade) {
            if (CMAccountUtils.getCMAccountAccount(context) != null) {
                GCMUtil.registerForGCM(context);
            }
            if (CMAccount.DEBUG) Log.d(TAG, "Play Services upgraded, disabling receiver");
            final PackageManager pm = context.getPackageManager();
            pm.setComponentEnabledSetting(new ComponentName(context, GCMPackageReceiver.this.getClass()), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
        }
    }

}
