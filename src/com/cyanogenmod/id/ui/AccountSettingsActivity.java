package com.cyanogenmod.id.ui;

import com.cyanogenmod.id.R;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.Account;
import android.app.Activity;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.os.Bundle;

public class AccountSettingsActivity extends Activity {

    private FragmentManager mFragmentManager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.account_settings);
        mFragmentManager = getFragmentManager();
        if (findViewById(R.id.account_settings_container) != null) {
            if (savedInstanceState != null) {
                return;
            }
            Account account = CMIDUtils.getCMIDAccount(this);
            AccountSettingsFragment accountSettingsFragment = new AccountSettingsFragment();
            Bundle args = new Bundle();
            args.putParcelable(AccountSettingsFragment.BUNDLE_KEY_ACCOUNT, account);
            accountSettingsFragment.setArguments(args);
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.replace(R.id.account_settings_container, accountSettingsFragment);
            transaction.commit();
        }
    }
}