package com.cyanogenmod.id.gcm;

import com.cyanogenmod.id.api.request.SendChannelRequestBody;
import com.cyanogenmod.id.util.EncryptionUtils;
import com.cyanogenmod.id.gcm.model.*;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.JsonSyntaxException;

import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.auth.AuthClient;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.PowerManager;
import android.util.Log;

import java.util.UUID;

public class GCMReceiver extends BroadcastReceiver implements Response.Listener<Integer>, Response.ErrorListener {

    private static final String TAG = GCMReceiver.class.getSimpleName();

    private static PowerManager.WakeLock sWakeLock;
    private static final int WAKE_LOCK_TIMEOUT = 1000 * 60 * 5;

    private GoogleCloudMessaging mGoogleCloudMessaging;
    private AccountManager mAccountManager;
    private Account mAccount;
    private AuthClient mAuthClient;

    public void onReceive(Context context, Intent intent) {
        if (sWakeLock == null) {
            PowerManager pm = (PowerManager)
                    context.getSystemService(Context.POWER_SERVICE);
            sWakeLock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, TAG);
        }
        if (!sWakeLock.isHeld()) {
            if (CMID.DEBUG) Log.v(TAG, "Acquiring " + WAKE_LOCK_TIMEOUT + " ms wakelock");
            sWakeLock.acquire(WAKE_LOCK_TIMEOUT);
        }
        mGoogleCloudMessaging = GoogleCloudMessaging.getInstance(context);
        mAccountManager = AccountManager.get(context);
        mAccount = CMIDUtils.getCMIDAccount(context);
        if (mAccount == null) {
            if (CMID.DEBUG) Log.d(TAG, "No CMID Configured!");
            return;
        }
        mAuthClient = AuthClient.getInstance(context);

        String messageType = mGoogleCloudMessaging.getMessageType(intent);

        if (CMID.DEBUG) Log.d(TAG, "messageType = " + messageType);
        String data = intent.getExtras().getString("data");
        if (CMID.DEBUG) Log.d(TAG, "message data = " + data);
        try {
            GCMessage message = GCMessage.fromJson(data);
            handleMessage(context, message);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error parsing GCM message", e);
        } catch (RuntimeException e) {
            // Since Message doesn't have a constructor, if a message comes through that we don't have a
            // type adapter for, we will get a RuntimeException.
            Log.e(TAG, "Received unknown message type", e);
        }
    }

    private void handleMessage(final Context context, final GCMessage message) {
        if (CMID.DEBUG) Log.d(TAG, "gson parsed message = " + message.toJson());
        String deviceId = CMIDUtils.getUniqueDeviceId(context);

        // TODO: We could just look at instanceof message.getMessage()
        if (GCMUtil.COMMAND_KEY_EXCHANGE.equals(message.getCommand())) {
            handleKeyExchange(deviceId, message.getMessage());
        } else if (GCMUtil.COMMAND_SECURE_MESSAGE.equals(message.getCommand())) {
            handleSecureMessage(context, message);
        }
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        if (CMID.DEBUG) volleyError.printStackTrace();
    }

    @Override
    public void onResponse(Integer integer) {
        if (CMID.DEBUG) Log.d(TAG, "sendChannel response="+integer);
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
        String publicKeyHashVerify = CMIDUtils.digest("SHA512", publicKeyMessage.getPublicKey() + passwordHash);
        if (!publicKeyHashVerify.equals(publicKeyMessage.getPublicKeyHash())) {
            if (CMID.DEBUG) Log.d(TAG, "Unable to verify public key hash");

            // TODO: Listen for some type of invalid message, for now, just send some bogus hash.
            SymmetricKeyMessage symmetricKeyMessage = new SymmetricKeyMessage("QkFDT04=", "1683867bccd35381e3c8b99c6a59095b");
            SendChannelRequestBody sendChannelRequestBody = new SendChannelRequestBody(GCMUtil.COMMAND_KEY_EXCHANGE, deviceId, "sessionId", symmetricKeyMessage);
            mAuthClient.sendChannel(sendChannelRequestBody, GCMReceiver.this, GCMReceiver.this);
            return;
        }

        // Generate the symmetric key, symmetric key verification, and session id.
        String symmetricKey = EncryptionUtils.AES.generateAesKey();
        String symmetricKeyVerify = CMIDUtils.digest("SHA512", symmetricKey + passwordHash);
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
        mAuthClient.sendChannel(sendChannelRequestBody, GCMReceiver.this, GCMReceiver.this);
    }

    private void handleSecureMessage(final Context context, final GCMessage message) {
        if (!(message.getMessage() instanceof EncryptedMessage)) {
            Log.w(TAG, "Expected EncryptedMessage, but got " + message.getClass().toString());
            return;
        }

        // Cast the message to the correct type
        EncryptedMessage encryptedMessage = (EncryptedMessage) message.getMessage();

        // Pull the AES key from the database
        // TODO: Also grab pair.
        AuthClient.SymmetricKeySequencePair pair = mAuthClient.getSymmetricKey(message.getSessionId());
        if (pair == null) {
            Log.w(TAG, "Unable to find symmetric key for session=" + message.getSessionId());
            return;
        }

        if (CMID.DEBUG) Log.d(TAG, "Attempting to decrypt secure message with key:" + pair.getSymmetricKey() + " for session_id:" + message.getSessionId());

        // Attempt to decrypt the message.
        String plaintext = EncryptionUtils.AES.decrypt(encryptedMessage.getCiphertext(), pair.getSymmetricKey(), encryptedMessage.getIV());
        if (plaintext != null) {
            if (CMID.DEBUG) Log.d(TAG, "plaintext message = " + plaintext);
            PlaintextMessage plaintextMessage = PlaintextMessage.fromJson(plaintext);

            // Verify the sequence
            int messageSequence = plaintextMessage.getSequence();
            int localSequence = pair.getLocalSequence();
            Log.d(TAG, "messageSequence=" + messageSequence + ", localSequence="+ localSequence);
            if (localSequence >= messageSequence) {
                Log.w(TAG, "Sequence " + plaintextMessage.getSequence() + " is invalid for session " + message.getSessionId());
                return;
            }

            if (CMID.DEBUG) Log.d(TAG, "Sequence " + plaintextMessage.getSequence() + " is valid for session " + message.getSessionId());
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
}
