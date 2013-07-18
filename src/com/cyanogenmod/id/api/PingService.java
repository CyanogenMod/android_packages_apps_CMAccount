package com.cyanogenmod.id.api;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

public class PingService extends Service implements Response.ErrorListener, Response.Listener<PingResponse> {

    private static final String TAG = PingService.class.getSimpleName();
    private static PowerManager.WakeLock sWakeLock;

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_RETRY= "retry";

    private AuthClient mAuthClient;
    private Account mAccount;
    private AccountManager mAccountManager;
    private String mAuthToken;

    public static void pingServer(Context context, Account account) {
        Intent intent = new Intent(context, PingService.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        final Context context = getApplicationContext();
        if (sWakeLock == null) {
            PowerManager pm = (PowerManager)
                    context.getSystemService(Context.POWER_SERVICE);
            sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!sWakeLock.isHeld()) {
            if (CMID.DEBUG) Log.v(TAG, "Acquiring wakelock");
            sWakeLock.acquire();
        }
        mAccount = intent.getParcelableExtra(EXTRA_ACCOUNT);
        mAccountManager = AccountManager.get(context);
        mAuthClient = AuthClient.getInstance(context);
        boolean retry = intent.getBooleanExtra(EXTRA_RETRY, false);
        if (!retry) {
            CMIDUtils.resetBackoff(AuthClient.getInstance(context).getAuthPreferences());
        }
        mAccountManager.getAuthToken(mAccount, CMID.AUTHTOKEN_TYPE_ACCESS, true, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture) {
                try {
                    Bundle bundle =  bundleAccountManagerFuture.getResult();
                    mAuthToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    mAuthClient.pingService(mAuthToken, PingService.this, PingService.this);
                } catch (OperationCanceledException e) {
                    Log.e(TAG, "Unable to get AuthToken", e);
                    handleError();
                } catch (IOException e) {
                    Log.e(TAG, "Unable to get AuthToken", e);
                    handleError();
                } catch (AuthenticatorException e) {
                    Log.e(TAG, "Unable to get AuthToken", e);
                    handleError();
                }
            }
        }, new Handler());
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sWakeLock != null) {
            if (CMID.DEBUG) Log.v(TAG, "Releasing wakelock");
            sWakeLock.release();
        }
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        volleyError.printStackTrace();
        if (volleyError.networkResponse.statusCode == 401) {
            mAccountManager.invalidateAuthToken(CMID.AUTHTOKEN_TYPE_ACCESS, mAuthToken);
        }
        handleError();
    }

    @Override
    public void onResponse(PingResponse pingResponse) {
        if (pingResponse.getStatusCode() == 200) {
            CMIDUtils.resetBackoff(mAuthClient.getAuthPreferences());
            stopSelf();
        } else {
            handleError();
        }
    }

    private void handleError() {
        Intent intent = new Intent(getApplicationContext(), PingService.class);
        intent.putExtra(EXTRA_ACCOUNT, mAccount);
        intent.putExtra(EXTRA_RETRY, true);
        CMIDUtils.scheduleRetry(getApplicationContext(), mAuthClient.getAuthPreferences(), intent);
        stopSelf();
    }
}
