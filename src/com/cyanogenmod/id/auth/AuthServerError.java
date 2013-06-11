package com.cyanogenmod.id.auth;

public class AuthServerError {
    private String error_description;
    private String error;

    public AuthServerError(){}

    public AuthServerError(String error, String error_description) {
        this.error = error;
        this.error_description = error_description;
    }

    public String getError() {
        return error;
    }

    public String getErrorDescription() {
        return error_description;
    }
}
