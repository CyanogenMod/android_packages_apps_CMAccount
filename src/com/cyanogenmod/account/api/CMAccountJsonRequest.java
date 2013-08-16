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

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonRequest;

import java.util.HashMap;
import java.util.Map;

public abstract class CMAccountJsonRequest<T> extends JsonRequest<T> {

    public static final String PARAM_AUTHORIZATION = "Authorization";

    private final Response.Listener<T> mListener;
    private HashMap<String, String> mHeaders = new HashMap<String, String>();

    protected CMAccountJsonRequest(String uri, String requestBody, Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(Method.POST, uri, requestBody, listener, errorListener);
        mListener = listener;
    }

    public CMAccountJsonRequest<T> addHeader(String name, String value) {
        mHeaders.put(name, value);
        return this;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return mHeaders;
    }

    @Override
    protected void deliverResponse(T response) {
        if (mListener != null) mListener.onResponse(response);
    }
}
