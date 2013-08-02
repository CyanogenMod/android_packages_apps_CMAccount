package com.cyanogenmod.id.util;

import android.util.Base64;
import android.util.Log;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.gcm.model.EncryptedMessage;

import javax.crypto.*;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;

public class EncryptionUtils {
    private static final String TAG = EncryptionUtils.class.getSimpleName();

    public static class AES {
        public static String generateAesKey() {
            try {
                KeyGenerator keyGenerator = KeyGenerator.getInstance("AES");
                SecureRandom secureRandom = new SecureRandom();
                keyGenerator.init(128, secureRandom);
                byte[] symmetricKey = keyGenerator.generateKey().getEncoded();
                return Base64.encodeToString(symmetricKey, Base64.DEFAULT).replace("\n", "");
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithimException", e);
            }
            return null;
        }

        public static String decrypt(String _ciphertext, String _key, String _iv) {
            byte[] key = Base64.decode(_key, Base64.DEFAULT);
            byte[] iv = Base64.decode(_iv, Base64.DEFAULT);
            byte[] ciphertext = Base64.decode(_ciphertext, Base64.DEFAULT);

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(iv);

            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
                byte[] plaintext = cipher.doFinal(ciphertext);

                return new String(plaintext);
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithimException", e);
            } catch (NoSuchPaddingException e) {
                Log.e(TAG, "NoSuchPaddingException", e);
            } catch (InvalidKeyException e) {
                Log.e(TAG, "InvalidKeyException", e);
            } catch (IllegalBlockSizeException e) {
                Log.e(TAG, "IllegalBlockSizeException", e);
            } catch (BadPaddingException e) {
                Log.e(TAG, "BadPaddingException", e);
            } catch (InvalidAlgorithmParameterException e) {
                Log.e(TAG, "InvalidAlgorithmParameterException", e);
            }

            return null;
        }

        public static EncryptedMessage encrypt(String plaintext, String _key) {
            byte[] key = Base64.decode(_key, Base64.DEFAULT);

            SecretKeySpec keySpec = new SecretKeySpec(key, "AES");

            try {
                Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                cipher.init(Cipher.ENCRYPT_MODE, keySpec);
                byte[] iv = cipher.getIV();
                byte[] ciphertext = cipher.doFinal(plaintext.getBytes());

                String encodedCiphertext = Base64.encodeToString(ciphertext, Base64.DEFAULT).replace("\n", "");
                String encodedIv = Base64.encodeToString(iv, Base64.DEFAULT).replace("\n", "");

                return new EncryptedMessage(encodedCiphertext, encodedIv);
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithimException", e);
            } catch (NoSuchPaddingException e) {
                Log.e(TAG, "NoSuchPaddingException", e);
            } catch (InvalidKeyException e) {
                Log.e(TAG, "InvalidKeyException", e);
            } catch (IllegalBlockSizeException e) {
                Log.e(TAG, "IllegalBlockSizeException", e);
            } catch (BadPaddingException e) {
                Log.e(TAG, "BadPaddingException", e);
            }
            return null;
        }
    }

    public static class RSA {

        private static PublicKey getPublicKey(String publicKey) {
            try {
                if (CMID.DEBUG) Log.d(TAG, "Building public key from PEM = " + publicKey.toString());
                KeyFactory keyFactory = KeyFactory.getInstance("RSA");
                return keyFactory.generatePublic(new X509EncodedKeySpec(Base64.decode(publicKey.toString(), Base64.DEFAULT)));
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithimException", e);
            } catch (InvalidKeySpecException e) {
                Log.e(TAG, "InvalidKeySpecException", e);
            }
            return null;
        }

        public static String encrypt(String _publicKey, String data) {
            PublicKey publicKey = getPublicKey(_publicKey);

            try {
                Cipher cipher = Cipher.getInstance("RSA/ECB/PKCS1Padding");
                cipher.init(Cipher.ENCRYPT_MODE, publicKey);
                byte[] result = cipher.doFinal(data.getBytes());
                return Base64.encodeToString(result, Base64.DEFAULT).replace("\n", "");
            } catch (NoSuchAlgorithmException e) {
                Log.e(TAG, "NoSuchAlgorithimException", e);
            } catch (NoSuchPaddingException e) {
                Log.e(TAG, "NoSuchPaddingException", e);
            } catch (InvalidKeyException e) {
                Log.e(TAG, "InvalidKeyException", e);
            } catch (IllegalBlockSizeException e) {
                Log.e(TAG, "IllegalBlockSizeException", e);
            } catch (BadPaddingException e) {
                Log.e(TAG, "BadPaddingException");
            }
            return null;
        }
    }
}
