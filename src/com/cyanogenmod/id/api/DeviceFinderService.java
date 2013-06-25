package com.cyanogenmod.id.api;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesClient;
import com.google.android.gms.location.LocationClient;
import com.google.android.gms.location.LocationListener;
import com.google.android.gms.location.LocationRequest;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.auth.AuthClient;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;

import java.io.IOException;

public class DeviceFinderService extends Service implements LocationListener,
        GooglePlayServicesClient.ConnectionCallbacks,  GooglePlayServicesClient.OnConnectionFailedListener,
        Response.Listener<Integer>, Response.ErrorListener {

    private static final String TAG = DeviceFinderService.class.getSimpleName();
    private static PowerManager.WakeLock sWakeLock;

    private static final String EXTRA_ACCOUNT = "account";

    private static final int LOCATION_UPDATE_INTERVAL = 5000;
    private static final int MAX_LOCATION_UPDATES = 10;
    private static final int LOCATION_ACCURACY_THRESHOLD = 5; //meters

    private LocationClient mLocationClient;
    private LocationRequest mLocationRequest;
    private Location mLastLocationUpdate;
    private Account mAccount;
    private String mAuthToken;
    private AuthClient mAuthClient;
    private Request<?> mInFlightRequest;

    private int mUpdateCount = 0;

    private boolean mIsRunning = false;

    public static void reportLocation(Context context, Account account) {
        if (sWakeLock == null) {
            PowerManager pm = (PowerManager)
                    context.getSystemService(Context.POWER_SERVICE);
            sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!sWakeLock.isHeld()) {
            if (Constants.DEBUG) Log.v(TAG, "Acquiring wakelock");
            sWakeLock.acquire();
        }
        Intent intent = new Intent(context, DeviceFinderService.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
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
            mAccount = intent.getParcelableExtra(EXTRA_ACCOUNT);
            mAuthClient = AuthClient.getInstance(context);
            mLocationClient = new LocationClient(context, this, this);
            mLocationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(LOCATION_UPDATE_INTERVAL)
                    .setNumUpdates(MAX_LOCATION_UPDATES);
            final AccountManager am = AccountManager.get(context);
            am.getAuthToken(mAccount, Constants.AUTHTOKEN_TYPE_ACCESS, true, new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture) {
                    try {
                        Bundle bundle =  bundleAccountManagerFuture.getResult();
                        mAuthToken = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                        mLocationClient.connect();
                    } catch (OperationCanceledException e) {
                        Log.e(TAG, "Unable to get AuthToken", e);
                        stopSelf();
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to get AuthToken", e);
                        stopSelf();
                    } catch (AuthenticatorException e) {
                        Log.e(TAG, "Unable to get AuthToken", e);
                        stopSelf();
                    }
                }
            }, new Handler());
        }
        return START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (sWakeLock != null) {
            if (Constants.DEBUG) Log.v(TAG, "Releasing wakelock");
            sWakeLock.release();
        }
        mIsRunning = false;
    }

    @Override
    public void onLocationChanged(final Location location) {
        onLocationChanged(location, false);
    }

    private void onLocationChanged(final Location location, boolean fromLastLocation) {
        if (Constants.DEBUG) Log.v(TAG, "onLocationChanged() " + location.toString());
        mLastLocationUpdate = location;
        if (mInFlightRequest != null && !fromLastLocation) {
            mInFlightRequest.cancel();
            mInFlightRequest = null;
        }
        if (mAuthToken != null) {
            mInFlightRequest = mAuthClient.reportLocation(mAuthToken, location.getLatitude(), location.getLongitude(), this, this);
            if (!fromLastLocation) mUpdateCount++;
        }
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
        if (Constants.DEBUG) Log.v(TAG, "Successfully posted location");
        mInFlightRequest = null;
        if (mLastLocationUpdate != null) {
            maybeStopLocationUpdates(mLastLocationUpdate.getAccuracy());
        }
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        int statusCode = volleyError.networkResponse.statusCode;
        if (Constants.DEBUG) Log.v(TAG, "Location post error status = "+ statusCode);
        volleyError.printStackTrace();
        mInFlightRequest = null;
        mLocationClient.disconnect();
        stopSelf();
        if (statusCode == 401) {
            final AccountManager am = AccountManager.get(getApplicationContext());
            am.invalidateAuthToken(Constants.AUTHTOKEN_TYPE_ACCESS, mAuthToken);
            reportLocation(getApplicationContext(), mAccount);
        }
    }

    private void maybeStopLocationUpdates(float accuracy) {
        if (Constants.DEBUG) Log.v(TAG, "Update count = "+ mUpdateCount);
        // if mUpdateCount, then this is a case we have the last known location. Don't stop in that case.
        if ((mUpdateCount != 0) && (accuracy <= LOCATION_ACCURACY_THRESHOLD || mUpdateCount == MAX_LOCATION_UPDATES)) {
            stopUpdates();
        }
    }

    private void stopUpdates() {
        if (Constants.DEBUG) Log.v(TAG, "Stopping location updates");
        mLocationClient.removeLocationUpdates(this);
        mLocationClient.disconnect();
        stopSelf();
    }
}
