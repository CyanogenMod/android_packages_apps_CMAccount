package com.cyanogenmod.id.auth;

import com.cyanogenmod.id.Constants;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;


public class AuthService extends Service {
    private static final String TAG = "AuthService";
    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        if (Constants.DEBUG) Log.d(TAG, "CMID Auth Service started.");
        mAuthenticator = new Authenticator(this);
    }

    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
