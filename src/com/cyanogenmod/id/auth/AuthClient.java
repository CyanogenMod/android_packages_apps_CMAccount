package com.cyanogenmod.id.auth;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response.ErrorListener;
import com.android.volley.Response.Listener;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.RequestFuture;
import com.android.volley.toolbox.Volley;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.api.*;
import com.cyanogenmod.id.gcm.GCMUtil;
import com.cyanogenmod.id.api.request.SendChannelRequestBody;
import com.cyanogenmod.id.gcm.model.WipeStartedMessage;
import com.cyanogenmod.id.provider.CMIDProvider;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OnAccountsUpdateListener;
import android.accounts.OperationCanceledException;
import android.app.admin.DevicePolicyManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.os.Handler;
import android.os.PowerManager;
import android.os.SystemProperties;
import android.telephony.TelephonyManager;
import android.text.TextUtils;
import android.util.Base64;
import android.util.Log;

import java.io.IOException;
import java.util.concurrent.ExecutionException;

public class AuthClient {

    private static final String TAG = AuthClient.class.getSimpleName();
    private static final int API_VERSION = 1;
    private static final String API_ROOT = "/api/v" + API_VERSION;
    private static final String PROFILE_METHOD = "/profile";
    private static final String REGISTER_METHOD = "/register";
    private static final String AVAILABLE_METHOD = "/available";
    private static final String DEVICE_METHOD = "/device";
    private static final String PING_METHOD = "/ping";
    private static final String SECMSG_METHOD = "/secmsg";
    private static final String SEND_CHANNEL_METHOD = "/send_channel";
    private static final String CMID_URI = "https://cmid-devel.appspot.com";
    private static final String SERVER_URI = getServerURI();

    public static final String AUTH_URI = SERVER_URI + "/oauth2/token";
    public static final String REGISTER_PROFILE_URI = SERVER_URI + API_ROOT + PROFILE_METHOD + REGISTER_METHOD;
    public static final String PROFILE_AVAILABLE_URI = SERVER_URI + API_ROOT + PROFILE_METHOD + AVAILABLE_METHOD;
    public static final String PING_URI = SERVER_URI + API_ROOT + DEVICE_METHOD + PING_METHOD;
    public static final String SEND_CHANNEL_URI = SERVER_URI + API_ROOT + SECMSG_METHOD + SEND_CHANNEL_METHOD;

    private static final String CLIENT_ID = "8001";
    private static final String SECRET = "b93bb90299bb46f3bafdd6ca630c8f3c";

    public static final String ENCODED_ID_SECRET = new String(Base64.encode((CLIENT_ID + ":" + SECRET).getBytes(), Base64.NO_WRAP));

    private RequestQueue mRequestQueue;

    private static AuthClient sInstance;
    private Context mContext;
    private AccountManager mAccountManager;

    private Request<?> mInFlightPingRequest;
    private Request<?> mInFlightTokenRequest;
    private Request<?> mInFlightStartWipeRequest;
    private Request<?> mInFlightAuthTokenRequest;
    private Request<?> mInFlightChannelRequest;

    private OnAccountsUpdateListener mAccountsUpdateListener;

    private AuthClient(Context context) {
        mContext = context.getApplicationContext();
        mAccountManager = AccountManager.get(mContext);
        mRequestQueue = Volley.newRequestQueue(mContext);
    }

    public static final AuthClient getInstance(Context context) {
        if (sInstance == null) sInstance = new AuthClient(context);
        return sInstance;
    }


    public AuthTokenResponse blockingLogin(String accountName, String password) throws VolleyError {
        RequestFuture<AuthTokenResponse> future = RequestFuture.newFuture();
        if (mInFlightAuthTokenRequest != null) {
            mInFlightAuthTokenRequest.cancel();
            mInFlightAuthTokenRequest = null;
        }
        mInFlightAuthTokenRequest = mRequestQueue.add(new AuthTokenRequest(accountName, CMIDUtils.digest("SHA512", password), future, future));
        try {
            AuthTokenResponse response = future.get();
            return response;
        } catch (InterruptedException e) {
            throw new VolleyError(e);
        } catch (ExecutionException e) {
            throw new VolleyError(e);
        } finally {
            mInFlightAuthTokenRequest = null;
        }
    }

    public Request<?> login(String accountName, String password, Listener<AuthTokenResponse> listener, ErrorListener errorListener) {
        return mRequestQueue.add(new AuthTokenRequest(accountName, password, listener, errorListener));
    }

