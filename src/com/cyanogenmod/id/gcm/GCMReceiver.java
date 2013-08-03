package com.cyanogenmod.id.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.cyanogenmod.id.CMID;

public class GCMReceiver extends BroadcastReceiver {

    private static final String TAG = GCMReceiver.class.getSimpleName();

    public void onReceive(Context context, Intent intent) {
        // Copy the extras over to a GCMIntentService Intent
        Intent serviceIntent = GCMIntentService.getIntent(context);
        serviceIntent.putExtras(intent.getExtras());

        // Forward the intent to GCMIntentService
        if ( CMID.DEBUG) Log.d(TAG, "Forwarding request to GCMIntentService");
        context.startService(serviceIntent);
    }
}
