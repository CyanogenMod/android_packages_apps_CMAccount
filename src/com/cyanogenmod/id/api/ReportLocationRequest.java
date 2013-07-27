package com.cyanogenmod.id.api;


import com.android.volley.NetworkResponse;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;

import android.util.Log;

public class ReportLocationRequest extends CMIDRequest<Integer> {

    private static final String TAG = ReportLocationRequest.class.getSimpleName();

    public ReportLocationRequest(String deviceId, String authToken, double latitude, double longitude, float accuracy,
            Response.Listener<Integer> listener, Response.ErrorListener errorListener) {
        super(AuthClient.REPORT_LOCATION_URI, listener, errorListener);
        addHeader(PARAM_AUTHORIZATION, "OAuth " + authToken);
        addParameter(PARAM_DID, deviceId);
        addParameter(PARAM_LATITUDE, String.valueOf(latitude));
        addParameter(PARAM_LONGITUDE, String.valueOf(longitude));
        addParameter(PARAM_ACCURACY, String.valueOf(accuracy));
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
