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

package com.cyanogenmod.account.auth;

import com.android.volley.VolleyError;
import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.R;
import com.cyanogenmod.account.api.AuthTokenResponse;
import com.cyanogenmod.account.ui.CMAccountActivity;

import android.accounts.AbstractAccountAuthenticator;
import android.accounts.Account;
import android.accounts.AccountAuthenticatorResponse;
import android.accounts.AccountManager;
import android.accounts.NetworkErrorException;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;


public class Authenticator extends AbstractAccountAuthenticator {

    private static final String TAG = Authenticator.class.getSimpleName();
    private final Context mContext;
    private AuthClient mAuthClient;
    private AccountManager mAccountManager;

    private final Handler mHandler = new Handler();

    public Authenticator(Context context) {
        super(context);
        mContext = context;
        mAuthClient = AuthClient.getInstance(context);
        mAccountManager = AccountManager.get(mContext);
    }

    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        if (CMAccount.DEBUG) Log.d(TAG, "addAccount()");
        int accounts = mAccountManager.getAccountsByType(accountType).length;
        final Bundle bundle = new Bundle();
        if (accounts > 0) {
            final String error = mContext.getString(R.string.cmaccount_error_multiple_accounts);
            bundle.putInt(AccountManager.KEY_ERROR_CODE, AccountManager.ERROR_CODE_UNSUPPORTED_OPERATION);
            bundle.putString(AccountManager.KEY_ERROR_MESSAGE, error);
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, error, Toast.LENGTH_SHORT).show();
                }
            });
            return bundle;
        } else {
            final Intent intent = new Intent(mContext, CMAccountActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, CMAccount.ACCOUNT_TYPE_CMAccount);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }
    }

    @Override
    public Bundle editProperties(AccountAuthenticatorResponse response, String accountType) {
        return null;
    }

    @Override
    public Bundle confirmCredentials(AccountAuthenticatorResponse response, Account account, Bundle options) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle getAuthToken(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle loginOptions) throws NetworkErrorException {
        if (CMAccount.DEBUG) Log.d(TAG, "getAuthToken() account="+account.name+ " type="+account.type);
        if (!authTokenType.equals(CMAccount.AUTHTOKEN_TYPE_ACCESS)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }
        if (hasRefreshToken(mAccountManager, account)) {
            if (CMAccount.DEBUG) Log.d(TAG, "refreshing token... account="+account.name+ " type="+account.type);
            Bundle bundle = refreshToken(mAccountManager, account, response);
            if (bundle != null) {
                return bundle;
            }
            return bundle;
        }
        final String password = mAccountManager.getPassword(account);
        if (password != null) {
            if (CMAccount.DEBUG) Log.d(TAG, "authenticating account="+account.name+ " type="+account.type);
            Bundle bundle = login(mAccountManager, account, password, response);
            if (bundle != null) {
                return bundle;
            }
        }

        final Bundle result = new Bundle();
        final Intent intent = new Intent(mContext, AuthActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        result.putParcelable(AccountManager.KEY_INTENT, intent);
        return result;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle loginOptions) throws NetworkErrorException {
        final Bundle result = new Bundle();
        final Intent intent = new Intent(mContext, AuthActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        result.putParcelable(AccountManager.KEY_INTENT, intent);
        return result;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    private Bundle login(AccountManager am, Account account, String password, AccountAuthenticatorResponse response) {
        final String accountName = account.name;
        try {
            AuthTokenResponse authResponse = mAuthClient.blockingLogin(accountName, password);
            mAuthClient.updateLocalAccount(am, account, authResponse);
            final String token = authResponse.getAccessToken();
            if (!TextUtils.isEmpty(token)) {
                final Bundle result = new Bundle();
                result.putParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
                result.putString(AccountManager.KEY_AUTHTOKEN, token);
                result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                result.putString(AccountManager.KEY_ACCOUNT_TYPE, CMAccount.ACCOUNT_TYPE_CMAccount);
                am.setAuthToken(account, CMAccount.AUTHTOKEN_TYPE_ACCESS, token);
                return result;
            }
        } catch (VolleyError volleyError) {
            volleyError.printStackTrace();
        }
        return null;
    }

    private Bundle refreshToken(AccountManager am, Account account, AccountAuthenticatorResponse response) {
        final String refreshToken = am.getUserData(account, CMAccount.AUTHTOKEN_TYPE_REFRESH);
        if (!TextUtils.isEmpty(refreshToken)) {
            try {
                AuthTokenResponse authResponse = mAuthClient.blockingRefreshAccessToken(refreshToken);
                mAuthClient.updateLocalAccount(am, account, authResponse);
                final String token = authResponse.getAccessToken();
                if (!TextUtils.isEmpty(token)) {
                    final Bundle result = new Bundle();
                    result.putParcelable(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
                    result.putString(AccountManager.KEY_AUTHTOKEN, token);
                    result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, CMAccount.ACCOUNT_TYPE_CMAccount);
                    am.setAuthToken(account, CMAccount.AUTHTOKEN_TYPE_ACCESS, token);
                    return result;
                }
            } catch (VolleyError volleyError) {
                volleyError.printStackTrace();
                final int status = volleyError.networkResponse.statusCode;
                if (status == 400 || status == 401) {
                    mAccountManager.setUserData(account, CMAccount.AUTHTOKEN_TYPE_REFRESH, null);
                }
            }
        }
        return null;
    }

    private boolean hasRefreshToken(AccountManager am, Account account) {
        return am.getUserData(account, CMAccount.AUTHTOKEN_TYPE_REFRESH) != null;
    }
}
