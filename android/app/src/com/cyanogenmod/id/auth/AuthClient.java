package com.cyanogenmod.id.auth;

import com.google.gson.Gson;

import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.cyanogenmod.id.Constants;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

public class AuthClient {

    private static final String TAG = "AuthClient";

    public static final String PARAM_FIRST_NAME = "first_name";
    public static final String PARAM_LAST_NAME = "last_name";
    public static final String PARAM_EMAIL = "email";
    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_TERMS = "terms_of_service";
    public static final String PARAM_GRANT_TYPE = "grant_type";
    public static final String PARAM_REFRESH_TOKEN= "refresh_token";
    private static final int API_VERSION = 1;
    private static final String API_ROOT = "/apis/" + API_VERSION;
    private static final String PROFILE_SERVICE = "/profile";
    private static final String REGISTER_METHOD = "/register";
    private static final String AVAILABLE_METHOD = "/available";
    private static final String CMID_URI = "https://cmid-devel.appspot.com";
    private static final String AUTH_URI = CMID_URI + "/oauth2/token";
    private static final String AUTH_URI_TEST = "http://76.74.219.185:9999";
    private static final String REGISTER_PROFILE_URI = CMID_URI + API_ROOT + PROFILE_SERVICE + REGISTER_METHOD;
    private static final String PROFILE_AVAILABLE_URI = CMID_URI + API_ROOT + PROFILE_SERVICE + AVAILABLE_METHOD;

    private static final String CLIENT_ID = "8001";
    private static final String SECRET = "b93bb90299bb46f3bafdd6ca630c8f3c";
    private static final String ENCODED_ID_SECRET = new String(Base64.encode((CLIENT_ID + ":" + SECRET).getBytes(), Base64.NO_WRAP));

    private RequestQueue mRequestQueue;
    private VolleyError mVolleyError;
    private volatile boolean mRequestRunning = false;

    private static AuthClient sInstance;

    private AuthClient(Context context) {
       mRequestQueue = Volley.newRequestQueue(context);
    }

    public static final AuthClient getInstance(Context context) {
        if (sInstance == null) sInstance = new AuthClient(context);
        return sInstance;
    }

    public Request<?> login(String username, String password, Listener<AuthTokenResponse> listener, ErrorListener errorListener) {
        return mRequestQueue.add(new AuthTokenRequest(username, password, listener, errorListener));
    }

    public Request<?> refreshAccessToken(String refreshToken, Listener<AuthTokenResponse> listener, ErrorListener errorListener) {
        return mRequestQueue.add(new AuthTokenRequest(refreshToken, listener, errorListener));
    }

    public Request<?> createProfile(String firstName, String lastName, String email, String username, String password, boolean termsOfService,
            Listener<CreateProfileResponse> listener, ErrorListener errorListener) {
            return mRequestQueue.add(new CreateProfileRequest(firstName, lastName, email, username, password, termsOfService, listener, errorListener));
    }

    public Request<?> checkProfile(String email, String username, Listener<ProfileAvailableResponse> listener, ErrorListener errorListener) {
        return mRequestQueue.add(new ProfileAvailableRequest(email, username, listener, errorListener));
    }

    public AuthTokenResponse blockingRefreshAccessToken(String refreshToken) throws VolleyError {
        final AuthTokenResponse authResponse = new AuthTokenResponse();
        mRequestRunning = true;
        Request<?> inFlightRequest = refreshAccessToken(refreshToken, new Listener<AuthTokenResponse>() {
              @Override
              public void onResponse(AuthTokenResponse response) {
                  authResponse.copy(response);
                  mRequestRunning = false;
              }
          }, new ErrorListener() {
              @Override
              public void onErrorResponse(VolleyError error) {
                  String jsonResponse = new String(error.networkResponse.data);
                  if (Constants.DEBUG) Log.d(TAG, "error jsonResponse="+jsonResponse);
                  mVolleyError = error;
                  mRequestRunning = false;
              }
          });
          while (mRequestRunning) {
               // do nothing
          }
        try {
            if (mVolleyError != null) {
                throw new VolleyError(mVolleyError);
            }
        } finally {
            mVolleyError = null;
        }
        return authResponse;
    }

    public void addLocalAccount(AccountManager accountManager, Account account, AuthTokenResponse response) {
        accountManager.addAccountExplicitly(account, null, null);
        updateLocalAccount(accountManager, account, response);
    }

