package com.cyanogenmod.id.auth;

import android.accounts.*;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;

import com.android.volley.VolleyError;
import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.ui.CMIDActivity;


public class Authenticator extends AbstractAccountAuthenticator {
    private static final String TAG = "Authenticator";
    private final Context mContext;
    private AuthClient mAuthClient;

    public Authenticator(Context context) {
        super(context);
        mContext = context;
        mAuthClient = AuthClient.getInstance(context);
    }



    @Override
    public Bundle addAccount(AccountAuthenticatorResponse response, String accountType,
                             String authTokenType, String[] requiredFeatures, Bundle options) throws NetworkErrorException {
        if (Constants.DEBUG) Log.d(TAG, "addAccount()");
        final Intent intent = new Intent(mContext, CMIDActivity.class);
        intent.putExtra(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
        final Bundle bundle = new Bundle();
        bundle.putParcelable(AccountManager.KEY_INTENT, intent);
        return bundle;
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
        if (Constants.DEBUG) Log.d(TAG, "getAuthToken() account="+account.name+ " type="+account.type);
        if (!authTokenType.equals(Constants.AUTHTOKEN_TYPE_ACCESS)) {
            final Bundle result = new Bundle();
            result.putString(AccountManager.KEY_ERROR_MESSAGE, "invalid authTokenType");
            return result;
        }
        final AccountManager am = AccountManager.get(mContext);
        if (!hasAuthenticated(am, account)) {
            if (Constants.DEBUG) Log.d(TAG, "not authenticated account="+account.name+ " type="+account.type);
            final Bundle bundle = new Bundle();
            final Intent intent = new Intent(mContext, AuthActivity.class);
            intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
            bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            return bundle;
        }
        if (isTokenExpired(am, account)) {
            Bundle bundle = refreshToken(am, account, response);
            if (bundle == null) {
                final Intent intent = new Intent(mContext, AuthActivity.class);
                intent.putExtra(AccountManager.KEY_ACCOUNT_AUTHENTICATOR_RESPONSE, response);
                bundle = new Bundle();
                bundle.putParcelable(AccountManager.KEY_INTENT, intent);
            }
            return bundle;
        }

        String token =  am.getUserData(account, Constants.AUTHTOKEN_TYPE_ACCESS);
        am.setAuthToken(account, Constants.AUTHTOKEN_TYPE_ACCESS, token);

        final Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, account.name);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        result.putString(AccountManager.KEY_AUTHTOKEN, token);
        return result;
    }

    @Override
    public String getAuthTokenLabel(String authTokenType) {
        return null;
    }

    @Override
    public Bundle updateCredentials(AccountAuthenticatorResponse response, Account account, String authTokenType, Bundle loginOptions) throws NetworkErrorException {
        return null;
    }

    @Override
    public Bundle hasFeatures(AccountAuthenticatorResponse response, Account account, String[] features) throws NetworkErrorException {
        final Bundle result = new Bundle();
        result.putBoolean(AccountManager.KEY_BOOLEAN_RESULT, false);
        return result;
    }

    private Bundle refreshToken(AccountManager am, Account account, AccountAuthenticatorResponse response) {
        final String refreshToken = am.getUserData(account, Constants.AUTHTOKEN_TYPE_REFRESH);
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
                    result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
                    am.setAuthToken(account, Constants.AUTHTOKEN_TYPE_ACCESS, token);
                    return result;
                }
            } catch (VolleyError volleyError) {
                volleyError.printStackTrace();
            }
        }
        return null;
    }

    private boolean isTokenExpired(AccountManager am, Account account) {
        final String expires_in = am.getUserData(account, Constants.AUTHTOKEN_EXPIRES_IN);
        final long expiresTime = expires_in == null ? 0 : Long.valueOf(expires_in);
        final boolean expired = System.currentTimeMillis() > expiresTime;
        if (expired) {
            final String token = am.getUserData(account, Constants.AUTHTOKEN_TYPE_ACCESS);
            if (!TextUtils.isEmpty(token)) {
                am.invalidateAuthToken(Constants.ACCOUNT_TYPE, token);
            }
        }
        return expired;
    }

    private boolean hasAuthenticated(AccountManager am, Account account) {
        return am.getUserData(account, Constants.AUTHTOKEN_TYPE_REFRESH) != null;
    }
}
