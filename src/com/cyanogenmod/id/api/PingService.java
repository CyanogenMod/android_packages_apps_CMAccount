package com.cyanogenmod.id.api;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.Account;
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
        Intent intent = new Intent(context, PingService.class);
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
        mAuthClient = AuthClient.getInstance(context);
        boolean retry = intent.getBooleanExtra(EXTRA_RETRY, false);
        if (!retry) {
            CMIDUtils.resetBackoff(AuthClient.getInstance(context).getAuthPreferences());
        }
        mAuthClient.pingService(PingService.this, PingService.this);
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
        if (CMID.DEBUG) volleyError.printStackTrace();
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
        intent.putExtra(EXTRA_RETRY, true);
        CMIDUtils.scheduleRetry(getApplicationContext(), mAuthClient.getAuthPreferences(), intent);
        stopSelf();
    }
}