    public void updateLocalAccount(AccountManager accountManager, Account account, AuthTokenResponse response) {
        accountManager.setUserData(account, Constants.AUTHTOKEN_TYPE_ACCESS, response.getAccessToken());
        if (!TextUtils.isEmpty(response.getRefreshToken())) {
            accountManager.setUserData(account, Constants.AUTHTOKEN_TYPE_REFRESH, response.getRefreshToken());
        }
        accountManager.setUserData(account, Constants.AUTHTOKEN_EXPIRES_IN, String.valueOf(System.currentTimeMillis() + (Long.valueOf(response.getExpiresIn())*1000)));
        if (Constants.DEBUG) {
            Log.d(TAG, "Current Time="+System.currentTimeMillis());
            Log.d(TAG, "Expires in="+response.getExpiresIn());
        }
    }

    public static class AuthTokenRequest extends CMIDRequest<AuthTokenResponse> {

        private AuthTokenRequest(String username, String password, Listener<AuthTokenResponse> listener, ErrorListener errorListener) {
            super(AUTH_URI, listener, errorListener);
            mParams.put(PARAM_GRANT_TYPE, PARAM_PASSWORD);
            mParams.put(PARAM_USERNAME, username);
            mParams.put(PARAM_PASSWORD, password);
            mHeaders.put("Authorization", "Basic " + ENCODED_ID_SECRET);
        }

        private AuthTokenRequest(String refreshToken, Listener<AuthTokenResponse> listener, ErrorListener errorListener) {
            super(AUTH_URI, listener, errorListener);
            mParams.put(PARAM_GRANT_TYPE, PARAM_REFRESH_TOKEN);
            mParams.put(PARAM_REFRESH_TOKEN, refreshToken);
            mHeaders.put("Authorization", "Basic " + ENCODED_ID_SECRET);
        }

        @Override
        protected Response<AuthTokenResponse> parseNetworkResponse(NetworkResponse response) {
            String jsonResponse = new String(response.data);
            if (Constants.DEBUG) Log.d(TAG, "jsonResponse="+jsonResponse);
            final Gson gson = new Gson();
            AuthTokenResponse res = gson.fromJson(jsonResponse, AuthTokenResponse.class);
            return Response.success(res, getCacheEntry());
        }

    }

    public static class CreateProfileRequest extends CMIDRequest<CreateProfileResponse> {

        private CreateProfileRequest(String firstName, String lastName, String email, String username, String password, boolean termsOfService,
                Listener<CreateProfileResponse> listener, ErrorListener errorListener) {
            super(REGISTER_PROFILE_URI, listener, errorListener);
            mParams.put(PARAM_USERNAME, username);
            mParams.put(PARAM_PASSWORD, password);
            mParams.put(PARAM_FIRST_NAME, firstName);
            mParams.put(PARAM_LAST_NAME, lastName);
            mParams.put(PARAM_EMAIL, email);
            mParams.put(PARAM_TERMS, String.valueOf(termsOfService));
        }

        @Override
        protected Response<CreateProfileResponse> parseNetworkResponse(NetworkResponse response) {
            String jsonResponse = new String(response.data);
            if (Constants.DEBUG) Log.d(TAG, "jsonResponse="+jsonResponse);
            final Gson gson = new Gson();
            CreateProfileResponse res = gson.fromJson(jsonResponse, CreateProfileResponse.class);
            return Response.success(res, getCacheEntry());
        }
    }

    public static class ProfileAvailableRequest extends CMIDRequest<ProfileAvailableResponse> {

        private ProfileAvailableRequest(String email, String username,
                Listener<ProfileAvailableResponse> listener, ErrorListener errorListener) {
            super(PROFILE_AVAILABLE_URI, listener, errorListener);
            if (username != null) mParams.put(PARAM_USERNAME, username);
            if (email != null) mParams.put(PARAM_EMAIL, email);
        }

        @Override
        protected Response<ProfileAvailableResponse> parseNetworkResponse(NetworkResponse response) {
            String jsonResponse = new String(response.data);
            if (Constants.DEBUG) Log.d(TAG, "jsonResponse="+jsonResponse);
            final Gson gson = new Gson();
            ProfileAvailableResponse res = gson.fromJson(jsonResponse, ProfileAvailableResponse.class);
            return Response.success(res, getCacheEntry());
        }
    }
}
