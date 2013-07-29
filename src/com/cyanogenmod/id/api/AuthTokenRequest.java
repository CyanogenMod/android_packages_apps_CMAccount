package com.cyanogenmod.id.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;

import android.util.Log;

public class AuthTokenRequest extends CMIDRequest<AuthTokenResponse> {

    private static final String TAG = AuthTokenRequest.class.getSimpleName();

    public AuthTokenRequest(String id, String password, Response.Listener<AuthTokenResponse> listener, Response.ErrorListener errorListener) {
        super(AuthClient.AUTH_URI, listener, errorListener);
        addParameter(PARAM_GRANT_TYPE, PARAM_PASSWORD);
        addParameter(PARAM_EMAIL, id);
        addParameter(PARAM_PASSWORD, password);
        addHeader(PARAM_AUTHORIZATION, "Basic " + AuthClient.ENCODED_ID_SECRET);
    }

    public AuthTokenRequest(String refreshToken, Response.Listener<AuthTokenResponse> listener, Response.ErrorListener errorListener) {
        super(AuthClient.AUTH_URI, listener, errorListener);
        addParameter(PARAM_GRANT_TYPE, PARAM_REFRESH_TOKEN);
        addParameter(PARAM_REFRESH_TOKEN, refreshToken);
        addHeader(PARAM_AUTHORIZATION, "Basic " + AuthClient.ENCODED_ID_SECRET);
    }

    @Override
    protected Response<AuthTokenResponse> parseNetworkResponse(NetworkResponse response) {
        String jsonResponse = new String(response.data);
        if (CMID.DEBUG) Log.d(TAG, "jsonResponse=" + jsonResponse);
        try {
            AuthTokenResponse res = new Gson().fromJson(jsonResponse, AuthTokenResponse.class);
            return Response.success(res, getCacheEntry());
        } catch (JsonSyntaxException e) {
            return Response.error(new VolleyError(e));
        }
    }

}

