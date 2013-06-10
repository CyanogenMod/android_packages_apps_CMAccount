package com.cyanogenmod.id.auth;

public class CreateProfileResponse {
    private Errors errors;
    private String username;
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

    public String getUsername() {
        return username;
    }

    public boolean hasErrors() {
        return errors != null;
    }

    public Errors getErrors() {
        return errors;
    }

    public static class Errors implements CheckProfileResponse {
        private static final String NA = "not_available";
        private String username;
        private String email;

        public String getEmail() {
            return email;
        }

        public String getUsername() {
            return username;
        }

        @Override
        public boolean emailAvailable() {
            return !NA.equals(username);
        }

        @Override
        public boolean usernameAvailable() {
            return !NA.equals(email);
        }
    }
}
