/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cyanogenmod.account.encryption;

import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.api.response.GetPublicKeyIdsResponse;
import com.cyanogenmod.account.auth.AuthClient;
import com.cyanogenmod.account.provider.CMAccountProvider;
import com.cyanogenmod.account.util.CMAccountUtils;

import java.util.ArrayList;
import java.util.List;

public class SyncPublicKeysTask implements Response.ErrorListener, Response.Listener<GetPublicKeyIdsResponse> {
    private static final String TAG = SyncPublicKeysTask.class.getSimpleName();

    private final AuthClient mAuthClient;
    private final Context mContext;

    public static Intent getIntent(Context context) {
        return ECDHKeyService.getIntent(context, ECDHKeyService.ACTION_SYNC);
    }

    public SyncPublicKeysTask(Context context) {
        mContext = context;
        mAuthClient = AuthClient.getInstance(context);
    }

    protected void start() {
        if (CMAccount.DEBUG) Log.d(TAG, "Syncing public keys with server");
        mAuthClient.getPublicKeyIds(this, this);
    }

    private void handleResponse(GetPublicKeyIdsResponse response) {
        List<String> localPublicKeyIds = getLocalPublicKeyIds();
        List<String> remotePublicKeyIds = response.getPublicKeyIds();
        for (String localPublicKeyId : localPublicKeyIds) {
            if (!remotePublicKeyIds.contains(localPublicKeyId)) {
                deleteLocalPublicKey(localPublicKeyId);
            }
        }
    }

    private List<String> getLocalPublicKeyIds() {
        List<String> localPublicKeyIds = new ArrayList<String>();

        String[] projection = new String[] { CMAccountProvider.ECDHKeyStoreColumns.KEY_ID };
        String selection = CMAccountProvider.ECDHKeyStoreColumns.UPLOADED + " = ?";
        Cursor cursor = mContext.getContentResolver().query(CMAccountProvider.ECDH_CONTENT_URI, projection, selection, new String[] { "1" }, null);
        while (cursor.moveToNext()) {
            String localPublicKeyId = cursor.getString(cursor.getColumnIndex(CMAccountProvider.ECDHKeyStoreColumns.KEY_ID));
            localPublicKeyIds.add(localPublicKeyId);
        }
        cursor.close();

        return localPublicKeyIds;
    }

    private void deleteLocalPublicKey(String keyId) {
        if (CMAccount.DEBUG) Log.d(TAG, "Deleting public key " + keyId);
        String selection = CMAccountProvider.ECDHKeyStoreColumns.KEY_ID + " = ?";
        String[] selectionArgs = new String[] { keyId };
        mContext.getContentResolver().delete(CMAccountProvider.ECDH_CONTENT_URI, selection, selectionArgs);
    }

    @Override
    public void onResponse(GetPublicKeyIdsResponse getPublicKeyIdsResponse) {
        if (getPublicKeyIdsResponse.getStatusCode() == 200) {
            handleResponse(getPublicKeyIdsResponse);
            CMAccountUtils.resetBackoff(mAuthClient.getEncryptionPreferences());
            CMAccountUtils.scheduleSyncPublicKeys(mContext, getIntent(mContext));
        } else {
            handleError();
        }
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        if (CMAccount.DEBUG) volleyError.printStackTrace();
        handleError();
    }

    private void handleError() {
        // It is not critical that this task runs, so just reschedule it.
        CMAccountUtils.scheduleSyncPublicKeys(mContext, getIntent(mContext));
    }
}
