/*
 * Copyright (C) 2014 The CyanogenMod Project
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
package com.cyanogenmod.account.util;

import android.content.Context;
import android.content.Intent;
import android.telephony.TelephonyManager;
import android.util.Log;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber;

/**
 * Utilities for interacting with WhisperPush
 *
 * @author Chris Soyars
 */
public class WhisperPushUtils {

    private static final String TAG = WhisperPushUtils.class.getSimpleName();
    private static final String ACTION_REGISTER_NUMBER = "org.thoughtcrime.securesms.RegistrationService.REGISTER_NUMBER";

    private static boolean isEmpty(String value) {
        return value == null || value.trim().length() == 0;
    }

    private static String getPhoneNumber(Context context) {
        String localNumber = ((TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE))
                .getLine1Number();

        if (!isEmpty(localNumber) && !localNumber.startsWith("+")) {
            if (localNumber.length() == 10) {
                localNumber = "+1" + localNumber;
            } else {
                localNumber = "+" + localNumber;
            }
        }

        try {
            if (!isEmpty(localNumber)) {
                PhoneNumberUtil numberUtil = PhoneNumberUtil.getInstance();
                Phonenumber.PhoneNumber localNumberObject = numberUtil.parse(localNumber, null);
                return numberUtil.format(localNumberObject, PhoneNumberUtil.PhoneNumberFormat.E164);
            }
        } catch (NumberParseException npe) {
            Log.w(TAG, npe);
        }

        return null;
    }

    public static void startRegistration(Context context) {
        String phoneNumber = getPhoneNumber(context);
        Log.d(TAG, "Starting WhisperPush registration with number: " + phoneNumber);
        if (phoneNumber != null) {
            Intent intent = new Intent();
            intent.setAction(ACTION_REGISTER_NUMBER);
            intent.setClassName("org.whispersystems.whisperpush", "org.whispersystems.whisperpush.service.RegistrationService");
            intent.putExtra("e164number", phoneNumber);
            context.startService(intent);
        }
    }

}