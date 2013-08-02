package com.cyanogenmod.id.api;

import com.android.volley.AuthFailureError;
import com.android.volley.Response;
import com.android.volley.toolbox.JsonRequest;

import java.util.HashMap;
import java.util.Map;

public abstract class CMIDJsonRequest<T> extends JsonRequest<T> {

    public static final String PARAM_AUTHORIZATION = "Authorization";

    private final Response.Listener<T> mListener;
    private HashMap<String, String> mHeaders = new HashMap<String, String>();

    protected CMIDJsonRequest(String uri, String requestBody, Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(Method.POST, uri, requestBody, listener, errorListener);
        mListener = listener;
    }

    public CMIDJsonRequest<T> addHeader(String name, String value) {
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
