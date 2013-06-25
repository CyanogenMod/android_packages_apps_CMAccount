package com.cyanogenmod.id.api;

public class PingResponse {

   int statusCode;
   private String status;

    public String getStatusMessage() {
        return status;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
