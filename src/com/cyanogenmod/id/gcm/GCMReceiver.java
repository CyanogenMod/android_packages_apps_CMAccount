package com.cyanogenmod.id.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.cyanogenmod.id.Constants;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;

public class GCMReceiver extends BroadcastReceiver {

    private static final String TAG = GCMReceiver.class.getSimpleName();

    private SharedPreferences mPreferences;
    private GoogleCloudMessaging mGoogleCloudMessaging;
    private boolean mDeviceFinderEnabled;

    public void onReceive(Context context, Intent intent) {
        mPreferences = context.getSharedPreferences(Constants.SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        mGoogleCloudMessaging = GoogleCloudMessaging.getInstance(context);
        mDeviceFinderEnabled = mPreferences.getBoolean(Constants.KEY_FIND_DEVICE_PREF, false);

        String messageType = mGoogleCloudMessaging.getMessageType(intent);

        if (Constants.DEBUG) Log.d(TAG, "messageType = " + messageType);
        String data = intent.getExtras().getString("data");
        if (Constants.DEBUG) Log.d(TAG, "message data = " + data);
        try {
            GCMessage message = new Gson().fromJson(data, GCMessage.class);
            if (Constants.DEBUG) Log.d(TAG, "GCMessage: \n " + message.dump());
            handleMessage(context, message);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error parsing GCM message", e);
        }
    }

    private void handleMessage(Context context, GCMessage message) {
        if (mDeviceFinderEnabled && GCMUtil.COMMAND_LOCATE.equals(message.getCommand())) {
            GCMUtil.reportLocation(context, message);
        }
    }
}
