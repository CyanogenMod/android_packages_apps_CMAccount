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
        final TypeAdapter<SecureMessage> secureMessageAdapter = gson.getDelegateAdapter(this, TypeToken.get(SecureMessage.class));
        final TypeAdapter<JsonElement> elementAdapter = gson.getAdapter(JsonElement.class);

        TypeAdapter<Message> result = new TypeAdapter<Message>() {
            @Override
            public void write(JsonWriter out, Message value) throws IOException {
                if (value instanceof PublicKeyMessage) {
                    keyExchangeAdapter.write(out, (PublicKeyMessage) value);
                } else if (value instanceof SymmetricKeyMessage) {
                    symmetricKeyAdapter.write(out, (SymmetricKeyMessage) value);
                } else if (value instanceof SecureMessage) {
                    secureMessageAdapter.write(out, (SecureMessage) value);
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