package com.cyanogenmod.id.encryption;

import android.util.Base64;
import android.util.Log;
import com.cyanogenmod.id.CMID;

import javax.crypto.*;
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
                Log.d(TAG, "NoSuchAlgorithimException", e);
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
                Log.d(TAG, "NoSuchAlgorithimException", e);
            } catch (InvalidKeySpecException e) {
                Log.d(TAG, "InvalidKeySpecException", e);
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
