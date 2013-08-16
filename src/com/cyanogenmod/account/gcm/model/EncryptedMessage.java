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

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import static com.cyanogenmod.account.util.EncryptionUtils.AES;

public class EncryptedMessage extends Message {
    @Expose
    private String ciphertext;

    @Expose
    private String initializationVector;

    public EncryptedMessage() {
    }

    public EncryptedMessage(String ciphertext, String initializationVector) {
        this.ciphertext = ciphertext;
        this.initializationVector = initializationVector;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public String getIV() {
        return initializationVector;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public void encrypt(String symmetricKey) {
        String json = toJson();

        AES.CipherResult result = AES.encrypt(json, symmetricKey);
        ciphertext = result.getCiphertext();
        initializationVector = result.getInitializationVector();
    }

}
