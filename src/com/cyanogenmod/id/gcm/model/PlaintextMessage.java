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
