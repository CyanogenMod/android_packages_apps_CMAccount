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

package com.cyanogenmod.account.gcm.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.math.ec.ECPoint;

public class GCMessage {
    public static final String COMMAND_SECURE_MESSAGE = "secure_message";

    private String command;
    private String account;
    private String payload;
    private String signature;
    private int sequence;

    public String getCommand() {
        return command;
    }

    public String getAccount() {
        return account;
    }

    public String getPayload() {
        return payload;
    }

    public String getSignature() {
        return signature;
    }

    public int getSequence() {
        return sequence;
    }

    public String toJson() {
        return new GsonBuilder().setPrettyPrinting().create().toJson(this);
    }
}
