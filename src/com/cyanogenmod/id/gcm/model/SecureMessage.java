package com.cyanogenmod.id.gcm.model;

public class SecureMessage extends Message {
    private String ciphertext;
    private String iv;

    public SecureMessage(String ciphertext, String iv) {
        this.ciphertext = ciphertext;
        this.iv = iv;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public String getIV() {
        return iv;
    }
}
