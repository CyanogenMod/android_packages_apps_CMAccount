package com.cyanogenmod.id.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;

import android.util.Log;

public class CreateProfileRequest extends CMIDRequest<CreateProfileResponse> {

    private static final String TAG = CreateProfileRequest.class.getSimpleName();

    public CreateProfileRequest(String firstName, String lastName, String email, String username, String password, boolean termsOfService,
            Response.Listener<CreateProfileResponse> listener, Response.ErrorListener errorListener) {
        super(AuthClient.REGISTER_PROFILE_URI, listener, errorListener);
        addParameter(PARAM_USERNAME, username);
        addParameter(PARAM_PASSWORD, password);
        addParameter(PARAM_FIRST_NAME, firstName);
        addParameter(PARAM_LAST_NAME, lastName);
        addParameter(PARAM_EMAIL, email);
        addParameter(PARAM_TERMS, String.valueOf(termsOfService));
    }

    @Override
    protected Response<CreateProfileResponse> parseNetworkResponse(NetworkResponse response) {
        String jsonResponse = new String(response.data);
        if (CMID.DEBUG) Log.d(TAG, "jsonResponse=" + jsonResponse);
        try {
            CreateProfileResponse res = new Gson().fromJson(jsonResponse, CreateProfileResponse.class);
            return Response.success(res, getCacheEntry());
        } catch (JsonSyntaxException e) {
            return Response.error(new VolleyError(e));
        }
    }
}