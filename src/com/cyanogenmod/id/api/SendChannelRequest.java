package com.cyanogenmod.id.api;

import android.util.Log;
import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;
import com.cyanogenmod.id.api.request.SendChannelRequestBody;

public class SendChannelRequest extends CMIDJsonRequest<Integer> {

    private static final String TAG = SendChannelRequest.class.getSimpleName();

    public SendChannelRequest(String authToken, SendChannelRequestBody message, Response.Listener<Integer> listener,
            Response.ErrorListener errorListener) {
        super(AuthClient.SEND_CHANNEL_URI, message.toJson(), listener, errorListener);
        addHeader(PARAM_AUTHORIZATION, "OAuth " + authToken);
    }

    @Override
    protected Response<Integer> parseNetworkResponse(NetworkResponse response) {
        if (CMID.DEBUG) Log.d(TAG, "response code=" + response.statusCode);
        if (CMID.DEBUG) Log.d(TAG, "response content = " + new String(response.data));
        if (response.statusCode == 200) {
            return Response.success(new Integer(response.statusCode), getCacheEntry());
        } else {
            return Response.error(new VolleyError(response));
        }
    }
}
