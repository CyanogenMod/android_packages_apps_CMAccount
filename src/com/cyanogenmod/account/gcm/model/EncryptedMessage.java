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

import com.cyanogenmod.account.util.CMAccountUtils;
import com.cyanogenmod.account.util.EncryptionUtils;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.annotations.Expose;

import org.spongycastle.crypto.params.ECPublicKeyParameters;

public class EncryptedMessage implements Message {
    @Expose
    private String ciphertext;

    @Expose
    protected String key_id;

    private String public_key;

    public byte[] getCiphertext() {
        return CMAccountUtils.decodeHex(ciphertext);
    }

    public String getKeyId() {
        return key_id;
    }

    public ECPublicKeyParameters getPublicKey() {
        return EncryptionUtils.ECDH.getPublicKey(public_key);
    }

    public String toJson() {
        return new Gson().toJson(this);
    }

    public String toExcludingJson() {
        return new GsonBuilder()
                .excludeFieldsWithoutExposeAnnotation()
                .create()
                .toJson(this);
    }

    public void encrypt(String symmetricKey) {
        byte[] symmetricKeyBytes = CMAccountUtils.decodeHex(symmetricKey);
        String json = toJson();

        byte[] result = EncryptionUtils.AES.encrypt(json, symmetricKeyBytes);
        ciphertext = CMAccountUtils.encodeHex(result);
    }

    public static EncryptedMessage fromJson(String json) {
        return new Gson().fromJson(json, EncryptedMessage.class);
    }
}
