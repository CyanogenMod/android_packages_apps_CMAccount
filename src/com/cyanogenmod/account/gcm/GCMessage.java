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

package com.cyanogenmod.account.gcm;

import com.cyanogenmod.account.gcm.model.Account;
import com.cyanogenmod.account.gcm.model.Message;
import com.cyanogenmod.account.gcm.model.MessageTypeAdapterFactory;
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
