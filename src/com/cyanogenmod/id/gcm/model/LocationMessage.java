package com.cyanogenmod.id.gcm.model;

import android.location.Location;
import android.util.Log;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;
import com.cyanogenmod.id.util.EncryptionUtils;
import com.google.gson.Gson;

public class LocationMessage extends EncryptedMessage {
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

    public LocationMessage(final Location location, int sequence) {
        this.params = new Params(location, sequence);
    }
}
