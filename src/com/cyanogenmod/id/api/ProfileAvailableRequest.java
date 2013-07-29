package com.cyanogenmod.id.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;

import android.util.Log;

public class ProfileAvailableRequest extends CMIDRequest<ProfileAvailableResponse> {

    private static final String TAG = ProfileAvailableRequest.class.getSimpleName();

    public ProfileAvailableRequest(String email,
            Response.Listener<ProfileAvailableResponse> listener, Response.ErrorListener errorListener) {
        super(AuthClient.PROFILE_AVAILABLE_URI, listener, errorListener);
        if (email != null) addParameter(PARAM_EMAIL, email);
    }

    @Override
    protected Response<ProfileAvailableResponse> parseNetworkResponse(NetworkResponse response) {
        String jsonResponse = new String(response.data);
        if (CMID.DEBUG) Log.d(TAG, "jsonResponse=" + jsonResponse);
        try {
            ProfileAvailableResponse res = new Gson().fromJson(jsonResponse, ProfileAvailableResponse.class);
            return Response.success(res, getCacheEntry());
        } catch (JsonSyntaxException e) {
            return Response.error(new VolleyError(e));
        }
    }
}