    public AuthTokenResponse blockingRefreshAccessToken(String refreshToken) throws VolleyError {
        RequestFuture<AuthTokenResponse> future = RequestFuture.newFuture();
        if (mInFlightAuthTokenRequest != null) {
            mInFlightAuthTokenRequest.cancel();
            mInFlightAuthTokenRequest = null;
        }
        mInFlightAuthTokenRequest = mRequestQueue.add(new AuthTokenRequest(refreshToken, future, future));
        try {
            AuthTokenResponse response = future.get();
            return response;
        } catch (InterruptedException e) {
            throw new VolleyError(e);
        } catch (ExecutionException e) {
            throw new VolleyError(e);
        } finally {
            mInFlightAuthTokenRequest = null;
        }
    }

    public Request<?> refreshAccessToken(String refreshToken, Listener<AuthTokenResponse> listener, ErrorListener errorListener) {
        return mRequestQueue.add(new AuthTokenRequest(refreshToken, listener, errorListener));
    }

    public Request<?> createProfile(String firstName, String lastName, String email, String password, boolean termsOfService,
            Listener<CreateProfileResponse> listener, ErrorListener errorListener) {
        return mRequestQueue.add(new CreateProfileRequest(firstName, lastName, email, password, termsOfService, listener, errorListener));
    }

    public Request<?> checkProfile(String email, Listener<ProfileAvailableResponse> listener, ErrorListener errorListener) {
        return mRequestQueue.add(new ProfileAvailableRequest(email, listener, errorListener));
    }

    public void pingService(final Listener<PingResponse> listener, final ErrorListener errorListener) {
        final Account account = CMIDUtils.getCMIDAccount(mContext);
        if (account == null) {
            if (CMID.DEBUG) Log.d(TAG, "No CMID Configured!");
            return;
        }
        final TokenCallback callback = new TokenCallback() {
            @Override
            public void onTokenReceived(final String token) {
                if (mInFlightPingRequest != null) {
                    mInFlightPingRequest.cancel();
                    mInFlightPingRequest = null;
                }
                mInFlightPingRequest = mRequestQueue.add(new PingRequest(mContext, CMIDUtils.getUniqueDeviceId(mContext), token, getCarrierName(),
                        new Listener<PingResponse>() {
                            @Override
                            public void onResponse(PingResponse pingResponse) {
                                mInFlightPingRequest = null;
                                if (CMID.DEBUG) Log.d(TAG, "pingService onResponse() : " + pingResponse.getStatusCode());
                                if (listener != null) {
                                    listener.onResponse(pingResponse);
                                }
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                mInFlightPingRequest = null;
                                if (volleyError.networkResponse == null) {
                                    if (CMID.DEBUG) Log.d(TAG, "pingService onErrorResponse() no response");
                                    volleyError.printStackTrace();
                                    errorListener.onErrorResponse(volleyError);
                                    return;
                                }
                                int statusCode = volleyError.networkResponse.statusCode;
                                if (CMID.DEBUG) Log.d(TAG, "pingService onErrorResponse() : " + statusCode);
                                if (statusCode == 401) {
                                    expireToken(mAccountManager, account);
                                    pingService(listener, errorListener);
                                }
                            }
                        }));
            }

            @Override
            public void onError(VolleyError error) {
                if (CMID.DEBUG) {
                    Log.d(TAG, "pingService onError(): ");
                    error.printStackTrace();
                }
                if (errorListener != null) {
                    errorListener.onErrorResponse(error);
                }
            }
        };

        doTokenRequest(account, callback);
    }

