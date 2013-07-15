package com.cyanogenmod.id.api;

import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.Response;

import java.util.HashMap;
import java.util.Map;

public abstract class CMIDRequest<T> extends Request<T> {

    public static final String PARAM_AUTHORIZATION = "Authorization";
    public static final String PARAM_FIRST_NAME = "first_name";
    public static final String PARAM_LAST_NAME = "last_name";
    public static final String PARAM_EMAIL = "email";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_TERMS = "terms_of_service";
    public static final String PARAM_GRANT_TYPE = "grant_type";
    public static final String PARAM_REFRESH_TOKEN= "refresh_token";
    public static final String PARAM_DID= "did";
    public static final String PARAM_PUSH_ID= "push_id";
    public static final String PARAM_MAKE= "make";
    public static final String PARAM_MODEL= "model";
    public static final String PARAM_OS_VERSION= "os_version";
    public static final String PARAM_CARRIER= "carrier";
    public static final String PARAM_LATITUDE= "latitude";
    public static final String PARAM_LONGITUDE= "longitude";

    private final Response.Listener<T> mListener;
    private HashMap<String, String> mParams = new HashMap<String, String>();
    private HashMap<String, String> mHeaders = new HashMap<String, String>();

    protected CMIDRequest(String uri, Response.Listener<T> listener, Response.ErrorListener errorListener) {
        super(Method.POST, uri, errorListener);
        mListener = listener;
    }

    public CMIDRequest<T> addHeader(String name, String value) {
        mHeaders.put(name, value);
        return this;
    }

    public CMIDRequest<T> addParameter(String name, String value) {
        mParams.put(name, value);
        return this;
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
        if (mListener != null) mListener.onResponse(response);
    }

}
