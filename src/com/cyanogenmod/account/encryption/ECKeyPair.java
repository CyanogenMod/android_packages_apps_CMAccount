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
package com.cyanogenmod.account.encryption;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;

import java.util.UUID;

public class ECKeyPair {
    private ECPrivateKeyParameters privateKeyParameters;
    private ECPublicKeyParameters publicKeyParameters;
    private String keyId;

    public ECKeyPair(AsymmetricCipherKeyPair keyPair) {
        this.privateKeyParameters = (ECPrivateKeyParameters) keyPair.getPrivate();
        this.publicKeyParameters = (ECPublicKeyParameters) keyPair.getPublic();
        this.keyId = UUID.randomUUID().toString();
    }

    public ECKeyPair(ECPublicKeyParameters publicKey, String keyId) {
        this.publicKeyParameters = publicKey;
        this.keyId = keyId;
    }

    public ECPublicKeyParameters getPublicKey() {
        return publicKeyParameters;
    }

    public ECPrivateKeyParameters getPrivateKey() {
        return privateKeyParameters;
    }

    public String getKeyId() {
        return keyId;
    }
}