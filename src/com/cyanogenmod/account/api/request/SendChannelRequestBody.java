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

import android.location.Location;
import android.util.Log;
import com.cyanogenmod.account.auth.AuthClient;
import com.cyanogenmod.account.gcm.GCMUtil;
import com.cyanogenmod.account.gcm.model.*;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

public class SendChannelRequestBody {
    @Expose
    private String command;

    @Expose
    private String device_id;

    @Expose
    private String session_id;

    @Expose
    private Message message;

    public SendChannelRequestBody(String command, String device_id, String session_id, Message message) {
        this.command = command;
        this.device_id = device_id;
        this.session_id = session_id;
        this.message = message;
    }

    // LocationMessage constructor
    public SendChannelRequestBody(Location location, AuthClient authClient, String sessionId) {
        this.command = GCMUtil.COMMAND_SECURE_MESSAGE;
        this.device_id = authClient.getUniqueDeviceId();
        this.session_id = sessionId;

        // Try to load the symmetric key from the database.
        AuthClient.SymmetricKeySequencePair keyPair = authClient.getSymmetricKey(sessionId);
        if (keyPair == null) {
            return;
        }

        LocationMessage locationMessage = new LocationMessage(location, keyPair.getRemoteSequence());
        locationMessage.encrypt(keyPair.getSymmetricKey());
        this.message = locationMessage;
    }

    // WipeStartedMessage constructor
    public SendChannelRequestBody(WipeStartedMessage message, AuthClient authClient, String sessionId) {
        this.command = GCMUtil.COMMAND_SECURE_MESSAGE;
        this.device_id = authClient.getUniqueDeviceId();
        this.session_id = sessionId;

        // Try to load the symmetric key from the database.
        AuthClient.SymmetricKeySequencePair keyPair = authClient.getSymmetricKey(sessionId);
        if (keyPair == null) {
            return;
        }

        message.setSequence(keyPair.getRemoteSequence());
        message.encrypt(keyPair.getSymmetricKey());
        this.message = message;
    }

    public Message getMessage() {
        return message;
    }

    public String toJsonPlaintext() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public String getSessionId() {
        return session_id;
    }
}
