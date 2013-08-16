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

package com.cyanogenmod.account.gcm;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.cyanogenmod.account.CMAccount;

public class GCMReceiver extends BroadcastReceiver {

    private static final String TAG = GCMReceiver.class.getSimpleName();

    public void onReceive(Context context, Intent intent) {
        // Change class and action so we can send it to GCMIntentService
        intent.setAction(GCMIntentService.ACTION_RECEIVE);
        intent.setClass(context, GCMIntentService.class);

        // Forward the intent to GCMIntentService
        if ( CMAccount.DEBUG) Log.d(TAG, "Forwarding request to GCMIntentService");
        context.startService(intent);
    }
}
