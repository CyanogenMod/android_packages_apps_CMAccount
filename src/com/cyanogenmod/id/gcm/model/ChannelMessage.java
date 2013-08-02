package com.cyanogenmod.id.gcm.model;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class ChannelMessage {
    private String command;
    private String device_id;
    private String session_id;
    private Message message;

    public ChannelMessage(String command, String device_id, String session_id, Message message) {
        this.command = command;
        this.device_id = device_id;
        this.session_id = session_id;
        this.message = message;
    }

    public static ChannelMessage fromJson(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(MessageTypeAdapterFactory.getInstance())
                .create();
        return gson.fromJson(json, ChannelMessage.class);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getSessionId() {
        return session_id;
    }
}
