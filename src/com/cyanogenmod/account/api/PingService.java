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

package com.cyanogenmod.account.api;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.auth.AuthClient;
import com.cyanogenmod.account.util.CMAccountUtils;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class PingService extends Service implements Response.ErrorListener, Response.Listener<PingResponse> {

    private static final String TAG = PingService.class.getSimpleName();
    private static PowerManager.WakeLock sWakeLock;

    private static final String EXTRA_RETRY= "retry";

    private AuthClient mAuthClient;

    public static void pingServer(Context context) {
        Intent intent = getPingIntent(context);
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
            if (CMAccount.DEBUG) Log.v(TAG, "Acquiring wakelock");
            sWakeLock.acquire();
        }
        if (CMAccountUtils.getCMAccountAccount(context) == null) {
            CMAccountUtils.cancelCMAccountPing(context, intent);
            stopSelf();
        }
        mAuthClient = AuthClient.getInstance(context);
        boolean retry = intent.getBooleanExtra(EXTRA_RETRY, false);
        if (!retry) {
            CMAccountUtils.resetBackoff(AuthClient.getInstance(context).getAuthPreferences());
        }
        mAuthClient.pingService(PingService.this, PingService.this);
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sWakeLock != null) {
            if (CMAccount.DEBUG) Log.v(TAG, "Releasing wakelock");
            sWakeLock.release();
        }
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        if (CMAccount.DEBUG) volleyError.printStackTrace();
        handleError();
    }

    @Override
    public void onResponse(PingResponse pingResponse) {
        if (pingResponse.getStatusCode() == 200) {
            CMAccountUtils.resetBackoff(mAuthClient.getAuthPreferences());
            final Context context = getApplicationContext();
            CMAccountUtils.scheduleCMAccountPing(context, getPingIntent(context));
            stopSelf();
        } else {
            handleError();
        }
    }

    private void handleError() {
        Intent intent = new Intent(getApplicationContext(), PingService.class);
        intent.putExtra(EXTRA_RETRY, true);
        CMAccountUtils.scheduleRetry(getApplicationContext(), mAuthClient.getAuthPreferences(), intent);
        stopSelf();
    }

    public static Intent getPingIntent(Context context) {
        return new Intent(context, PingService.class);
    }
}
