package com.cyanogenmod.id.api;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;

import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.auth.AuthClient;
import com.cyanogenmod.id.gcm.GCMUtil;

import android.content.Context;
import android.os.Build;
import android.util.Log;

public class PingRequest extends CMIDRequest<PingResponse> {

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
    }

    @Override
    protected Response<PingResponse> parseNetworkResponse(NetworkResponse response) {
        String jsonResponse = new String(response.data);
        if (Constants.DEBUG) Log.d(TAG, "jsonResponse=" + jsonResponse);
        try {
            PingResponse res = new Gson().fromJson(jsonResponse, PingResponse.class);
            res.statusCode = response.statusCode;
            return Response.success(res, getCacheEntry());
        } catch (JsonSyntaxException e) {
            return Response.error(new VolleyError(e));
        }
    }
}
