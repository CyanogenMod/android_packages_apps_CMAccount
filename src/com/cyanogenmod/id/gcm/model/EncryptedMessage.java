package com.cyanogenmod.id.gcm.model;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import static com.cyanogenmod.id.util.EncryptionUtils.AES;

public class EncryptedMessage extends Message {
    @Expose
    private String ciphertext;

    @Expose
    private String initializationVector;

    public EncryptedMessage() {
    }

    public EncryptedMessage(String ciphertext, String initializationVector) {
        this.ciphertext = ciphertext;
        this.initializationVector = initializationVector;
    }

    public String getCiphertext() {
        return ciphertext;
    }

    public String getIV() {
        return initializationVector;
    }

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public void encrypt(String symmetricKey) {
        String json = toJson();

        AES.CipherResult result = AES.encrypt(json, symmetricKey);
        ciphertext = result.getCiphertext();
        initializationVector = result.getInitializationVector();
    }

}
