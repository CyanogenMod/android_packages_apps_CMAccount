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
import com.cyanogenmod.account.encryption.ECKeyPair;
import com.cyanogenmod.account.util.CMAccountUtils;
import com.cyanogenmod.account.util.EncryptionUtils;
import com.google.gson.Gson;

import org.spongycastle.crypto.params.ECPublicKeyParameters;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

public class AddPublicKeysRequestBody {
    private LinkedList<PublicKey> public_keys;
    private String public_keys_hash;
    private String device_id;

    public AddPublicKeysRequestBody(Context context, List<ECKeyPair> keyPairs) {
        device_id = CMAccountUtils.getUniqueDeviceId(context);
        public_keys = new LinkedList<PublicKey>();
        byte[] hmacSecret = CMAccountUtils.getHmacSecret(context);

        for (ECKeyPair keyPair : keyPairs) {
            String key_id = keyPair.getKeyId();
            ECPublicKeyParameters publicKey = keyPair.getPublicKey();
            String signature = calculateSignature(publicKey, hmacSecret);

            public_keys.add(new PublicKey(publicKey, key_id, signature));
        }

        public_keys_hash = generatePublicKeysHash();
    }

    private String generatePublicKeysHash() {
        Collections.sort(public_keys);
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");

            for (PublicKey publicKey : public_keys) {
                md.update(publicKey.getKeyId().getBytes());
                md.update(publicKey.getSignature().getBytes());
                md.update(publicKey.getPublicKey().getBytes());
            }

            return CMAccountUtils.encodeHex(md.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new AssertionError(e);
        }
    }

    private String calculateSignature(ECPublicKeyParameters publicKey, byte[] hmacSecret) {
        return EncryptionUtils.HMAC.getSignature(hmacSecret,
                CMAccountUtils.encodeHex(publicKey.getQ().getEncoded()));
    }

    public String toJson(Gson gson) {
        return gson.toJson(this);
    }

    private static class PublicKey implements Comparable<PublicKey> {
        private String public_key;
        private String key_id;
        private String signature;

        public PublicKey(ECPublicKeyParameters publicKey, String key_id, String signature) {
            this.public_key = CMAccountUtils.encodeHex(publicKey.getQ().getEncoded());
            this.key_id = key_id;
            this.signature = signature;
        }

        public String getPublicKey() {
            return public_key;
        }

        public String getKeyId() {
            return key_id;
        }

        public String getSignature() {
            return signature;
        }

        @Override
        public int compareTo(PublicKey another) {
            return key_id.compareTo(another.getKeyId());
        }
    }
}