    public void sendChannel(final SendChannelRequestBody sendChannelRequestBody, final Listener<Integer> listener, final ErrorListener errorListener) {
        final Account account = CMIDUtils.getCMIDAccount(mContext);
        if (account == null) {
            if (CMID.DEBUG) Log.d(TAG, "No CMID Configured!");
            return;
        }

        // Since we are sending a message, bump the remote sequence.
        if (sendChannelRequestBody.getSessionId() != null) {
            incrementSessionRemoteSequence(sendChannelRequestBody.getSessionId());
        }

        if (CMID.DEBUG) Log.d(TAG, "Sending secure message, plaintext content = " + sendChannelRequestBody.toJsonPlaintext());

        final TokenCallback callback = new TokenCallback() {
            @Override
            public void onTokenReceived(String token) {
                if (mInFlightChannelRequest != null) {
                    mInFlightChannelRequest.cancel();
                    mInFlightChannelRequest = null;
                }

                mInFlightChannelRequest = mRequestQueue.add(new SendChannelRequest(token, sendChannelRequestBody,
                        new Listener<Integer>() {
                            @Override
                            public void onResponse(Integer integer) {
                                mInFlightChannelRequest = null;
                                if (listener != null) {
                                    listener.onResponse(integer);
                                }
                            }
                        },
                        new ErrorListener() {
                            @Override
                            public void onErrorResponse(VolleyError volleyError) {
                                mInFlightChannelRequest = null;
                                if (volleyError.networkResponse == null) {
                                    if (CMID.DEBUG) Log.d(TAG, "sendChannel() onErrorResponse no response");
                                    volleyError.printStackTrace();
                                    errorListener.onErrorResponse(volleyError);
                                    return;
                                }
                                int statusCode = volleyError.networkResponse.statusCode;
                                if (CMID.DEBUG) Log.d(TAG, "sendChannel onErrorResponse() : " + statusCode);
                                if (statusCode == 401) {
                                    expireToken(mAccountManager, account);
                                    sendChannel(sendChannelRequestBody, listener, errorListener);
                                }
                            }
                        }
                ));
            }

            @Override
            public void onError(VolleyError error) {
                if (errorListener != null) {
                    errorListener.onErrorResponse(error);
                }
            }
        };

        doTokenRequest(account, callback);
    }

    public void addLocalAccount(final AccountManager accountManager, final Account account, String password, AuthTokenResponse response) {
        mAccountsUpdateListener = new OnAccountsUpdateListener() {
            @Override
            public void onAccountsUpdated(Account[] accounts) {
                Log.d(TAG, "onAccountsUpdated()");
                for (Account updatedAccount : accounts) {
                    if (updatedAccount.type.equals(CMID.ACCOUNT_TYPE_CMID) && CMIDUtils.getCMIDAccount(mContext) != null) {
                        Log.d(TAG, "onAccountsUpdated() ACCOUNT_TYPE_CMID");
                        if (GCMUtil.googleServicesExist(mContext)) {
                            GCMUtil.registerForGCM(mContext);
                        } else {
                            PingService.pingServer(mContext);
                        }
                        mAccountManager.removeOnAccountsUpdatedListener(mAccountsUpdateListener);
                    }
                }
            }
        };
        mAccountManager.addOnAccountsUpdatedListener(mAccountsUpdateListener, new Handler(), false);
        accountManager.addAccountExplicitly(account, password, null);
        updateLocalAccount(accountManager, account, response);
    }

    public void updateLocalAccount(AccountManager accountManager, Account account, AuthTokenResponse response) {
        accountManager.setUserData(account, CMID.AUTHTOKEN_TYPE_ACCESS, response.getAccessToken());
        accountManager.setAuthToken(account, CMID.AUTHTOKEN_TYPE_ACCESS, response.getAccessToken());
        if (!TextUtils.isEmpty(response.getRefreshToken())) {
            accountManager.setUserData(account, CMID.AUTHTOKEN_TYPE_REFRESH, response.getRefreshToken());
        }
        accountManager.setUserData(account, CMID.AUTHTOKEN_EXPIRES_IN, String.valueOf(System.currentTimeMillis() + (Long.valueOf(response.getExpiresIn()) * 1000)));
        if (CMID.DEBUG) {
            Log.d(TAG, "Access token Expires in = " + (Long.valueOf(response.getExpiresIn()) * 1000) + "ms");
        }
    }

