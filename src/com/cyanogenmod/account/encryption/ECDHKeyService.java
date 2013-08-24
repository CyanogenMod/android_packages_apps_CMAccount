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
package com.cyanogenmod.account.encryption;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.IBinder;
import android.os.PowerManager;
import android.util.Log;
import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.auth.AuthClient;

public class ECDHKeyService extends IntentService {
    private static final String TAG = ECDHKeyService.class.getSimpleName();
    protected static final String EXTRA_RETRY = "retry";

    protected static final String ACTION_GENERATE = "com.cyanogenmod.account.encryption.GENERATE";
    protected static final String ACTION_SYNC = "com.cyanogenmod.account.encryption.SYNC";

    private static PowerManager.WakeLock sWakeLock;
    private static final int WAKE_LOCK_TIMEOUT = 1000 * 60 * 5;

    private Context mContext;
    private AuthClient mAuthClient;

    public ECDHKeyService() {
        super(TAG);
    }

    public static void startGenerate(Context context) {
        Intent intent = getIntent(context, ACTION_GENERATE);
        context.startService(intent);
    }

    protected static Intent getIntent(Context context, String action) {
        Intent intent = new Intent(context, ECDHKeyService.class);
        intent.setAction(action);
        return intent;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (CMAccount.DEBUG) Log.d(TAG, "Creating ECDHKeyService");
        mContext = getApplicationContext();
        mAuthClient = AuthClient.getInstance(mContext);
        acquireWakeLock();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        if (CMAccount.DEBUG) Log.d(TAG, "Starting ECDHKeyService");
        return START_REDELIVER_INTENT;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (ACTION_GENERATE.equals(intent.getAction())) {
            GeneratePublicKeysTask generatePublicKeysTask = new GeneratePublicKeysTask(mContext);
            generatePublicKeysTask.start(intent);
        } else if (ACTION_SYNC.equals(intent.getAction())) {
            SyncPublicKeysTask syncPublicKeysTask = new SyncPublicKeysTask(mContext);
            syncPublicKeysTask.start();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (CMAccount.DEBUG) Log.d(TAG, "onDestroy");
        releaseWakeLock();
    }

    private void acquireWakeLock() {
        if (sWakeLock == null) {
            PowerManager pm = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
            sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }

        if (!sWakeLock.isHeld()) {
            if (CMAccount.DEBUG) Log.v(TAG, "Acquiring " + WAKE_LOCK_TIMEOUT + " ms wakelock");
            sWakeLock.acquire(WAKE_LOCK_TIMEOUT);
        }
    }

    private void releaseWakeLock() {
        if (sWakeLock != null) {
            if (CMAccount.DEBUG) Log.v(TAG, "Releasing wakelock");
            sWakeLock.release();
        }
    }
}