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

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.account.auth.AuthClient;

public class GetMinimumAppVersionRequest extends Request<String> {
    private Response.Listener<String> mListener;

    public GetMinimumAppVersionRequest(Response.Listener<String> listener, Response.ErrorListener errorListener) {
        super(Method.GET, AuthClient.GET_MINIMUM_APP_VERSION_URI, errorListener);
        mListener = listener;
    }

    @Override
    protected Response<String> parseNetworkResponse(NetworkResponse networkResponse) {
        if (networkResponse.statusCode == 200) {
            String response = new String(networkResponse.data);
            return Response.success(response, getCacheEntry());
        } else {
            return Response.error(new VolleyError("Unexpected status code " + networkResponse.statusCode));
        }
    }

    @Override
    protected void deliverResponse(String response) {
        if (mListener != null) mListener.onResponse(response);
    }
}
