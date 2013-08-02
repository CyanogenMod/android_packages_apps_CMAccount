package com.cyanogenmod.id.gcm.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class PlaintextMessage {
    private String command;
    private String sequence;

    public PlaintextMessage(String command, String sequence) {
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

    public String getSequence() {
        return sequence;
    }
}
