package com.cyanogenmod.id.gcm.model;

import android.util.Log;
import com.google.gson.Gson;
import com.google.gson.annotations.Expose;

import static com.cyanogenmod.id.util.EncryptionUtils.AES;

public class EncryptedMessage extends Message {
    @Expose
    private String ciphertext;

    @Expose
    private String iv;

    public EncryptedMessage() {
    }

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

    public String toJson() {
        Gson gson = new Gson();
        return gson.toJson(this);
    }

    public void encrypt(String symmetricKey) {
        String json = toJson();

        AES.CipherResult result = AES.encrypt(json, symmetricKey);
        ciphertext = result.getCiphertext();
        iv = result.getIv();
    }

}
