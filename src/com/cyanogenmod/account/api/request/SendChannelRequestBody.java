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

package com.cyanogenmod.account.api.request;

import android.content.Context;
import android.location.Location;
import android.util.Log;
import com.cyanogenmod.account.auth.AuthClient;
import com.cyanogenmod.account.gcm.GCMUtil;
import com.cyanogenmod.account.gcm.model.*;
import com.cyanogenmod.account.util.CMAccountUtils;
import com.cyanogenmod.account.util.EncryptionUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class SendChannelRequestBody {
    private transient AuthClient mAuthClient;
    private transient byte[] mHmacSecret;
    private transient String mKeyId;
    private transient AuthClient.SymmetricKeySequencePair mKeyPair;

    private String command;
    private String device_id;
    private String payload;
    private String signature;
    private int sequence;

    // PlaintextMessage constructor
    public SendChannelRequestBody(String command, String device_id, PlaintextMessage payload) {
        this.command = command;
        this.device_id = device_id;
        this.payload = payload.toJson();
    }

    // LocationMessage constructor
    public SendChannelRequestBody(Context context, String keyId, Location location) {
        setup(context, keyId);
        this.command = GCMUtil.COMMAND_SECURE_MESSAGE;
        this.sequence = mKeyPair.getRemoteSequence();

        LocationMessage locationMessage = new LocationMessage(location, keyId);
        locationMessage.encrypt(mKeyPair.getSymmetricKey());
        this.payload = locationMessage.toExcludingJson();
        signPayload();
    }

    // WipeStartedMessage constructor
    public SendChannelRequestBody(Context context, String keyId, WipeStartedMessage payload) {
        setup(context, keyId);
        this.command = GCMUtil.COMMAND_SECURE_MESSAGE;
        this.sequence = mKeyPair.getRemoteSequence();
        payload.setKeyId(keyId);
        payload.encrypt(mKeyPair.getSymmetricKey());
        this.payload = payload.toExcludingJson();
        signPayload();
    }

    private void setup(Context context, String keyId) {
        mAuthClient = AuthClient.getInstance(context);
        mHmacSecret = CMAccountUtils.getHmacSecret(context);
        mKeyPair = mAuthClient.getSymmetricKey(keyId);
        mKeyId = keyId;
        device_id = mAuthClient.getUniqueDeviceId();
    }

    private void signPayload() {
        this.signature = EncryptionUtils.HMAC.getSignature(mHmacSecret, payload);
    }

    public String getKeyId() {
        return mKeyId;
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public String toJsonPretty() {
        return new GsonBuilder()
                .setPrettyPrinting()
                .create()
                .toJson(this);
    }
}
