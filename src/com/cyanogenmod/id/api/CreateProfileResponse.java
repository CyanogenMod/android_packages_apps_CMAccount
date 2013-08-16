/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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
