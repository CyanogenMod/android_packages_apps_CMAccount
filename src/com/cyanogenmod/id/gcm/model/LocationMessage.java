package com.cyanogenmod.id.gcm.model;

import android.location.Location;
import android.util.Log;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;
import com.cyanogenmod.id.encryption.EncryptionUtils;
import com.google.gson.Gson;

public class LocationMessage {

    private static final String TAG = LocationMessage.class.getSimpleName();

    private final String command = "device_location";
    private Params params;

    private static class Params {
        private double latitude;
        private double longitude;
        private float accuracy;
        private int sequence;

        public Params(final Location location, int sequence) {
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
            this.accuracy = location.getAccuracy();
            this.sequence = sequence;
        }
    }

    public LocationMessage(final Location location, final int sequence) {
        this.params = new Params(location, sequence);
    }

    public static SecureMessage getEncrypted(final Location location, AuthClient authClient, String sessionId, int sequence) {
        LocationMessage locationMessage = new LocationMessage(location, sequence);

        // Load AES key from database
        String aesKey = authClient.getSymmetricKey(sessionId);
        if (aesKey == null) {
            if (CMID.DEBUG) Log.d(TAG, "Unable to load AES key for session:" + sessionId);
            return null;
        }

        // Encrypt the JSON version of the LocationMessage
        String json = locationMessage.toJson();
        EncryptionUtils.AES.CiphertextIvPair result = EncryptionUtils.AES.encrypt(json, aesKey);
        SecureMessage secureMessage = null;
        if (result != null) {
            secureMessage = new SecureMessage(result.getCiphertext(), result.getIV());
        }
        return secureMessage;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }
}
