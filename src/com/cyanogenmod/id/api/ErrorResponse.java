package com.cyanogenmod.id.api;

public class ErrorResponse {

    public static final int ERROR_CODE_INVALID_EMAIL_FORMAT = 1;
    public static final int ERROR_CODE_EMAIL_IN_USE = 10;

    private String message;
    private int code;

    public String getMessage() {
        return message;
    }

    public int getCode() {
        return code;
    }
}
