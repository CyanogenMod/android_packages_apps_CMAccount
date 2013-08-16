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

package com.cyanogenmod.id.gcm.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PlaintextMessage extends Message {
    private String command;
    private int sequence;

    public PlaintextMessage(String command, int sequence) {
        this.command = command;
        this.sequence = sequence;
    }

    public static PlaintextMessage fromJson(String json) {
        Gson gson = new GsonBuilder().create();
        return gson.fromJson(json, PlaintextMessage.class);
    }

    public String toJson() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        return gson.toJson(this);
    }

    public String getCommand() {
        return command;
    }

    public int getSequence() {
        return sequence;
    }
}
