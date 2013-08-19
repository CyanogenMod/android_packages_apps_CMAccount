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

package com.cyanogenmod.account.gcm;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.api.request.SendChannelRequestBody;
import com.cyanogenmod.account.auth.AuthClient;
import com.cyanogenmod.account.gcm.model.*;
import com.cyanogenmod.account.util.CMAccountUtils;
import com.cyanogenmod.account.util.EncryptionUtils;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.util.UUID;

/**
 * Created by ctso on 8/3/13.
 */
public class GCMIntentService extends IntentService implements Response.Listener<Integer>, Response.ErrorListener {

    private static final String TAG = GCMIntentService.class.getSimpleName();
    protected static final String ACTION_RECEIVE = "com.cyanogenmod.account.gcm.RECEIVE";

    private static PowerManager.WakeLock sWakeLock;
    private static final int WAKE_LOCK_TIMEOUT = 1000 * 60 * 5;

    private Context mContext;
    private GoogleCloudMessaging mGoogleCloudMessaging;
    private AccountManager mAccountManager;
    private Account mAccount;
    private AuthClient mAuthClient;
    private Gson mGson;

    public GCMIntentService() {
        super(TAG);

        mGson = new GsonBuilder().registerTypeAdapterFactory(MessageTypeAdapterFactory.getInstance()).create();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        mContext = getApplicationContext();

        // Drop the intent if it isn't a GCM message.
        if (!ACTION_RECEIVE.equals(intent.getAction())) {
            return;
        }

        if (sWakeLock == null) {
            PowerManager pm = (PowerManager)
                    mContext.getSystemService(Context.POWER_SERVICE);
            sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!sWakeLock.isHeld()) {
            if (CMAccount.DEBUG) Log.v(TAG, "Acquiring " + WAKE_LOCK_TIMEOUT + " ms wakelock");
            sWakeLock.acquire(WAKE_LOCK_TIMEOUT);
        }
        mGoogleCloudMessaging = GoogleCloudMessaging.getInstance(mContext);
        mAccountManager = AccountManager.get(mContext);
        mAccount = CMAccountUtils.getCMAccountAccount(mContext);
        if (mAccount == null) {
            if (CMAccount.DEBUG) Log.d(TAG, "No CMAccount Configured!");
            return;
        }
        mAuthClient = AuthClient.getInstance(mContext);

        String messageType = mGoogleCloudMessaging.getMessageType(intent);

        if (CMAccount.DEBUG) Log.d(TAG, "messageType = " + messageType);
        String data = intent.getExtras().getString("data");
        if (CMAccount.DEBUG) Log.d(TAG, "message data = " + data);
        try {
            GCMessage message = mGson.fromJson(data, GCMessage.class);
            handleMessage(mContext, message);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error parsing GCM message", e);
        } catch (RuntimeException e) {
            // Since Message doesn't have a constructor, if a message comes through that we don't have a
            // type adapter for, we will get a RuntimeException.
            Log.e(TAG, "Received unknown message type", e);
        }
    }

    private void handleMessage(final Context context, final GCMessage message) {
        if (CMAccount.DEBUG) Log.d(TAG, "gson parsed message = " + message.toJson());
        String deviceId = CMAccountUtils.getUniqueDeviceId(context);

        // Match account on email, drop if this message is for a different account.
        String messageEmail = message.getAccount().getEmail();
        if (!mAccount.name.equals(messageEmail)) {
            if (CMAccount.DEBUG) Log.d(TAG, "Message was for " + messageEmail + ", current user is " + mAccount.name + ".  Dropping message.");
            return;
        }

        // TODO: We could just look at instanceof message.getMessage()
        if (GCMUtil.COMMAND_KEY_EXCHANGE.equals(message.getCommand())) {
            handleKeyExchange(deviceId, message.getMessage());
        } else if (GCMUtil.COMMAND_SECURE_MESSAGE.equals(message.getCommand())) {
            handleSecureMessage(context, message);
        } else if (GCMUtil.COMMAND_PASSWORD_RESET.equals(message.getCommand())) {
            handlePasswordReset();
        }
    }

