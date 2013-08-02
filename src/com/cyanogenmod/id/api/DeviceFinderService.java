package com.cyanogenmod.id.api;

import com.cyanogenmod.id.gcm.GCMUtil;
import com.cyanogenmod.id.gcm.model.ChannelMessage;
import com.cyanogenmod.id.gcm.model.LocationMessage;
import com.cyanogenmod.id.gcm.model.SecureMessage;
import com.cyanogenmod.id.util.CMIDUtils;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

public class DeviceFinderService extends Service implements LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,  GooglePlayServicesClient.OnConnectionFailedListener,
        Response.Listener<Integer>, Response.ErrorListener {

    private static final String TAG = DeviceFinderService.class.getSimpleName();
    private static PowerManager.WakeLock sWakeLock;

    private static final String EXTRA_ACCOUNT = "account";
    private static final String EXTRA_SESSION_ID = "session_id";

    private static final int LOCATION_UPDATE_INTERVAL = 5000;
    private static final int MAX_LOCATION_UPDATES = 10;
    private static final int LOCATION_ACCURACY_THRESHOLD = 5; //meters

    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocationUpdate;
    private AuthClient mAuthClient;
    private String mSessionId;
    private static String sDeviceId;

    private int mUpdateCount = 0;

    private boolean mIsRunning = false;

    public static void reportLocation(Context context, Account account, final String sessionId) {
        sDeviceId = CMIDUtils.getUniqueDeviceId(context);
        if (sWakeLock == null) {
            PowerManager pm = (PowerManager)
                    context.getSystemService(Context.POWER_SERVICE);
            sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!sWakeLock.isHeld()) {
            if (CMID.DEBUG) Log.v(TAG, "Acquiring wakelock");
            sWakeLock.acquire();
        }
        Intent intent = new Intent(context, DeviceFinderService.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
        intent.putExtra(EXTRA_SESSION_ID, sessionId);
        context.startService(intent);
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!mIsRunning) {
            final Context context = getApplicationContext();
            mIsRunning = true;
            mAuthClient = AuthClient.getInstance(context);
            mLocationClient = new LocationClient(context, this, this);
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(LOCATION_UPDATE_INTERVAL)
                    .setNumUpdates(MAX_LOCATION_UPDATES);
            mLocationClient.connect();
        }

        // Reset the session
        Bundle extras = intent.getExtras();
        if (extras != null) mSessionId = extras.getString(EXTRA_SESSION_ID);
        mUpdateCount = 0;

        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sWakeLock != null) {
            if (CMID.DEBUG) Log.v(TAG, "Releasing wakelock");
            sWakeLock.release();
        }
        mIsRunning = false;
    }

    @Override
    public void onLocationChanged(final Location location) {
        onLocationChanged(location, false);
    }

    private void onLocationChanged(final Location location, boolean fromLastLocation) {
        if (CMID.DEBUG) Log.v(TAG, "onLocationChanged() " + location.toString());
        mLastLocationUpdate = location;

        // Create an encrypted LocationMessage
        SecureMessage locationMessage = LocationMessage.getEncrypted(location, mAuthClient, mSessionId);

        // Create the ChannelMessage
        ChannelMessage channelMessage = new ChannelMessage(GCMUtil.COMMAND_SECURE_MESSAGE, sDeviceId, mSessionId, locationMessage);

        // Send it
        if (CMID.DEBUG) Log.d(TAG, "Sending secure location message = " + channelMessage.toJson());
        mAuthClient.sendChannel(channelMessage, this, this);
        if (!fromLastLocation) mUpdateCount++;

        mLastLocationUpdate = location;
    }

    @Override
    public void onConnected(Bundle bundle) {
        mUpdateCount = 0;
        Location lastLocation = mLocationClient.getLastLocation();
        if (lastLocation != null) {
            onLocationChanged(lastLocation, true);
        }
        mLocationClient.requestLocationUpdates(mLocationRequest, this);
    }

    @Override
    public void onDisconnected() {}

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        stopSelf();
    }

    @Override
    public void onResponse(Integer status) {
        if (CMID.DEBUG) Log.v(TAG, "Successfully posted location");
        if (mLastLocationUpdate != null) {
            maybeStopLocationUpdates(mLastLocationUpdate.getAccuracy());
        }
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        int statusCode = volleyError.networkResponse.statusCode;
        if (CMID.DEBUG) Log.v(TAG, "Location post error status = "+ statusCode);
        volleyError.printStackTrace();
        mLocationClient.disconnect();
        stopSelf();
    }

    private void maybeStopLocationUpdates(float accuracy) {
        if (CMID.DEBUG) Log.v(TAG, "Update count = "+ mUpdateCount);
        // if mUpdateCount, then this is a case we have the last known location. Don't stop in that case.
        if ((mUpdateCount != 0) && (accuracy <= LOCATION_ACCURACY_THRESHOLD || mUpdateCount == MAX_LOCATION_UPDATES)) {
            stopUpdates();
        }
    }

    private void stopUpdates() {
        if (CMID.DEBUG) Log.v(TAG, "Stopping location updates");
        mLocationClient.removeLocationUpdates(this);
        mLocationClient.disconnect();
        stopSelf();
    }
}
