package com.cyanogenmod.id.api.request;

import com.cyanogenmod.id.gcm.model.Message;
import com.cyanogenmod.id.gcm.model.MessageTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class SendChannelRequestBody {
    private String command;
    private String device_id;
    private String session_id;
    private Message message;

    public SendChannelRequestBody(String command, String device_id, String session_id, Message message) {
        this.command = command;
        this.device_id = device_id;
        this.session_id = session_id;
        this.message = message;
    }

    public static SendChannelRequestBody fromJson(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(MessageTypeAdapterFactory.getInstance())
                .create();
        return gson.fromJson(json, SendChannelRequestBody.class);
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public String getSessionId() {
        return session_id;
    }
}
