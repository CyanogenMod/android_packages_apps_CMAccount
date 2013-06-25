package com.cyanogenmod.id.api;

public class AuthTokenResponse {
    private String access_token;
    private String refresh_token;
    private String expires_in;

    public String getAccessToken() {
        return access_token;
    }

    public String getExpiresIn() {
        return expires_in;
    }

    public String getRefreshToken() {
        return refresh_token;
    }

    public void copy(AuthTokenResponse authResponse) {
        access_token = authResponse.access_token;
        refresh_token = authResponse.refresh_token;
        expires_in = authResponse.expires_in;
    }
}
