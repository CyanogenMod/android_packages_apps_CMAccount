package com.cyanogenmod.id.gcm.model;

public class EncryptedMessage extends Message {
    private String ciphertext;
    private String iv;

    public EncryptedMessage(String ciphertext, String iv) {
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
