package com.cyanogenmod.id.gcm;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.gson.Gson;
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
            GCMessage message = new Gson().fromJson(data, GCMessage.class);
            if (CMID.DEBUG) Log.d(TAG, "GCMessage: \n " + message.dump());
            handleMessage(context, message);
        } catch (JsonSyntaxException e) {
            Log.e(TAG, "Error parsing GCM message", e);
        }
    }

    private void handleMessage(final Context context, final GCMessage message) {
        if (GCMUtil.COMMAND_START_HANDSHAKE.equals(message.getCommand())) {
            setHandshakeSecret(message.getArgs().getCommand());
        }
        if (GCMUtil.COMMAND_LOCATE.equals(message.getCommand())) {
            AuthClient.HandshakeTokenItem item = mAuthClient.getHandshakeToken(message.getToken(), message.getCommand());
            if (item != null) {
                if (CMID.DEBUG) Log.d(TAG, message.getCommand() + " handshake token is good!");
                GCMUtil.reportLocation(context);
                mAuthClient.cleanupHandshakeTokenByType(message.getCommand());
            } else {
                if (CMID.DEBUG) Log.d(TAG, message.getCommand() + " handshake token is bad!");
                setHandshakeSecret(message.getCommand());
            }
        } else  if (GCMUtil.COMMAND_WIPE.equals(message.getCommand())) {
            AuthClient.HandshakeTokenItem item = mAuthClient.getHandshakeToken(message.getToken(), message.getCommand());
            if (item != null) {
                if (CMID.DEBUG) Log.d(TAG, message.getCommand() + " handshake token is good!");
                mAuthClient.destroyDevice(context);
                mAuthClient.cleanupHandshakeTokenByType(message.getCommand());
            } else {
                if (CMID.DEBUG) Log.d(TAG, message.getCommand() + " handshake token is bad!");
                setHandshakeSecret(message.getCommand());
            }
        }
    }

    @Override
    public void onErrorResponse(VolleyError volleyError) {
        if (CMID.DEBUG) volleyError.printStackTrace();
    }

    @Override
    public void onResponse(Integer integer) {
        if (CMID.DEBUG) Log.d(TAG, "sendHandshakeSecret response="+integer);
    }

    private void setHandshakeSecret(final String command) {
        String uuid = UUID.randomUUID().toString();
        mAuthClient.generateHandshakeToken(mAccountManager, mAccount, uuid, command);
        mAuthClient.sendHandshakeSecret(command, uuid, GCMReceiver.this, GCMReceiver.this);
    }
}
