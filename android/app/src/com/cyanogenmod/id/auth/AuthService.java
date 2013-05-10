package com.cyanogenmod.id.auth;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;


public class AuthService extends Service {

    private Authenticator mAuthenticator;

    @Override
    public void onCreate() {
        super.onCreate();
        mAuthenticator = new Authenticator(this);
    }

    public IBinder onBind(Intent intent) {
        return mAuthenticator.getIBinder();
    }
}
