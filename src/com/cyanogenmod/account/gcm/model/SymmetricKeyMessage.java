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

import com.google.gson.annotations.Expose;

public class SymmetricKeyMessage extends Message {

    @Expose
    private String symmetric_key;

    @Expose
    private String symmetric_key_verification;

    public SymmetricKeyMessage(String symmetric_key, String symmetric_key_verification) {
        this.symmetric_key = symmetric_key;
        this.symmetric_key_verification = symmetric_key_verification;
    }
}
