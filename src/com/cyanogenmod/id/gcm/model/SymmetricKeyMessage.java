package com.cyanogenmod.id.gcm.model;

public class SymmetricKeyMessage extends Message {
    private String symmetric_key;
    private String symmetric_key_verification;

    public SymmetricKeyMessage(String symmetric_key, String symmetric_key_verification) {
        this.symmetric_key = symmetric_key;
        this.symmetric_key_verification = symmetric_key_verification;
    }
}
