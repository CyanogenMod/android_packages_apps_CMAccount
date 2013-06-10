package com.cyanogenmod.id.auth;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;

import java.util.HashMap;
import java.util.Map;

public abstract class CMIDRequest<T> extends Request<T> {

    private final Response.Listener<T> mListener;
    protected HashMap<String, String> mParams = new HashMap<String, String>();
    protected HashMap<String, String> mHeaders = new HashMap<String, String>();

    protected CMIDRequest(String uri, Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(Method.POST, uri, errorListener);
        mListener = listener;
    }

    @Override
    protected Map<String, String> getParams() throws AuthFailureError {
        return mParams;
    }

    @Override
    public Map<String, String> getHeaders() throws AuthFailureError {
        return mHeaders;
    }

    @Override
    protected void deliverResponse(T response) {
        mListener.onResponse(response);
    }

}
