/*
 * Copyright (C) 2014 The CyanogenMod Project
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
package com.cyanogenmod.account.receiver;

import android.app.NotificationManager;
import android.app.StatusBarManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ThemeUtils;
import android.content.res.ThemeConfig;
import android.content.res.ThemeManager;
import android.provider.ThemesContract;
import android.util.Log;

import java.util.ArrayList;

public class ApplyHexoIconsReceiver extends BroadcastReceiver {

    private static final String TAG = ApplyHexoIconsReceiver.class.getSimpleName();
    public static final String HEXO_ICONS_PACKAGE_NAME = "com.cyngn.hexoicons";
    public static final int HEXO_NOTIFICATION_ID = 500;
    public static final String ACTION_APPLY_HEXO_ICONS =
            "com.cyanogenmod.account.intent.action.APPLY_HEXO_ICONS";

    public void onReceive(Context context, Intent intent) {
        if (ThemeUtils.getDefaultThemePackageName(context) != ThemeConfig.HOLO_DEFAULT
                && isPackageInstalled(context, HEXO_ICONS_PACKAGE_NAME)) {
            ArrayList<String> componentList = new ArrayList<String>();
            componentList.add(ThemesContract.ThemesColumns.MODIFIES_ICONS);

            ThemeManager themeManager = (ThemeManager)
                    context.getSystemService(Context.THEME_SERVICE);
            themeManager.requestThemeChange(HEXO_ICONS_PACKAGE_NAME, componentList);
        } else {
            Log.e(TAG,
                   "Hexoicons did not apply: Hexo Icons not installed, or Hexo Theme not default!");
        }

        NotificationManager notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(HEXO_NOTIFICATION_ID);

        StatusBarManager statusBarManager = (StatusBarManager)
                context.getSystemService(Context.STATUS_BAR_SERVICE);
        statusBarManager.collapsePanels();
    }

    public static boolean isPackageInstalled(Context context, String pkg) {
        if (pkg != null) {
            try {
                PackageInfo pi = context.getPackageManager().getPackageInfo(pkg, 0);
                if (!pi.applicationInfo.enabled) {
                    return false;
                }
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }

        return true;
    }

}
