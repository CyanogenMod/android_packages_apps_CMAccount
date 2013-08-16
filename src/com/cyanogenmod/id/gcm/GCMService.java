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

package com.cyanogenmod.id.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;

import com.cyanogenmod.id.CMID;
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
                register(context, gcm, GCMUtil.SENDER_ID, intent);
            }
        } catch (IOException e) {
            if (CMID.DEBUG) Log.e(TAG, "Unable to register for GCM: " + e.toString());
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
