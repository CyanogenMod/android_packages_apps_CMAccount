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

import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import com.google.gson.stream.JsonWriter;

import java.io.IOException;

public class MessageTypeAdapterFactory implements TypeAdapterFactory {

    private static MessageTypeAdapterFactory mInstance;

    public static MessageTypeAdapterFactory getInstance() {
        if (mInstance == null) {
            mInstance = new MessageTypeAdapterFactory();
        }

        return mInstance;
    }

    @Override
    public <T> TypeAdapter<T> create(Gson gson, TypeToken<T> type) {
        if (!Message.class.isAssignableFrom(type.getRawType())) {
            return null;
        }

        final TypeAdapter<Message> rootAdapter = gson.getDelegateAdapter(this, TypeToken.get(Message.class));
        final TypeAdapter<PublicKeyMessage> keyExchangeAdapter = gson.getDelegateAdapter(this, TypeToken.get(PublicKeyMessage.class));
        final TypeAdapter<SymmetricKeyMessage> symmetricKeyAdapter = gson.getDelegateAdapter(this, TypeToken.get(SymmetricKeyMessage.class));
        final TypeAdapter<EncryptedMessage> secureMessageAdapter = gson.getDelegateAdapter(this, TypeToken.get(EncryptedMessage.class));
        final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

        TypeAdapter<Message> result = new TypeAdapter<Message>() {
            @Override
            public void write(JsonWriter out, Message value) throws IOException {
                if (value instanceof PublicKeyMessage) {
                    keyExchangeAdapter.write(out, (PublicKeyMessage) value);
                } else if (value instanceof SymmetricKeyMessage) {
                    symmetricKeyAdapter.write(out, (SymmetricKeyMessage) value);
                } else if (value instanceof EncryptedMessage) {
                    secureMessageAdapter.write(out, (EncryptedMessage) value);
                } else {
                    JsonObject object = rootAdapter.toJsonTree(value).getAsJsonObject();
                    elementAdapter.write(out, object);
                }
            }

            @Override
            public Message read(JsonReader in) throws IOException {
                JsonObject object = elementAdapter.read(in).getAsJsonObject();
                if (object.has("public_key")) {
                    return keyExchangeAdapter.fromJsonTree(object);
                } else if (object.has("symmetric_key")) {
                    return symmetricKeyAdapter.fromJsonTree(object);
                } else if (object.has("ciphertext")) {
                    return secureMessageAdapter.fromJsonTree(object);
                } else {
                    return rootAdapter.fromJsonTree(object);
                }
            }
        }.nullSafe();

        return (TypeAdapter<T>) result;
    }
}