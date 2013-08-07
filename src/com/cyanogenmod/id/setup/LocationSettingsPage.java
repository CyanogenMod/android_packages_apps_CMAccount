package com.cyanogenmod.id.setup;


import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.R;

import android.app.Activity;
import android.app.Fragment;
import android.content.ContentQueryMap;
import android.content.ContentResolver;
import android.content.Context;
import android.database.Cursor;
import android.location.LocationManager;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.preference.SwitchPreference;
import android.provider.Settings;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.Observable;
import java.util.Observer;

public class LocationSettingsPage extends Page {

    private static final String KEY_LOCATION_TOGGLE = "location_toggle";
    private static final String KEY_LOCATION_NETWORK = "location_network";
    private static final String KEY_LOCATION_GPS = "location_gps";

    private static final String TAG = LocationSettingsPage.class.getSimpleName();

    public LocationSettingsPage(Context context, SetupDataCallbacks callbacks, int titleResourceId) {
        super(context, callbacks, titleResourceId);
    }

    @Override
    public Fragment createFragment() {
        Bundle args = new Bundle();
        args.putString(Page.KEY_PAGE_ARGUMENT, getKey());

        LocationSettingsFragment fragment = new LocationSettingsFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getNextButtonResId() {
        return R.string.next;
    }

    // Logic kanged from settings app
    public static class LocationSettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {

        private CheckBoxPreference mNetwork;
        private CheckBoxPreference mGps;
        private SwitchPreference mLocationAccess;

        private SetupDataCallbacks mCallbacks;
        private String mKey;
        private Page mPage;
        private View mRootView;

        // These provide support for receiving notification when Location Manager settings change.
        // This is necessary because the Network Location Provider can change settings
        // if the user does not confirm enabling the provider.
        private ContentQueryMap mContentQueryMap;

        private Observer mSettingsObserver;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            Bundle args = getArguments();
            mKey = args.getString(Page.KEY_PAGE_ARGUMENT);
            if (mKey == null) {
                throw new IllegalArgumentException("No KEY_PAGE_ARGUMENT given");
            }
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                Bundle savedInstanceState) {
            mRootView = inflater.inflate(R.layout.setup_preference_page, container, false);
            TextView titleView = (TextView) mRootView.findViewById(android.R.id.title);
            titleView.setText(mKey);
            return mRootView;
        }

        @Override
        public void onActivityCreated(Bundle savedInstanceState) {
            super.onActivityCreated(savedInstanceState);
            mPage = mCallbacks.getPage(mKey);
        }

        @Override
        public void onAttach(Activity activity) {
            super.onAttach(activity);
            if (!(activity instanceof SetupDataCallbacks)) {
                throw new ClassCastException("Activity implement SetupDataCallbacks");
            }
            mCallbacks = (SetupDataCallbacks) activity;
        }

        @Override
        public void onDetach() {
            super.onDetach();
            mCallbacks = null;
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
            }
            return true;
        }

        private PreferenceScreen createPreferenceHierarchy() {
            PreferenceScreen root = getPreferenceScreen();
            if (root != null) {
                root.removeAll();
            }
            addPreferencesFromResource(R.xml.location_settings_preferences);
            root = getPreferenceScreen();
            mLocationAccess = (SwitchPreference)root.findPreference(KEY_LOCATION_TOGGLE);
            mNetwork = (CheckBoxPreference) root.findPreference(KEY_LOCATION_NETWORK);
            mGps = (CheckBoxPreference) root.findPreference(KEY_LOCATION_GPS);

            mLocationAccess.setOnPreferenceChangeListener(this);
            return root;
        }


        private void updateLocationToggles() {
            ContentResolver res = getActivity().getContentResolver();
            boolean gpsEnabled = Settings.Secure.isLocationProviderEnabled(
                    res, LocationManager.GPS_PROVIDER);
            boolean networkEnabled = Settings.Secure.isLocationProviderEnabled(
                    res, LocationManager.NETWORK_PROVIDER);
            mGps.setChecked(gpsEnabled);
            mNetwork.setChecked(networkEnabled);
            mLocationAccess.setChecked(gpsEnabled || networkEnabled);
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
}
