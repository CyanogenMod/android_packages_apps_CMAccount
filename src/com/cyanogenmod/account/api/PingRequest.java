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

package com.cyanogenmod.account.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.auth.AuthClient;
import com.cyanogenmod.account.gcm.GCMUtil;
import com.cyanogenmod.account.util.CMAccountUtils;

import android.content.Context;
import android.os.Build;
import android.util.Log;

public class PingRequest extends CMAccountRequest<PingResponse> {

    private static final String TAG = PingRequest.class.getSimpleName();

    public PingRequest(Context context, String deviceId, String authToken, String carrier,
            Response.Listener<PingResponse> listener, Response.ErrorListener errorListener) {
        super(AuthClient.PING_URI, listener, errorListener);
        addHeader(PARAM_AUTHORIZATION, "OAuth " + authToken);
        addParameter(PARAM_DID, deviceId);
        addParameter(PARAM_PUSH_ID, "gcm:" + GCMUtil.getRegistrationId(context));
        addParameter(PARAM_MAKE, Build.MANUFACTURER);
        addParameter(PARAM_MODEL, Build.MODEL);
        addParameter(PARAM_CARRIER, carrier);
        addParameter(PARAM_ANDROID_VERSION, Build.VERSION.RELEASE);
        addParameter(PARAM_CM_VERSION, CMAccountUtils.getDisplayVersion());
        addParameter(PARAM_SALT, CMAccountUtils.getDeviceSalt(context));
    }

    @Override
    protected Response<PingResponse> parseNetworkResponse(NetworkResponse response) {
        String jsonResponse = new String(response.data);
        if (CMAccount.DEBUG) Log.d(TAG, "jsonResponse=" + jsonResponse);
        try {
            PingResponse res = new Gson().fromJson(jsonResponse, PingResponse.class);
            res.statusCode = response.statusCode;
            return Response.success(res, getCacheEntry());
        } catch (JsonSyntaxException e) {
            return Response.error(new VolleyError(e));
        }
    }
}
