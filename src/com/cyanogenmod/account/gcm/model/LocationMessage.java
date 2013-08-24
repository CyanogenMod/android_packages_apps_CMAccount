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

import android.location.Location;
import com.google.gson.annotations.Expose;

public class LocationMessage extends EncryptedMessage {
    private final String command = "device_location";
    private Params params;

    private static class Params {
        private double latitude;
        private double longitude;
        private float accuracy;

        public Params(final Location location) {
            this.latitude = location.getLatitude();
            this.longitude = location.getLongitude();
            this.accuracy = location.getAccuracy();
        }
    }

    public LocationMessage(final Location location, String keyId) {
        this.key_id = keyId;
        this.params = new Params(location);
    }
}