    private void handleKeyExchange(String deviceId, final Message message) {
        if (!(message instanceof PublicKeyMessage)) {
            Log.w(TAG, "Expected PublicKeyMessage, but got " + message.getClass().toString());
            return;
        }

        // Cast the message to the correct type
        PublicKeyMessage publicKeyMessage = (PublicKeyMessage) message;

        // Obtain the user's hashed password.
        String passwordHash = mAccountManager.getPassword(mAccount);

        // Verify the public key hash
        // TODO: koush/ctso, we MUST use HMAC here. No nightlies until this is fixed.
        String publicKeyHashVerify = CMAccountUtils.digest("SHA512", publicKeyMessage.getPublicKey() + passwordHash);
        if (!publicKeyHashVerify.equals(publicKeyMessage.getPublicKeyHash())) {
            if (CMAccount.DEBUG) Log.d(TAG, "Unable to verify public key hash");

            // It would be nice if we could just ignore the message at this point, however, when the public key hash
            // is incorrect it means either the public key was tampered with or the user entered the wrong password.
            // Sending a key_exchange_failed message will cause the browser to prompt the user for their password again.

            PlaintextMessage keyExchangeFailedMessage = new PlaintextMessage(GCMUtil.COMMAND_KEY_EXCHANGE_FAILED, 0);
            SendChannelRequestBody sendChannelRequestBody = new SendChannelRequestBody(GCMUtil.COMMAND_KEY_EXCHANGE_FAILED, deviceId, null, keyExchangeFailedMessage);
            mAuthClient.sendChannel(sendChannelRequestBody, GCMIntentService.this, GCMIntentService.this);
            return;
        }

        // Generate the symmetric key, symmetric key verification, and session id.
        String symmetricKey = EncryptionUtils.AES.generateAesKey();
        String symmetricKeyVerify = CMAccountUtils.digest("SHA512", symmetricKey + passwordHash);
        String sessionId = UUID.randomUUID().toString();

        // Persist it
        mAuthClient.storeSymmetricKey(symmetricKey, sessionId);

        // Encrypt the symmetric key
        String encryptedSymmetricKey = EncryptionUtils.RSA.encrypt(publicKeyMessage.getPublicKey(), symmetricKey);

        // Build the symmetric key message
        SymmetricKeyMessage symmetricKeyMessage = new SymmetricKeyMessage(encryptedSymmetricKey, symmetricKeyVerify);

        // Build the channel message, passing in symmetric key message
        SendChannelRequestBody sendChannelRequestBody = new SendChannelRequestBody(GCMUtil.COMMAND_KEY_EXCHANGE, deviceId, sessionId, symmetricKeyMessage);

        // Send the channel message
        mAuthClient.sendChannel(sendChannelRequestBody, GCMIntentService.this, GCMIntentService.this);
    }

    private void handleSecureMessage(final Context context, final GCMessage message) {
        if (!(message.getMessage() instanceof EncryptedMessage)) {
            Log.w(TAG, "Expected EncryptedMessage, but got " + message.getClass().toString());
            return;
        }

        // Cast the message to the correct type
        EncryptedMessage encryptedMessage = (EncryptedMessage) message.getMessage();

        // Pull the AES key from the database
        AuthClient.SymmetricKeySequencePair pair = mAuthClient.getSymmetricKey(message.getSessionId());
        if (pair == null) {
            Log.w(TAG, "Unable to find symmetric key for session=" + message.getSessionId());
            return;
        }

        if (CMAccount.DEBUG) Log.d(TAG, "Attempting to decrypt secure message with key:" + pair.getSymmetricKey() + " for session_id:" + message.getSessionId());

        // Attempt to decrypt the message.
        String plaintext = EncryptionUtils.AES.decrypt(encryptedMessage.getCiphertext(), pair.getSymmetricKey(), encryptedMessage.getIV());
        if (plaintext != null) {
            if (CMAccount.DEBUG) Log.d(TAG, "plaintext message = " + plaintext);
            PlaintextMessage plaintextMessage = PlaintextMessage.fromJson(plaintext);

            // Verify the sequence
            int messageSequence = plaintextMessage.getSequence();
            int localSequence = pair.getLocalSequence();
            Log.d(TAG, "messageSequence=" + messageSequence + ", localSequence="+ localSequence);
            if (localSequence >= messageSequence) {
                Log.w(TAG, "Sequence " + plaintextMessage.getSequence() + " is invalid for session " + message.getSessionId());
                return;
            }

            if (CMAccount.DEBUG) Log.d(TAG, "Sequence " + plaintextMessage.getSequence() + " is valid for session " + message.getSessionId());
            // Increment the local sequence, messages are responsible for loading the sequence from the DB, which is now
            // the correct sequence, before being sent.  See LocationMessage.
            mAuthClient.incrementSessionLocalSequence(message.getSessionId());

            handlePlaintextMessage(context, plaintextMessage, message.getSessionId());
        }
    }

    private void handlePlaintextMessage(final Context context, final PlaintextMessage message, final String sessionId) {
        if (GCMUtil.COMMAND_LOCATE.equals(message.getCommand())) {
            GCMUtil.reportLocation(context, sessionId);
        }

        if (GCMUtil.COMMAND_WIPE.equals(message.getCommand())) {
            mAuthClient.destroyDevice(context, sessionId);
        }
    }

    private void handlePasswordReset() {
        if (CMAccount.DEBUG) Log.d(TAG, "Got password reset message, expiring access and refresh tokens");
        mAuthClient.expireToken(mAccountManager, mAccount);
        mAuthClient.expireRefreshToken(mAccountManager, mAccount);
        mAuthClient.clearPassword(mAccount);
        mAuthClient.notifyPasswordChange(mAccount);
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        if (CMAccount.DEBUG) volleyError.printStackTrace();
    }

    @Override
    public void onResponse(Integer integer) {
        if (CMAccount.DEBUG) Log.d(TAG, "sendChannel response="+integer);
    }
}
