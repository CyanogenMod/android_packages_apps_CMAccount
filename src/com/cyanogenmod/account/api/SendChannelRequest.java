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

import android.util.Log;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.auth.AuthClient;
import com.cyanogenmod.account.api.request.SendChannelRequestBody;

public class SendChannelRequest extends CMAccountJsonRequest<Integer> {

    private static final String TAG = SendChannelRequest.class.getSimpleName();

    public SendChannelRequest(String authToken, String message, Response.Listener<Integer> listener,
            Response.ErrorListener errorListener) {
        super(AuthClient.SEND_CHANNEL_URI, message, listener, errorListener);
        addHeader(PARAM_AUTHORIZATION, "OAuth " + authToken);
    }

    @Override
    protected Response<Integer> parseNetworkResponse(NetworkResponse response) {
        if (CMAccount.DEBUG) Log.d(TAG, "response code=" + response.statusCode);
        if (CMAccount.DEBUG) Log.d(TAG, "response content = " + new String(response.data));
        if (response.statusCode == 200) {
            return Response.success(new Integer(response.statusCode), getCacheEntry());
        } else {
            return Response.error(new VolleyError(response));
        }
    }
}