    private void doTokenRequest(final Account account, final TokenCallback tokenCallback) {
        final String currentToken = mAccountManager.peekAuthToken(account, CMID.AUTHTOKEN_TYPE_ACCESS);
        if (CMID.DEBUG) Log.d(TAG, "doTokenRequest() peekAuthToken:  " + currentToken);
        if (isTokenExpired(mAccountManager, account) || currentToken == null) {
            if (CMID.DEBUG) Log.d(TAG, "doTokenRequest() isTokenExpired:  " + "true");
            mAccountManager.invalidateAuthToken(CMID.ACCOUNT_TYPE_CMID, currentToken);
            final String refreshToken = mAccountManager.getUserData(account, CMID.AUTHTOKEN_TYPE_REFRESH);
            if (mInFlightTokenRequest != null) {
                mInFlightTokenRequest.cancel();
                mInFlightTokenRequest = null;
            }
            mInFlightTokenRequest = refreshAccessToken(refreshToken,
                    new Listener<AuthTokenResponse>() {
                        @Override
                        public void onResponse(AuthTokenResponse authTokenResponse) {
                            mInFlightTokenRequest = null;
                            if (CMID.DEBUG) Log.d(TAG, "refreshAccessToken() onResponse token:  " + authTokenResponse.getAccessToken());
                            updateLocalAccount(mAccountManager, account, authTokenResponse);
                            tokenCallback.onTokenReceived(authTokenResponse.getAccessToken());
                        }
                    },
                    new ErrorListener() {
                        @Override
                        public void onErrorResponse(VolleyError volleyError) {
                            mInFlightTokenRequest = null;
                            if (volleyError.networkResponse == null) {
                                if (CMID.DEBUG) Log.d(TAG, "refreshAccessToken() onErrorResponse no response");
                                volleyError.printStackTrace();
                                tokenCallback.onError(volleyError);
                                return;
                            }
                            final int status = volleyError.networkResponse.statusCode;

                            if (CMID.DEBUG) Log.d(TAG, "refreshAccessToken() onErrorResponse:  " + status);
                            if (status == 400 || status == 401) {
                                mAccountManager.setUserData(account, CMID.AUTHTOKEN_TYPE_REFRESH, null);
                                reAuthenticate(account, tokenCallback, volleyError);
                            } else {
                                tokenCallback.onError(volleyError);
                            }
                        }
                    });
        } else {
            if (CMID.DEBUG) Log.d(TAG, "doTokenRequest() returning cached token : " + currentToken);
            tokenCallback.onTokenReceived(currentToken);
        }
    }

