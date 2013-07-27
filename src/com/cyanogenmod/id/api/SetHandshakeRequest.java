package com.cyanogenmod.id.api;


import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;

import android.util.Log;

public class SetHandshakeRequest extends CMIDRequest<Integer> {

    private static final String TAG = SetHandshakeRequest.class.getSimpleName();

    public SetHandshakeRequest(String deviceId, String authToken, String command, String secret,
            Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        super(AuthClient.SET_HANDSHAKE_URI, listener, errorListener);
        addHeader(PARAM_AUTHORIZATION, "OAuth " + authToken);
        addParameter(PARAM_DID, deviceId);
        addParameter(PARAM_COMMAND, command);
        addParameter(PARAM_SECRET, secret);
        Log.d(TAG, PARAM_SECRET + " = " + secret);
    }

    @Override
    protected Response<Integer> parseNetworkResponse(NetworkResponse response) {
        if (CMID.DEBUG) Log.d(TAG, "response code=" + response.statusCode);
        if (response.statusCode == 200) {
            return Response.success(new Integer(response.statusCode), getCacheEntry());
        } else {
            return Response.error(new VolleyError(response));
        }
    }
}
