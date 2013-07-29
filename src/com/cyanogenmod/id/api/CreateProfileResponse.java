package com.cyanogenmod.id.api;

import com.cyanogenmod.id.api.CheckProfileResponse;

public class CreateProfileResponse {
    private Errors errors;
    private String first_name;
    private String last_name;
    private String id;
    private String email;

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return first_name;
    }

    public String getId() {
        return id;
    }

    public String getLastName() {
        return last_name;
    }

    public boolean hasErrors() {
        return errors != null;
    }

    public Errors getErrors() {
        return errors;
    }

    public static class Errors implements CheckProfileResponse {
        private static final String NA = "not_available";
        private String email;

        public String getEmail() {
            return email;
        }

        @Override
        public boolean emailAvailable() {
            return !NA.equals(email);
        }
    }
}
