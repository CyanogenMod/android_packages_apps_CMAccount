package com.cyanogenmod.id;

public class Constants {

    public static final String TAG = "CMID";
    public static final boolean DEBUG = true;

    public static final String ACCOUNT_TYPE = "com.cyanogenmod.id";
    public static final String AUTHTOKEN_TYPE_ACCESS = "com.cyanogenmod.id";
    public static final String AUTHTOKEN_TYPE_REFRESH = "com.cyanogenmod.id.auth.refresh_token";
    public static final String AUTHTOKEN_EXPIRES_IN= "com.cyanogenmod.id.auth.expires_in";

    public static final String ACTION_SETUP_GOOGLE_ACCOUNT = "com.google.android.accounts.AccountIntro";
    public static final String ACTION_SETUP_WIFI = "com.android.net.wifi.SETUP_WIFI_NETWORK";

    public static final String EXTRA_FIRST_RUN = "firstRun";
    public static final String EXTRA_SHOW_BUTTON_BAR = "extra_prefs_show_button_bar";
    public static final String EXTRA_SHOW_WIFI_MENU = "wifi_show_menus";

    public static final String GCM_PREFERENCES = "com.cyanogenmod.id.gcm";
    public static final String AUTH_PREFERENCES = "com.cyanogenmod.id.auth";
    public static final String SETTINGS_PREFERENCES = "com.cyanogenmod.id_preferences";

    public static final String KEY_FIND_DEVICE_PREF = "find_device";

    public static final String BACKOFF_MS = "backoff_ms";
    public static final int DEFAULT_BACKOFF_MS = 3000;
    public static final int MAX_BACKOFF_MS = 1000 * 60 * 60; // 1 hour

}
