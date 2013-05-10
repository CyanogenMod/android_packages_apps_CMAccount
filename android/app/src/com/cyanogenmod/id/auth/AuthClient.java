package com.cyanogenmod.id.auth;

import android.net.Uri;
import android.util.Base64;
import android.util.Log;
import com.cyanogenmod.id.Constants;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.HttpResponseException;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.conn.params.ConnManagerParams;
import org.apache.http.impl.client.AbstractHttpClient;
import org.apache.http.impl.client.BasicResponseHandler;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.message.BasicNameValuePair;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.text.ParseException;
import java.util.ArrayList;

public class AuthClient {

    private AuthClient() {}

    private static final String TAG = "AuthClient";

    public static final String PARAM_USERNAME = "username";
    public static final String PARAM_PASSWORD = "password";
    public static final String PARAM_GRANT_TYPE = "grant_type";
    private static final Uri AUTH_URI = Uri.parse("https://cmid-devel.appspot.com/oauth2/token");
    private static final int HTTP_REQUEST_TIMEOUT_MS = 30 * 1000;

    private static final String CLIENT_ID = "8b657e90e8be40a7a780c7fcd78722d4";
    private static final String SECRET = "922b4a50b13845a58374a843c1a9630b";

    private static AbstractHttpClient getHttpClient() {
        AbstractHttpClient httpClient = new DefaultHttpClient();
        final HttpParams params = httpClient.getParams();
        HttpConnectionParams.setConnectionTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        HttpConnectionParams.setSoTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        ConnManagerParams.setTimeout(params, HTTP_REQUEST_TIMEOUT_MS);
        return httpClient;
    }

    public static AuthResponse refreshAuthTokens(String username, String password) {
        final ArrayList<NameValuePair> params = new ArrayList<NameValuePair>();
        params.add(new BasicNameValuePair(PARAM_GRANT_TYPE, PARAM_PASSWORD));
        params.add(new BasicNameValuePair(PARAM_USERNAME, username));
        params.add(new BasicNameValuePair(PARAM_PASSWORD, password));
        final HttpEntity entity;
        try {
            entity = new UrlEncodedFormEntity(params);
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
        String encoding = new String(Base64.encode((CLIENT_ID + ":" + SECRET).getBytes(), Base64.DEFAULT));
        final HttpPost post = new HttpPost(AUTH_URI.toString());
        post.setHeader("Authorization", "Basic " + encoding);
        post.addHeader(entity.getContentType());
        post.setEntity(entity);
        try {
            if (Constants.DEBUG) {
                Log.d(TAG,post.getRequestLine().toString());
                Header[] headers = post.getAllHeaders();
                for (Header header : headers) {
                    Log.d(TAG,header.getName()+":"+header.getValue());
                }


                InputStream istream = (entity != null) ? entity.getContent()
                        : null;
                if (istream != null) {
                    BufferedReader ireader = new BufferedReader(new InputStreamReader(istream));
                    Log.d(TAG,ireader.readLine().trim());
                }
            }
            AbstractHttpClient client = getHttpClient();
            AuthResponse response = new AuthResponse(excuteForJSONObject(client, post));
            return response;
        } catch (final IOException e) {
            Log.e(TAG, "IOException when refreshing authtoken", e);
            return null;
        } catch (final JSONException e) {
            Log.e(TAG, "JSONException when refreshing authtoken", e);
            return null;
        } catch (final ParseException e) {
            Log.e(TAG, "ParseException when refreshing authtoken", e);
            return null;
        } catch (final AuthResponseException e) {
            Log.e(TAG, "AuthResponseException when refreshing authtoken", e);
            return null;
        }
    }


    private static JSONObject excuteForJSONObject(AbstractHttpClient client, HttpUriRequest request) throws AuthResponseException {
        try {
            String response = client.execute(request, new BasicResponseHandler(){
                @Override
                public String handleResponse(HttpResponse response) throws HttpResponseException, IOException {
                    if (Constants.DEBUG) Log.d(TAG, "response="+response.getStatusLine().getStatusCode());
                    return super.handleResponse(response);
                }
            });
            if (Constants.DEBUG) Log.d(TAG, response);
            return response != null ? new JSONObject(response) : null;
        } catch (JSONException e) {
                throw new AuthResponseException(e);
        } catch (ClientProtocolException e) {
            throw new AuthResponseException(e);
        } catch (IOException e) {
            throw new AuthResponseException(e);
        }

    }

    public static class AuthResponse {

        private static final String FIELD_ACCESS_TOKEN = "access_token";
        private static final String FIELD_REFESH_TOKEN = "refresh_token";
        private static final String FIELD_EXPIRES_IN = "expires_in";

        private boolean hasAccessToken;
        private String accessToken;
        private boolean hasRefreshToken;
        private String refreshToken;
        private boolean hasExpiresIn;
        private String expiresIn;

        private AuthResponse(JSONObject object) throws JSONException, ParseException {
             if ((hasAccessToken = object.has(FIELD_ACCESS_TOKEN))) {
                 accessToken = object.getString(FIELD_ACCESS_TOKEN);
             }
            if ((hasRefreshToken = object.has(FIELD_REFESH_TOKEN))) {
                refreshToken = object.getString(FIELD_REFESH_TOKEN);
            }
            if ((hasExpiresIn = object.has(FIELD_EXPIRES_IN))) {
                expiresIn = object.getString(FIELD_EXPIRES_IN);
            }
        }

        private static void throwUnless(String field, boolean hasField) {
            if (!hasField) {
                throw new IllegalArgumentException("'" + field + "' does not exist in auth response");
            }
        }

        public boolean iHasRefreshToken() {
            return hasRefreshToken;
        }

        public boolean iHasExpiresIn() {
            return hasExpiresIn;
        }

        public boolean iHasAccessToken() {
            return hasAccessToken;
        }

        public String getAccessToken() {
            throwUnless(FIELD_ACCESS_TOKEN, hasAccessToken);
            return accessToken;
        }

        public String getExpiresIn() {
            throwUnless(FIELD_EXPIRES_IN, hasExpiresIn);
            return expiresIn;
        }

        public String getRefreshToken() {
            throwUnless(FIELD_REFESH_TOKEN, hasRefreshToken);
            return refreshToken;
        }
    }

    public static class AuthResponseException extends Exception {
        public AuthResponseException(Throwable throwable) {
            super(throwable);
        }

        public AuthResponseException(String message) {
            super(message);
        }
    }
}
