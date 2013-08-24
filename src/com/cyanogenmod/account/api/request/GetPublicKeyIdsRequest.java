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
package com.cyanogenmod.account.api.request;

import android.content.Context;
import android.util.Log;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.api.CMAccountRequest;
import com.cyanogenmod.account.api.response.GetPublicKeyIdsResponse;
import com.cyanogenmod.account.auth.AuthClient;
import com.cyanogenmod.account.util.CMAccountUtils;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

public class GetPublicKeyIdsRequest extends CMAccountRequest<GetPublicKeyIdsResponse> {
    private static final String TAG = GetPublicKeyIdsRequest.class.getSimpleName();

    public GetPublicKeyIdsRequest(Context context, String authToken,
                                  Response.Listener<GetPublicKeyIdsResponse> listener, Response.ErrorListener errorListener) {
        super(AuthClient.GET_PUBLIC_KEY_IDS_URI, listener, errorListener);
        addHeader(PARAM_AUTHORIZATION, "OAuth " + authToken);
        addParameter(PARAM_DEVICE_ID, CMAccountUtils.getUniqueDeviceId(context));
    }

    @Override
    protected Response<GetPublicKeyIdsResponse> parseNetworkResponse(NetworkResponse response) {
        String jsonResponse = new String(response.data);
        if (CMAccount.DEBUG) Log.d(TAG, "jsonResponse=" + jsonResponse);
        try {
            GetPublicKeyIdsResponse res = new Gson().fromJson(jsonResponse, GetPublicKeyIdsResponse.class);
            res.setStatusCode(response.statusCode);
            return Response.success(res, getCacheEntry());
        } catch (JsonSyntaxException e) {
            return Response.error(new VolleyError(e));
        }
    }
}