    private void reAuthenticate(final Account account, final TokenCallback tokenCallback, final VolleyError originalError) {
        mAccountManager.getAuthToken(account, CMID.ACCOUNT_TYPE_CMID, null, true, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture) {
                try {
                    Bundle bundle =  bundleAccountManagerFuture.getResult();
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    if (!TextUtils.isEmpty(token)) {
                        tokenCallback.onTokenReceived(token);
                    } else {
                        tokenCallback.onError(originalError);
                    }
                } catch (OperationCanceledException e) {
                    Log.e(TAG, "Unable to get AuthToken", e);
                    tokenCallback.onError(new VolleyError(e));
                } catch (IOException e) {
                    Log.e(TAG, "Unable to get AuthToken", e);
                    tokenCallback.onError(new VolleyError(e));
                } catch (AuthenticatorException e) {
                    Log.e(TAG, "Unable to get AuthToken", e);
                    tokenCallback.onError(new VolleyError(e));
                }
            }
        }, new Handler());
    }

    private String getCarrierName() {
        TelephonyManager manager = (TelephonyManager)mContext.getSystemService(Context.TELEPHONY_SERVICE);
        return manager.getNetworkOperatorName();
    }

    private static String getServerURI() {
        String uri = CMID.DEBUG ? SystemProperties.get("cmid.uri") : CMID_URI;
        String cmidUri = (uri == null || uri.length() == 0) ? CMID_URI : uri;
        if (CMID.DEBUG) Log.d(TAG, "Using cmid uri:  " + cmidUri);
        return cmidUri;
    }

    private boolean okToDestroy() {
        String prop = CMID.DEBUG ? SystemProperties.get("cmid.skipwipe") : null;
        boolean skipWipe = (prop == null || prop.length() == 0) ? false : Integer.valueOf(prop) > 0;
        return !skipWipe;
    }

    public void destroyDevice(Context context, String sessionId) {
        final PowerManager pm = (PowerManager)context.getSystemService(Context.POWER_SERVICE);
        final PowerManager.WakeLock wakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        wakeLock.acquire(1000 * 60);
        final DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

        // Send a message back to the browser to indicate that the wipe has started.
        final SendChannelRequestBody sendChannelRequestBody = new SendChannelRequestBody(new WipeStartedMessage(), this, sessionId);
        Thread t = new Thread(new Runnable() {
            @Override
            public void run() {
                sendChannel(sendChannelRequestBody,
                new Listener<Integer>() {
                    @Override
                    public void onResponse(Integer integer) {
                        if (CMID.DEBUG) Log.d(TAG, "wipeStarted onResponse="+integer);
                    }
                },
                new ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError volleyError) {
                        if (CMID.DEBUG) Log.d(TAG, "wipeStarted onErrorResponse:");
                        volleyError.printStackTrace();
                    }
                });
                if (okToDestroy()) {
                    if (CMID.DEBUG) Log.d(TAG, "Wipe enabled, wiping....");
                    dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
                } else {
                    if (CMID.DEBUG) Log.d(TAG, "Skipping wipe");
                }
            }
        });
        t.start();
    }

    public SharedPreferences getAuthPreferences() {
        return mContext.getSharedPreferences(CMID.AUTH_PREFERENCES,
                Context.MODE_PRIVATE);
    }

    public boolean isTokenExpired(AccountManager am, Account account) {
        final String expires_in = am.getUserData(account, CMID.AUTHTOKEN_EXPIRES_IN);
        long expiresTime = expires_in == null ? 0 : Long.valueOf(expires_in);
        return System.currentTimeMillis() > expiresTime;
    }

    public void expireToken(AccountManager am, Account account) {
        final String token = am.getUserData(account, CMID.AUTHTOKEN_TYPE_ACCESS);
        if (!TextUtils.isEmpty(token)) {
            am.invalidateAuthToken(CMID.ACCOUNT_TYPE_CMID, token);
        }
    }

    public void storeSymmetricKey(String symmetricKey, String sessionId) {
        // TODO: keys should expire
        if (CMID.DEBUG) Log.d(TAG, "Storing symmetricKey:" + symmetricKey +" for sessionId:" + sessionId);

        ContentValues values = new ContentValues();
        values.put(CMIDProvider.SymmetricKeyStoreColumns.KEY, symmetricKey);
        values.put(CMIDProvider.SymmetricKeyStoreColumns.SESSION_ID, sessionId);
        mContext.getContentResolver().insert(CMIDProvider.CONTENT_URI, values);
    }

    public void incrementSessionRemoteSequence(String sessionId) {
        if (CMID.DEBUG) Log.d(TAG, "Incrementing remote sequence for sessionId:" + sessionId);
        CMIDProvider.incrementSequence(mContext, CMIDProvider.SymmetricKeyStoreColumns.REMOTE_SEQUENCE, sessionId);
    }

    public void incrementSessionLocalSequence(String sessionId) {
        if (CMID.DEBUG) Log.d(TAG, "Incrementing local sequence for sessionId:" + sessionId);
        CMIDProvider.incrementSequence(mContext, CMIDProvider.SymmetricKeyStoreColumns.LOCAL_SEQUENCE, sessionId);
    }

    public SymmetricKeySequencePair getSymmetricKey(String sessionId) {
        if (CMID.DEBUG) Log.d(TAG, "Loading symmetric key for sessionId:" + sessionId);
        // TODO: keys should expire
        if (sessionId == null) {
            return null;
        }
        Cursor c = null;
        try {
            c = mContext.getContentResolver().query(CMIDProvider.CONTENT_URI, null, CMIDProvider.SymmetricKeyStoreColumns.SESSION_ID + " = ?", new String[]{sessionId}, null);
            if (c != null && c.getCount() > 0) {
                c.moveToFirst();
                String symmetricKey = c.getString(c.getColumnIndex(CMIDProvider.SymmetricKeyStoreColumns.KEY));
                int localSequence = c.getInt(c.getColumnIndex(CMIDProvider.SymmetricKeyStoreColumns.LOCAL_SEQUENCE));
                int remoteSequence = c.getInt(c.getColumnIndex(CMIDProvider.SymmetricKeyStoreColumns.REMOTE_SEQUENCE));
                return new SymmetricKeySequencePair(symmetricKey, localSequence, remoteSequence);
            }
        } finally {
            if (c != null) {
                c.close();
            }
        }

        Log.w(TAG, "Unable to load symmetric key from database for sessionId:" + sessionId);
        return null;
    }

    public String getUniqueDeviceId() {
        return CMIDUtils.getUniqueDeviceId(mContext);
    }

    private static interface TokenCallback {
        void onTokenReceived(String token);
        void onError(VolleyError error);
    }

    public static class SymmetricKeySequencePair {
        private String symmetricKey;
        private int localSequence;
        private int remoteSequence;

        public SymmetricKeySequencePair(String symmetricKey, int localSequence, int remoteSequence) {
            this.symmetricKey = symmetricKey;
            this.localSequence = localSequence;
            this.remoteSequence = remoteSequence;
        }

        public String getSymmetricKey() {
            return symmetricKey;
        }

        public int getLocalSequence() {
            return localSequence;
        }

        public int getRemoteSequence() {
            return remoteSequence;
        }
    }

}
