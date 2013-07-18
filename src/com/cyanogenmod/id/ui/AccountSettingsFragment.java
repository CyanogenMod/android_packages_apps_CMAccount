package com.cyanogenmod.id.ui;

import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.gcm.GCMService;
import com.cyanogenmod.id.gcm.GCMUtil;

import android.accounts.Account;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;

import java.util.Observable;
import java.util.Observer;

public class AccountSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {


    private static final String TAG = AccountSettingsFragment.class.getSimpleName();

    public static final String BUNDLE_KEY_ACCOUNT = "account";
    private static final String KEY_LOCATION_NETWORK = "location_network";
    private static final String KEY_LOCATION_GPS = "location_gps";

    private Account mAccount;

    private SwitchPreference mDeviceFinderPreference;
    private CheckBoxPreference mNetwork;
    private CheckBoxPreference mGps;

    // These provide support for receiving notification when Location Manager settings change.
    // This is necessary because the Network Location Provider can change settings
    // if the user does not confirm enabling the provider.
    private ContentQueryMap mContentQueryMap;

    private Observer mSettingsObserver;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle b = getArguments();
        if (b == null || b.get(BUNDLE_KEY_ACCOUNT) == null) {
            throw new IllegalArgumentException("Must provide an Account");
        }
        mAccount = (Account)b.get(BUNDLE_KEY_ACCOUNT);
    }

    @Override
    public void onStart() {
        super.onStart();
        // listen for Location Manager settings changes
        Cursor settingsCursor = getActivity().getContentResolver().query(Settings.Secure.CONTENT_URI, null,
                "(" + Settings.System.NAME + "=?)",
                new String[]{Settings.Secure.LOCATION_PROVIDERS_ALLOWED},
                null);
        mContentQueryMap = new ContentQueryMap(settingsCursor, Settings.System.NAME, true, null);
    }

    @Override
    public void onResume() {
        super.onResume();

        // Make sure we reload the preference hierarchy since some of these settings
        // depend on others...
        createPreferenceHierarchy();
        updateLocationToggles();

        if (mSettingsObserver == null) {
            mSettingsObserver = new Observer() {
                public void update(Observable o, Object arg) {
                    updateLocationToggles();
                }
            };
        }

        mContentQueryMap.addObserver(mSettingsObserver);
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mSettingsObserver != null) {
            mContentQueryMap.deleteObserver(mSettingsObserver);
        }
        mContentQueryMap.close();
    }

    @Override
    public boolean onPreferenceTreeClick(PreferenceScreen preferenceScreen, Preference preference) {
        final ContentResolver cr = getActivity().getContentResolver();
        if (preference == mNetwork) {
            Settings.Secure.setLocationProviderEnabled(cr,
                    LocationManager.NETWORK_PROVIDER, mNetwork.isChecked());
        } else if (preference == mGps) {
            boolean enabled = mGps.isChecked();
            Settings.Secure.setLocationProviderEnabled(cr,
                    LocationManager.GPS_PROVIDER, enabled);
        } else {
            // If we didn't handle it, let preferences handle it.
            return super.onPreferenceTreeClick(preferenceScreen, preference);
        }
        return true;
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference.getKey().equals(CMID.KEY_FIND_DEVICE_PREF)) {
            onToggleLocationAccess((Boolean) value);
            if ((Boolean)value) {
                GCMService.registerClient(getActivity(), mAccount);
            } else {
                GCMService.unregisterClient(getActivity(), mAccount);
            }
        }
        return true;
    }
    private PreferenceScreen createPreferenceHierarchy() {
        PreferenceScreen root = getPreferenceScreen();
        if (root != null) {
            root.removeAll();
        }
        addPreferencesFromResource(R.xml.account_settings_preferences);
        root = getPreferenceScreen();
        mDeviceFinderPreference = (SwitchPreference)root.findPreference(CMID.KEY_FIND_DEVICE_PREF);
        mNetwork = (CheckBoxPreference) root.findPreference(KEY_LOCATION_NETWORK);
        mGps = (CheckBoxPreference) root.findPreference(KEY_LOCATION_GPS);

        mDeviceFinderPreference.setOnPreferenceChangeListener(this);
        checkForPlayServices();
        return root;
    }

    private void checkForPlayServices() {
        mDeviceFinderPreference.setEnabled(GCMUtil.googleServicesExist(getActivity()));
    }

    private void updateLocationToggles() {
        ContentResolver res = getActivity().getContentResolver();
        boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.GPS_PROVIDER);
        boolean networkEnabled = Settings.Secure.isLocationProviderEnabled(
                res, LocationManager.NETWORK_PROVIDER);
        mGps.setChecked(gpsEnabled);
        mNetwork.setChecked(networkEnabled);
        mDeviceFinderPreference.setChecked(gpsEnabled || networkEnabled);
    }

    private void onToggleLocationAccess(boolean checked) {
        final ContentResolver cr = getActivity().getContentResolver();
        Settings.Secure.setLocationProviderEnabled(cr,
                LocationManager.GPS_PROVIDER, checked);
        Settings.Secure.setLocationProviderEnabled(cr,
                LocationManager.NETWORK_PROVIDER, checked);
        updateLocationToggles();
    }
}