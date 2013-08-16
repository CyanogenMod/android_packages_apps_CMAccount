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

