package com.cyanogenmod.id.auth;

import android.accounts.AccountManager;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import com.cyanogenmod.id.R;

public class AuthActivity extends Activity {

    private static final String TAG = "AuthActivity";

    private static final String PARAM_CREATE_ACCOUNT = "create-account";

    private static final int DIALOG_REFRESH_TOKEN = 0;
    private static final int DIALOG_CREATE_ACCOUNT = 1;

    private AccountManager mAccountManager;

    private TextView mTitle;
    private TextView mEmailText;
    private EditText mEmailEdit;
    private EditText mUsernameEdit;
    private EditText mPasswordEdit;
    private Button mCancelButton;
    private Button mSubmitButton;


    private boolean mCreateNewAccount = false;

    private String mUsername;
    private String mPassword;

    private RefreshTokenTask mRefreshTokenTask;
    private ProgressDialog mProgressDialog;

    public static void showForCreate(Context context) {
        Intent intent = new Intent(context, AuthActivity.class);
        intent.putExtra(PARAM_CREATE_ACCOUNT, true);
        context.startActivity(intent);
    }

    public static void showForAuth(Context context) {
        context.startActivity(new Intent(context, AuthActivity.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cmid_auth);
        mAccountManager = AccountManager.get(this);
        mTitle = (TextView) findViewById(android.R.id.title);
        mEmailText = (TextView) findViewById(R.id.cmid_email_label);
        mEmailEdit = (EditText) findViewById(R.id.cmid_email);
        mUsernameEdit = (EditText) findViewById(R.id.cmid_username);
        mPasswordEdit = (EditText) findViewById(R.id.cmid_password);
        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mSubmitButton = (Button) findViewById(R.id.submit_button);
        mCreateNewAccount = getIntent().getBooleanExtra(PARAM_CREATE_ACCOUNT, false);
        if (mCreateNewAccount) {
            mEmailText.setVisibility(View.VISIBLE);
            mEmailEdit.setVisibility(View.VISIBLE);
            mTitle.setText(R.string.cmid_setup_create_title);
            mSubmitButton.setText(R.string.create);
        }  else {
            mEmailText.setVisibility(View.GONE);
            mEmailEdit.setVisibility(View.GONE);
            mTitle.setText(R.string.cmid_setup_login_title);
            mSubmitButton.setText(R.string.login);
        }
        mCancelButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                setResult(RESULT_CANCELED);
                finish();
            }
        });
        mSubmitButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doRefreshToken();
            }
        });
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        final ProgressDialog dialog = new ProgressDialog(this);
        dialog.setMessage(getText(R.string.cmid_login_message));
        dialog.setIndeterminate(true);
        dialog.setCancelable(true);
        dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            public void onCancel(DialogInterface dialog) {
                if (mRefreshTokenTask != null) {
                    mRefreshTokenTask.cancel(true);
                }
            }
        });
        mProgressDialog = dialog;
        return dialog;
    }

    private void showProgress(int type) {
        showDialog(type);
    }

    private void hideProgress() {
        if (mProgressDialog != null) {
            mProgressDialog.dismiss();
            mProgressDialog = null;
        }
    }

    private void doRefreshToken() {
        mUsername = mUsernameEdit.getText().toString();
        mPassword = mPasswordEdit.getText().toString();
        showProgress(DIALOG_REFRESH_TOKEN);
        mRefreshTokenTask = new RefreshTokenTask();
        mRefreshTokenTask.execute();
    }


    public class RefreshTokenTask extends AsyncTask<Void, Void, AuthClient.AuthResponse> {

        @Override
        protected AuthClient.AuthResponse doInBackground(Void... params) {
               return AuthClient.refreshAuthTokens(mUsername, mPassword);
        }

        @Override
        protected void onPostExecute(final AuthClient.AuthResponse authResponse) {
            mRefreshTokenTask = null;
            hideProgress();
        }

        @Override
        protected void onCancelled() {
            mRefreshTokenTask = null;
            hideProgress();
        }
    }
}
