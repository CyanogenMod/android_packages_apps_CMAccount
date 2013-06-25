package com.cyanogenmod.id.auth;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.Volley;
import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.api.AuthTokenRequest;
import com.cyanogenmod.id.api.AuthTokenResponse;
import com.cyanogenmod.id.api.CreateProfileRequest;
import com.cyanogenmod.id.api.CreateProfileResponse;
import com.cyanogenmod.id.api.PingRequest;
import com.cyanogenmod.id.api.PingResponse;
import com.cyanogenmod.id.api.PingService;
import com.cyanogenmod.id.api.ProfileAvailableRequest;
import com.cyanogenmod.id.api.ProfileAvailableResponse;
import com.cyanogenmod.id.api.ReportLocationRequest;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.sql.Timestamp;

public class AuthClient {

    private static final String TAG = AuthClient.class.getSimpleName();
    private static final int API_VERSION = 1;
    private static final String API_ROOT = "/apis/" + API_VERSION;
    private static final String PROFILE_METHOD = "/profile";
    private static final String REGISTER_METHOD = "/register";
    private static final String AVAILABLE_METHOD = "/available";
    private static final String DEVICE_METHOD = "/device";
    private static final String PING_METHOD = "/ping";
    private static final String REPORT_LOCATION_METHOD = "/report_location";
    private static final String CMID_URI = "https://cmid-devel.appspot.com";

    public static final String AUTH_URI = CMID_URI + "/oauth2/token";
    public static final String REGISTER_PROFILE_URI = CMID_URI + API_ROOT + PROFILE_METHOD + REGISTER_METHOD;
    public static final String PROFILE_AVAILABLE_URI = CMID_URI + API_ROOT + PROFILE_METHOD + AVAILABLE_METHOD;
    public static final String PING_URI = CMID_URI + API_ROOT + DEVICE_METHOD + PING_METHOD;
    public static final String REPORT_LOCATION_URI = CMID_URI + API_ROOT + DEVICE_METHOD + REPORT_LOCATION_METHOD;

    private static final String CLIENT_ID = "8001";
    private static final String SECRET = "b93bb90299bb46f3bafdd6ca630c8f3c";

    public static final String ENCODED_ID_SECRET = new String(Base64.encode((CLIENT_ID + ":" + SECRET).getBytes(), Base64.NO_WRAP));

    private RequestQueue mRequestQueue;
    private VolleyError mVolleyError;
    private volatile boolean mRequestRunning = false;

    private static AuthClient sInstance;
    private Context mContext;

    private AuthClient(Context context) {
        mContext = context.getApplicationContext();
        mRequestQueue = Volley.newRequestQueue(mContext);
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

    public Request<?> pingService(String authToken, Listener<PingResponse> listener, ErrorListener errorListener) {
        return mRequestQueue.add(new PingRequest(mContext, getAndroidID(), authToken, getCarrierName(), listener, errorListener));
    }

    public Request<?> reportLocation(String authToken, double latitude, double longitude, Listener<Integer> listener, ErrorListener errorListener) {
        return mRequestQueue.add(new ReportLocationRequest(getAndroidID(), authToken, latitude, longitude, listener, errorListener));
    }

    /*
     * XXX: This is a kludge. FIXIT
     */
    public AuthTokenResponse blockingRefreshAccessToken(String refreshToken) throws VolleyError {
        final AuthTokenResponse authResponse = new AuthTokenResponse();
        mRequestRunning = true;
        refreshAccessToken(refreshToken, new Listener<AuthTokenResponse>() {
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
        PingService.pingServer(mContext, account);
    }

    public void updateLocalAccount(AccountManager accountManager, Account account, AuthTokenResponse response) {
        accountManager.setUserData(account, Constants.AUTHTOKEN_TYPE_ACCESS, response.getAccessToken());
        if (!TextUtils.isEmpty(response.getRefreshToken())) {
            accountManager.setUserData(account, Constants.AUTHTOKEN_TYPE_REFRESH, response.getRefreshToken());
        }
        accountManager.setUserData(account, Constants.AUTHTOKEN_EXPIRES_IN, String.valueOf(System.currentTimeMillis() + (Long.valueOf(response.getExpiresIn())*1000)));
        if (Constants.DEBUG) {
            Log.d(TAG, "Current Time = " + new Timestamp(System.currentTimeMillis()));
            Log.d(TAG, "Expires in = " + response.getExpiresIn() + "ms");
        }
    }

    private String getAndroidID() {
        return android.provider.Settings.Secure.getString(mContext.getContentResolver(),
                android.provider.Settings.Secure.ANDROID_ID);
    }

    private String getCarrierName() {
        TelephonyManager manager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return manager.getNetworkOperatorName();
    }

    public SharedPreferences getAuthPreferences() {
        return mContext.getSharedPreferences(Constants.AUTH_PREFERENCES,
                Context.MODE_PRIVATE);
    }

}
