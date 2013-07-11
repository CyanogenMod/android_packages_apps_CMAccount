package com.cyanogenmod.id.ui;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;

import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.gcm.GCMService;

import android.accounts.Account;
import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;

public class AccountSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {


    private static final String TAG = AccountSettingsFragment.class.getSimpleName();

    public static final String BUNDLE_KEY_ACCOUNT = "account";

    private Account mAccount;

    private SwitchPreference mDeviceFinderPreference;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.account_settings_preferences);
        mDeviceFinderPreference = (SwitchPreference)findPreference(Constants.KEY_FIND_DEVICE_PREF);
        checkForPlayServices();
        Bundle b = getArguments();
        if (b == null || b.get(BUNDLE_KEY_ACCOUNT) == null) {
            throw new IllegalArgumentException("Must provide an Account");
        }
        mAccount = (Account)b.get(BUNDLE_KEY_ACCOUNT);
        mDeviceFinderPreference.setOnPreferenceChangeListener(this);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object value) {
        if (preference.getKey().equals(Constants.KEY_FIND_DEVICE_PREF)) {
            if ((Boolean)value) {
                GCMService.registerClient(getActivity(), mAccount);
            } else {
                GCMService.unregisterClient(getActivity(), mAccount);
            }
        }
        return true;
    }

    private void checkForPlayServices() {
        int playServiceStatus = GooglePlayServicesUtil.isGooglePlayServicesAvailable(getActivity());
        mDeviceFinderPreference.setEnabled(playServiceStatus == ConnectionResult.SUCCESS);
    }
}