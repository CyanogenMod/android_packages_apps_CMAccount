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

package com.cyanogenmod.account.util;

import android.util.Base64;
import android.util.Log;
import com.cyanogenmod.account.encryption.ECKeyPair;

import org.spongycastle.crypto.AsymmetricCipherKeyPair;
import org.spongycastle.crypto.agreement.ECDHBasicAgreement;
import org.spongycastle.crypto.generators.ECKeyPairGenerator;
import org.spongycastle.crypto.params.ECDomainParameters;
import org.spongycastle.crypto.params.ECKeyGenerationParameters;
import org.spongycastle.crypto.params.ECPrivateKeyParameters;
import org.spongycastle.crypto.params.ECPublicKeyParameters;
import org.spongycastle.math.ec.ECCurve;
import org.spongycastle.math.ec.ECFieldElement;
import org.spongycastle.math.ec.ECPoint;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.Mac;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;
import java.math.BigInteger;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

public class EncryptionUtils {
    private static final String TAG = EncryptionUtils.class.getSimpleName();
    private static final SecureRandom secureRandom = new SecureRandom();

    public static class ECDH {
        private static final BigInteger q = new BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFF", 16);
        private static final BigInteger a = new BigInteger("FFFFFFFF00000001000000000000000000000000FFFFFFFFFFFFFFFFFFFFFFFC", 16);
        private static final BigInteger b = new BigInteger("5AC635D8AA3A93E7B3EBBD55769886BC651D06B0CC53B0F63BCE3C3E27D2604B", 16);
        private static final BigInteger n = new BigInteger("FFFFFFFF00000000FFFFFFFFFFFFFFFFBCE6FAADA7179E84F3B9CAC2FC632551", 16);

        private static final ECFieldElement x = new ECFieldElement.Fp(q, new BigInteger("6B17D1F2E12C4247F8BCE6E563A440F277037D812DEB33A0F4A13945D898C296", 16));
        private static final ECFieldElement y = new ECFieldElement.Fp(q, new BigInteger("4FE342E2FE1A7F9B8EE7EB4A7C0F9E162BCE33576B315ECECBB6406837BF51F5", 16));

        private static final ECCurve curve = new ECCurve.Fp(q, a, b);
        private static final ECPoint g = new ECPoint.Fp(curve, x, y, true);

        public static final ECDomainParameters DOMAIN_PARAMETERS = new ECDomainParameters(curve, g, n);

        public static ECKeyPair generateKeyPair() {
            ECKeyGenerationParameters keyParams = new ECKeyGenerationParameters(DOMAIN_PARAMETERS, secureRandom);
            ECKeyPairGenerator ecKeyPairGenerator = new ECKeyPairGenerator();
            ecKeyPairGenerator.init(keyParams);
            AsymmetricCipherKeyPair keyPair = ecKeyPairGenerator.generateKeyPair();
            return new ECKeyPair(keyPair);
        }

        public static ECPublicKeyParameters getPublicKey(byte[] publicKeyBytes) {
            ECPoint keyPoint = curve.decodePoint(publicKeyBytes);
            return new ECPublicKeyParameters(keyPoint, DOMAIN_PARAMETERS);
        }

        public static ECPublicKeyParameters getPublicKey(String publicKeyHex) {
            return getPublicKey(CMAccountUtils.decodeHex(publicKeyHex));
        }

        public static byte[] calculateSecret(ECPrivateKeyParameters privateKey, ECPublicKeyParameters publicKey) {
            ECDHBasicAgreement keyAgreement = new ECDHBasicAgreement();
            keyAgreement.init(privateKey);
            byte[] secretBytes = keyAgreement.calculateAgreement(publicKey).toByteArray();
            // Hash secret with SHA-256 to obtain AES key
            return CMAccountUtils.digestBytes("SHA-256", secretBytes);
        }
    }

