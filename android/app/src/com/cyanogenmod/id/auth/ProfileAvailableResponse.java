package com.cyanogenmod.id.auth;

public class ProfileAvailableResponse implements CheckProfileResponse {
    private boolean username = true;
    private boolean email = true;

    public boolean emailAvailable() {
        return email;
    }

    public boolean usernameAvailable() {
        return username;
    }
}
