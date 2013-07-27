package com.cyanogenmod.id.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.cyanogenmod.id.api.PingService;
import com.cyanogenmod.id.util.CMIDUtils;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;


public class GCMService extends IntentService {

    private static final String TAG = GCMService.class.getSimpleName();

    private static final String ACTION_REGISTER = "com.cyanogenmod.id.gcm.GCMService.REGISTER";
    private static final String ACTION_UNREGISTER = "com.cyanogenmod.id.gcm.GCMService.UNREGISTER";

    public GCMService() {
       super(TAG);
    }

    public static void registerClient(Context context) {
        context.startService(getRegisterIntent(context));
    }

    public static void unregisterClient(Context context) {
        Intent intent = new Intent(ACTION_UNREGISTER, null, context, GCMService.class);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Context context = getApplicationContext();
        String action = intent.getAction();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        try {
            if (ACTION_UNREGISTER.equals(action)) {
                unregister(context, gcm);
            } else if (ACTION_REGISTER.equals(action)) {
                if (GCMUtil.isRegistrationExpired(context)) {
                    unregister(context, gcm);
                }
                register(context, gcm, GCMUtil.SENDER_ID, intent);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString(), e);
            GCMUtil.clearRegistrationId(context);
            CMIDUtils.scheduleRetry(context, GCMUtil.getGCMPreferences(context), intent);
        }
    }

    private void register(Context context, GoogleCloudMessaging gcm, String sender_id, Intent intent) throws IOException {
        String regId = gcm.register(sender_id);
        if (regId != null) {
            GCMUtil.setRegistrationId(context, regId);
            CMIDUtils.resetBackoff(GCMUtil.getGCMPreferences(context));
            PingService.pingServer(context);
            GCMUtil.scheduleGCMReRegister(context, intent);
        } else {
            GCMUtil.clearRegistrationId(context);
            CMIDUtils.scheduleRetry(context, GCMUtil.getGCMPreferences(context), intent);
            GCMUtil.cancelGCMReRegister(context, intent);
        }
    }

    private void unregister(Context context, GoogleCloudMessaging gcm) throws IOException {
        gcm.unregister();
        GCMUtil.clearRegistrationId(context);
        CMIDUtils.resetBackoff(GCMUtil.getGCMPreferences(context));
        PingService.pingServer(context);
        GCMUtil.cancelGCMReRegister(context, getRegisterIntent(context));
    }

    private static Intent getRegisterIntent(Context context) {
        return new Intent(ACTION_REGISTER, null, context, GCMService.class);
    }
}
