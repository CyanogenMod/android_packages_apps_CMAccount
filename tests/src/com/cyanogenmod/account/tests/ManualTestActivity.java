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

package com.cyanogenmod.account.tests;

import com.cyanogenmod.account.CMAccount;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;

public class ManualTestActivity extends Activity {

    private AccountManager mAccountManager;
    private Account mSelectedAccount;
    private Spinner mSpinner;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cmaccount_test);
        mSpinner = (Spinner) findViewById(R.id.accounts);
        mAccountManager = AccountManager.get(this);
        final Account[] accounts = mAccountManager.getAccountsByType(CMAccount.ACCOUNT_TYPE_CMAccount);
        ArrayAdapter<Account> adapter = new ArrayAdapter<Account>(this, android.R.layout.simple_list_item_1, android.R.id.text1, accounts);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mSelectedAccount = accounts[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        findViewById(R.id.get_token).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getToken();
            }
        });
        findViewById(R.id.invalidate_token).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                invalidateAuthToken();
            }
        });
        findViewById(R.id.expire_token).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                expireAuthToken();
            }
        });
        findViewById(R.id.enable_setup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableSetup();
            }
        });
        findViewById(R.id.enable_google_setup).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                enableGoogleSetup();
            }
        });
    }

    private void getToken() {
        mAccountManager.getAuthToken(mSelectedAccount, CMAccount.AUTHTOKEN_TYPE_ACCESS, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture) {
                try {
                    Bundle bundle =  bundleAccountManagerFuture.getResult();
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    Log.d("AccountManagerTest", "token=" + token);
                    Toast.makeText(ManualTestActivity.this, "AuthToken\n" + token, Toast.LENGTH_SHORT).show();
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                }
            }
        }, new Handler());
    }

    private void invalidateAuthToken() {
        String token = mAccountManager.peekAuthToken(mSelectedAccount, CMAccount.AUTHTOKEN_TYPE_ACCESS);
        if (token != null) {
            mAccountManager.invalidateAuthToken(CMAccount.AUTHTOKEN_TYPE_ACCESS, token);
            Toast.makeText(ManualTestActivity.this, "AuthToken\n" + token + "\nInvalidated", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(ManualTestActivity.this, "No AuthToken", Toast.LENGTH_SHORT).show();
        }
    }

    private void expireAuthToken() {
        mAccountManager.setUserData(mSelectedAccount, CMAccount.AUTHTOKEN_EXPIRES_IN, "0");
        String token = mAccountManager.peekAuthToken(mSelectedAccount, CMAccount.AUTHTOKEN_TYPE_ACCESS);
        if (token != null) {
            mAccountManager.invalidateAuthToken(CMAccount.AUTHTOKEN_TYPE_ACCESS, token);
        }
        Toast.makeText(ManualTestActivity.this, "AuthToken expired", Toast.LENGTH_SHORT).show();
    }

    private void enableSetup() {
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0);
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 0);
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        final PackageManager pm = getPackageManager();
        ComponentName componentName = new ComponentName("com.cyanogenmod.account", "com.cyanogenmod.account.ui.SetupWizardActivity");
        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        componentName = new ComponentName("com.google.android.setupwizard", "com.google.android.setupwizard.SetupWizardActivity");
        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | intent.getFlags());
        startActivity(intent);
        finish();
    }

    private void enableGoogleSetup() {
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 0);
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 0);
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        final PackageManager pm = getPackageManager();
        ComponentName componentName = new ComponentName("com.google.android.setupwizard", "com.google.android.setupwizard.SetupWizardActivity");
        pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | intent.getFlags());
        startActivity(intent);
        finish();
    }

}