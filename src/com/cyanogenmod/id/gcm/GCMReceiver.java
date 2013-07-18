package com.cyanogenmod.id.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.cyanogenmod.id.CMID;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

public class GCMReceiver extends BroadcastReceiver {

    private static final String TAG = GCMReceiver.class.getSimpleName();

    private PowerManager.WakeLock mWakeLock;
    private GoogleCloudMessaging mGoogleCloudMessaging;

    public void onReceive(Context context, Intent intent) {
        mGoogleCloudMessaging = GoogleCloudMessaging.getInstance(context);

        String messageType = mGoogleCloudMessaging.getMessageType(intent);

        if (CMID.DEBUG) Log.d(TAG, "messageType = " + messageType);
        String data = intent.getExtras().getString("data");
        if (CMID.DEBUG) Log.d(TAG, "message data = " + data);
        try {
            GCMessage message = new Gson().fromJson(data, GCMessage.class);
            if (CMID.DEBUG) Log.d(TAG, "GCMessage: \n " + message.dump());
            handleMessage(context, message);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error parsing GCM message", e);
        }
    }

    private void handleMessage(Context context, GCMessage message) {
        if (GCMUtil.COMMAND_LOCATE.equals(message.getCommand())) {
            GCMUtil.reportLocation(context, message);
        } else  if (GCMUtil.COMMAND_WIPE.equals(message.getCommand())) {
            final PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
            final PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
            wakeLock.acquire(1000 * 60);
            final DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            Thread t = new Thread(new Runnable() {
                @Override
                public void run() {
                    dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
                }
            });
            t.start();
        }
    }
}
