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

package com.cyanogenmod.account;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.account.api.response.GetMinimumAppVersionResponse;
import com.cyanogenmod.account.auth.AuthClient;

import android.app.Application;
import android.app.StatusBarManager;
import android.app.admin.DeviceAdminReceiver;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;

import com.cyanogenmod.account.encryption.ECDHKeyService;
import com.cyanogenmod.account.util.CMAccountUtils;

public class CMAccount extends Application implements Response.Listener<GetMinimumAppVersionResponse>, Response.ErrorListener {

    public static final String TAG = "CMAccount";
    // Leave this off for release
    public static final boolean DEBUG = false;

    public static final String ACCOUNT_TYPE_CMAccount = "com.cyanogenmod.account";
    public static final String AUTHTOKEN_TYPE_ACCESS = "com.cyanogenmod.account";
    public static final String AUTHTOKEN_EXPIRES_IN= "com.cyanogenmod.account.auth.expires_in";
    public static final String ACCOUNT_EXTRA_DEVICE_SALT = "com.cyanogenmod.account.auth.device_salt";
    public static final String ACCOUNT_EXTRA_HMAC_SECRET = "com.cyanogenmod.account.auth.hmac_secret";

    public static final String ACTION_SETUP_WIFI = "com.android.net.wifi.SETUP_WIFI_NETWORK";
    public static final String WIFI_COMPONENT_PKG = "com.android.settings";
    public static final String WIFI_COMPONENT_CLASS = "com.android.settings.wifi.WifiSettings";

    public static final String EXTRA_FIRST_RUN = "firstRun";
    public static final String EXTRA_ALLOW_SKIP = "allowSkip";
    public static final String EXTRA_SHOW_SKIP = "extra_prefs_show_button_bar";
    public static final String EXTRA_AUTO_FINISH = "wifi_auto_finish_on_connect";
    public static final String EXTRA_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";
    public static final String EXTRA_PREF_BACK_TEXT = "extra_prefs_set_back_text";
    public static final String EXTRA_ONLY_ACCESS_POINTS = "only_access_points";

    public static final String GCM_PREFERENCES = "com.cyanogenmod.account.gcm";
    public static final String AUTH_PREFERENCES = "com.cyanogenmod.account.auth";
    public static final String SETTINGS_PREFERENCES = "com.cyanogenmod.account_preferences";
    public static final String ENCRYPTION_PREFERENCES = "com.cyanogenmod.account.encryption";

    public static final String BACKOFF_MS = "backoff_ms";
    public static final int DEFAULT_BACKOFF_MS = 3000;
    public static final int MAX_BACKOFF_MS = 1000 * 60 * 60 * 6; // 6 hours
    public static final String MINIMUM_APP_VERSION = "minimum_app_version";

    public static final int REQUEST_CODE_SETUP_WIFI = 0;
    public static final int REQUEST_CODE_SETUP_CMAccount = 1;

    public static final int NOTIFICATION_ID_PASSWORD_RESET = 666;
    public static final int NOTIFICATION_ID_INCOMPATIBLE_VERSION = 667;

    private String mCMAccountUri;

    private StatusBarManager mStatusBarManager;

    @Override
    public void onCreate() {
        super.onCreate();
        mCMAccountUri = getString(R.string.cmaccount_uri);
        mStatusBarManager = (StatusBarManager)getSystemService(Context.STATUS_BAR_SERVICE);
        final DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        final ComponentName deviceAdmin = new ComponentName(getApplicationContext(), CMAccountAdminReceiver.class);
        dpm.setActiveAdmin(deviceAdmin, true);
        //Warm the auth client instance
        AuthClient authClient = AuthClient.getInstance(getApplicationContext());
        // Warm ECDH public keys
        ECDHKeyService.startGenerateNoUpload(getApplicationContext());
        // Check minimum required app version
        if (CMAccountUtils.isNetworkConnected(getApplicationContext())) {
            authClient.getMinimumAppVersion(this, this);
        }
    }

    public String getCMAccountUri() {
        return mCMAccountUri;
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        // noop
    }

    @Override
    public void onResponse(GetMinimumAppVersionResponse getMinimumAppVersionResponse) {
        int minimumVersion = getMinimumAppVersionResponse.getVersion();
        CMAccountUtils.setMinimumAppVersion(getApplicationContext(), minimumVersion);
        if (CMAccountUtils.getApplicationVersion(getApplicationContext()) < minimumVersion) {
            CMAccountUtils.showIncompatibleVersionNotification(getApplicationContext());
        }
    }

    public static class CMAccountAdminReceiver extends DeviceAdminReceiver {}

}
