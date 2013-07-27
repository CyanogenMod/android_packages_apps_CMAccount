package com.cyanogenmod.id.api;


import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;

import android.util.Log;

public class SendStartingWipeRequest extends CMIDRequest<Integer> {

    private static final String TAG = SendStartingWipeRequest.class.getSimpleName();

    public SendStartingWipeRequest(String deviceId, String authToken,
            Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        super(AuthClient.SEND_WIPE_STARTED_URI, listener, errorListener);
        addHeader(PARAM_AUTHORIZATION, "OAuth " + authToken);
        addParameter("device_id", deviceId);
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
