package com.cyanogenmod.id.gcm;

import com.cyanogenmod.id.gcm.model.Account;
import com.cyanogenmod.id.gcm.model.Message;
import com.cyanogenmod.id.gcm.model.MessageTypeAdapterFactory;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class GCMessage {

    private Account account;
    private String session_id;
    private String command;
    private Message message;

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
        return session_id;
    }

    public String getCommand() {
        return command;
    }

    public Message getMessage() {
        return message;
    }
}
