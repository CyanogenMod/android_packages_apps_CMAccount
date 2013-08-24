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

import com.cyanogenmod.account.util.EncryptionUtils;

import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.math.ec.ECPoint;

import java.math.BigInteger;

public class PublicKeyMessage {
    private String public_key;
    private String signature;

    public ECPublicKeyParameters getPublicKey() {
        return EncryptionUtils.ECDH.getPublicKey(public_key);
    }

    public String getSignature() {
        return signature;
    }
}
