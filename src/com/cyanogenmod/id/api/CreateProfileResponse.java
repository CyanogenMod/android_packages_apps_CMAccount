package com.cyanogenmod.id.api;

public class CreateProfileResponse {
    private ErrorResponse[] errors;
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
        return errors != null && errors.length > 0;
    }

    public ErrorResponse[] getErrors() {
        return errors;
    }

}
