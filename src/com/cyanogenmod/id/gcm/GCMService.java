package com.cyanogenmod.id.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.cyanogenmod.id.api.PingService;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.Account;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.IOException;


public class GCMService extends IntentService {

    private static final String TAG = GCMService.class.getSimpleName();

    private static final String ACTION_REGISTER = "com.cyanogenmod.id.gcm.GCMService.REGISTER";
    private static final String ACTION_UNREGISTER = "com.cyanogenmod.id.gcm.GCMService.UNREGISTER";

    private static final String EXTRA_ACCOUNT = "account";

    public GCMService() {
       super(TAG);
    }

    public static void registerClient(Context context, Account account) {
        context.startService(getRegisterIntent(context, account));
    }

    public static void unregisterClient(Context context, Account account) {
        Intent intent = new Intent(ACTION_UNREGISTER, null, context, GCMService.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        final Context context = getApplicationContext();
        String action = intent.getAction();
        GoogleCloudMessaging gcm = GoogleCloudMessaging.getInstance(context);
        Account account = intent.getParcelableExtra(EXTRA_ACCOUNT);
        try {
            if (ACTION_UNREGISTER.equals(action)) {
                unregister(context, gcm, account);
            } else if (ACTION_REGISTER.equals(action)) {
                if (GCMUtil.isRegistrationExpired(context)) {
                    unregister(context, gcm, account);
                }
                register(context, gcm, GCMUtil.SENDER_ID, intent, account);
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString(), e);
            GCMUtil.clearRegistrationId(context);
            CMIDUtils.scheduleRetry(context, GCMUtil.getGCMPreferences(context), intent);
        }
    }

    private void register(Context context, GoogleCloudMessaging gcm, String sender_id, Intent intent, Account account) throws IOException {
        String regId = gcm.register(sender_id);
        if (regId != null) {
            GCMUtil.setRegistrationId(context, regId);
            CMIDUtils.resetBackoff(GCMUtil.getGCMPreferences(context));
            if (account != null) {
                PingService.pingServer(context, account);
            }
            GCMUtil.scheduleGCMReRegister(context, intent);
        } else {
            GCMUtil.clearRegistrationId(context);
            CMIDUtils.scheduleRetry(context, GCMUtil.getGCMPreferences(context), intent);
            GCMUtil.cancelGCMReRegister(context, intent);
        }
    }

    private void unregister(Context context, GoogleCloudMessaging gcm, Account account) throws IOException {
        gcm.unregister();
        GCMUtil.clearRegistrationId(context);
        CMIDUtils.resetBackoff(GCMUtil.getGCMPreferences(context));
        if (account != null) {
            PingService.pingServer(context, account);
        }
        GCMUtil.cancelGCMReRegister(context, getRegisterIntent(context, account));
    }


    private static Intent getRegisterIntent(Context context, Account account) {
        Intent intent = new Intent(ACTION_REGISTER, null, context, GCMService.class);
        intent.putExtra(EXTRA_ACCOUNT, account);
        return intent;
    }
}
