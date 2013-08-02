package com.cyanogenmod.id.gcm;

import com.cyanogenmod.id.gcm.model.Account;
import com.cyanogenmod.id.gcm.model.Message;
import com.cyanogenmod.id.gcm.model.MessageTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GCMessage {

    private Account account;
    private String sessionId;
    private String command;
    private Message message;

    public static GCMessage fromJson(String json) {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(MessageTypeAdapterFactory.getInstance())
                .create();
        return gson.fromJson(json, GCMessage.class);
    }

    public String toJson() {
        Gson gson = new GsonBuilder()
                .registerTypeAdapterFactory(MessageTypeAdapterFactory.getInstance())
                .setPrettyPrinting()
                .create();
        return gson.toJson(this);
    }

    public Account getAccount() {
        return account;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getCommand() {
        return command;
    }

    public Message getMessage() {
        return message;
    }
}
