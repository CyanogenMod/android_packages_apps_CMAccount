package com.cyanogenmod.id.gcm.model;

public class PublicKeyMessage extends Message {
    private String public_key;
    private String public_key_hash;

    public String getPublicKey() {
        return public_key;
    }

    public String getPublicKeyHash() {
        return public_key_hash;
    }
}