    public static class PBKDF2 {
        public static byte[] getDerivedKey(String password, String salt) {
            char[] passwordChars = password.toCharArray();
            byte[] saltBytes = Base64.decode(salt, Base64.NO_WRAP);

            try {
                SecretKeyFactory keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA1");
                KeySpec keySpec = new PBEKeySpec(passwordChars, saltBytes, 1024, 256);
                SecretKey secretKey = keyFactory.generateSecret(keySpec);
                return secretKey.getEncoded();
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithmException", e);
                throw new AssertionError(e);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, "InvalidKeySpecException", e);
                throw new AssertionError(e);
            }
        }

        public static String getDerivedKeyBase64(String password, String salt) {
            return Base64.encodeToString(getDerivedKey(password, salt), Base64.NO_WRAP);
        }
    }

    public static class HMAC {
        public static String getSignature(byte[] key, String message) {
            try {
                Mac hmac = Mac.getInstance("HmacSHA256");
                Key secretKey = new SecretKeySpec(key, "HmacSHA256");
                hmac.init(secretKey);
                hmac.update(message.getBytes());
                return CMAccountUtils.encodeHex(hmac.doFinal());
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithmException", e);
                throw new AssertionError(e);
            } catch (InvalidKeyException e) {
                Log.e(TAG, "InvalidKeyException", e);
                throw new AssertionError(e);
            }
        }
    }

    public static class AES {
        public static String decrypt(byte[] ciphertext, byte[] key) {
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
                IvParameterSpec ivSpec = new IvParameterSpec(ciphertext, 0, cipher.getBlockSize());
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
                byte[] plaintext = cipher.doFinal(ciphertext, cipher.getBlockSize(), ciphertext.length - cipher.getBlockSize());

                return new String(plaintext);
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithimException", e);
                throw new AssertionError(e);
            } catch (NoSuchPaddingException e) {
                Log.e(TAG, "NoSuchPaddingException", e);
                throw new AssertionError(e);
            } catch (InvalidKeyException e) {
                Log.e(TAG, "InvalidKeyException", e);
                throw new AssertionError(e);
            } catch (IllegalBlockSizeException e) {
                Log.e(TAG, "IllegalBlockSizeException", e);
                throw new AssertionError(e);
            } catch (BadPaddingException e) {
                Log.e(TAG, "BadPaddingException", e);
                throw new AssertionError(e);
            } catch (InvalidAlgorithmParameterException e) {
                Log.e(TAG, "InvalidAlgorithmParameterException", e);
                throw new AssertionError(e);
            }
        }

        public static byte[] encrypt(String plaintext, byte[] key) {
            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec, getRandomIV());
                byte[] ciphertext = cipher.doFinal(plaintext.getBytes());
                byte[] initializationVector = cipher.getIV();

                // Combine IV and Ciphertext
                byte[] combined = new byte[initializationVector.length + ciphertext.length];
                System.arraycopy(initializationVector, 0, combined, 0, initializationVector.length);
                System.arraycopy(ciphertext, 0, combined, initializationVector.length, ciphertext.length);
                return combined;
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithimException", e);
                throw new AssertionError(e);
            } catch (NoSuchPaddingException e) {
                Log.e(TAG, "NoSuchPaddingException", e);
                throw new AssertionError(e);
            } catch (InvalidKeyException e) {
                Log.e(TAG, "InvalidKeyException", e);
                throw new AssertionError(e);
            } catch (IllegalBlockSizeException e) {
                Log.e(TAG, "IllegalBlockSizeException", e);
                throw new AssertionError(e);
            } catch (BadPaddingException e) {
                Log.e(TAG, "BadPaddingException", e);
                throw new AssertionError(e);
            } catch (InvalidAlgorithmParameterException e) {
                Log.e(TAG, "InvalidAlgorithmParameterException");
                throw new AssertionError(e);
            }
        }

        private static IvParameterSpec getRandomIV() {
            IvParameterSpec ivSpec = new IvParameterSpec(generateSalt(16));
            return ivSpec;
        }
    }

    public static byte[] generateSalt(int size) {
        byte[] salt = new byte[size];
        secureRandom.nextBytes(salt);
        return salt;
    }

    public static String generateSaltBase64(int size) {
        byte[] salt = generateSalt(size);
        return Base64.encodeToString(salt, Base64.NO_WRAP);
    }
}
