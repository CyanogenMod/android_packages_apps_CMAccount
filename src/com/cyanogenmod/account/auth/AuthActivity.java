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

package com.cyanogenmod.account.auth;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.R;
import com.cyanogenmod.account.api.AuthTokenResponse;
import com.cyanogenmod.account.api.CreateProfileResponse;
import com.cyanogenmod.account.api.ErrorResponse;
import com.cyanogenmod.account.api.ProfileAvailableResponse;
import com.cyanogenmod.account.api.response.GetMinimumAppVersionResponse;
import com.cyanogenmod.account.ui.WebViewDialogFragment;
import com.cyanogenmod.account.util.CMAccountUtils;

import android.accounts.Account;
import android.accounts.AccountAuthenticatorActivity;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.Html;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;
import android.text.style.ClickableSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

public class AuthActivity extends AccountAuthenticatorActivity implements Response.ErrorListener {

    private static final String TAG = "AuthActivity";

    public static final String EXTRA_PARAM_CREATE_ACCOUNT = "create-account";

    private static final int DIALOG_LOGIN = 0;
    private static final int DIALOG_CREATE_ACCOUNT = 1;
    private static final int DIALOG_SERVER_ERROR = 2;
    private static final int DIALOG_NO_NETWORK_WARNING = 3;
    private static final int DIALOG_ERROR_CREATING_ACCOUNT = 4;
    private static final int DIALOG_ERROR_LOGIN = 5;
    private static final int DIALOG_CHECKING_FOR_UPDATES = 6;
    private static final int DIALOG_SKIP_WIFI_WARNING = 7;

    private static final int MIN_PASSWORD_LENGTH = 8;

    private AccountManager mAccountManager;
    private AuthClient mAuthClient;

    private TextView mTitle;
    private EditText mEmailEdit;
    private EditText mPasswordEdit;
    private EditText mConfirmPasswordEdit;
    private CheckBox mCheckBox;
    private Button mCancelButton;
    private Button mSubmitButton;


    private boolean mCreateNewAccount = false;

    private boolean mEmailAvailable = true;
    private boolean mEmailInvalid = false;

    private String mEmail;
    private String mPassword;
    private String mPasswordHash;

    private String mPasswordMismatchText;
    private String mPasswordInvalidText;
    private String mEmailInvalidText;
    private String mEmailUnavailableText;

    private Dialog mDialog;

    private Request<?> mInFlightRequest;

    private Response.Listener<AuthTokenResponse> mAuthTokenResponseListener = new Response.Listener<AuthTokenResponse>() {
        @Override
        public void onResponse(AuthTokenResponse authTokenResponse) {
            hideProgress();
            handleLogin(authTokenResponse);
            mInFlightRequest = null;
        }
    };

    private Response.Listener<CreateProfileResponse> mCreateProfileResponseListener = new Response.Listener<CreateProfileResponse>() {
        @Override
        public void onResponse(CreateProfileResponse createProfileResponse) {
            hideProgress();
            handleProfileCreation(createProfileResponse);
            mInFlightRequest = null;
        }
    };

    private Response.Listener<ProfileAvailableResponse> mProfileAvailableResponseListener = new Response.Listener<ProfileAvailableResponse>() {
        @Override
        public void onResponse(ProfileAvailableResponse profileAvailableResponse) {
            mEmailAvailable = profileAvailableResponse.emailAvailable();
            validateFields();
            mInFlightRequest = null;
        }
    };

    private Response.ErrorListener mProfileAvailableErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
            if (volleyError.networkResponse != null) {
                Log.e(TAG, String.valueOf(volleyError.networkResponse.data), volleyError);
            } else {
                Log.e(TAG, "No response from server", volleyError);
            }
        }
    };

    public static void showForCreate(Activity context, int requestCode) {
        /* DEPRECATED */
//        Intent intent = new Intent(context, AuthActivity.class);
//        intent.putExtra(EXTRA_PARAM_CREATE_ACCOUNT, true);
//        context.startActivityForResult(intent, requestCode);
    }

    public static void showForAuth(Activity context, int requestCode, boolean showButtonBar,
            boolean isImmersive) {
        Intent intent = new Intent(context, AuthActivity.class);
        intent.putExtra(CMAccount.EXTRA_USE_IMMERSIVE, isImmersive);
        intent.putExtra(CMAccount.EXTRA_SHOW_BUTTON_BAR, showButtonBar);
        context.startActivityForResult(intent, requestCode);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getIntent().getBooleanExtra(CMAccount.EXTRA_USE_IMMERSIVE, false)) {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_IMMERSIVE
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        }
        setContentView(R.layout.cmaccount_auth);
        getWindow().setStatusBarColor(getResources().getColor(R.color.primary_dark));
        mAccountManager = AccountManager.get(this);
        mAuthClient = AuthClient.getInstance(getApplicationContext());
        CMAccountUtils.hideNotification(this, CMAccount.NOTIFICATION_ID_PASSWORD_RESET);
        mTitle = (TextView) findViewById(android.R.id.title);
        mEmailEdit = (EditText) findViewById(R.id.cmaccount_email);
        mEmailEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                mEmailAvailable = true;
                validateFields();
                if (mCreateNewAccount && validEmail(text.toString())) {
                    mEmailInvalid = false;
                    checkProfile();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mPasswordEdit = (EditText) findViewById(R.id.cmaccount_password);
        mPasswordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                mConfirmPasswordEdit.setError(null);
                validateFields();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mConfirmPasswordEdit = (EditText) findViewById(R.id.cmaccount_confirm_password);
        mConfirmPasswordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                mConfirmPasswordEdit.setError(null);
                validateFields();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mSubmitButton = (Button) findViewById(R.id.submit_button);
        mCheckBox = (CheckBox) findViewById(R.id.cmaccount_tos);
        mCheckBox.setText(buildTermsLabel());
        mCheckBox.setLinksClickable(true);
        mCheckBox.setMovementMethod(LinkMovementMethod.getInstance());
        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                validateFields();
            }
        });
        mCreateNewAccount = getIntent().getBooleanExtra(EXTRA_PARAM_CREATE_ACCOUNT, false);
        mPasswordMismatchText = getString(R.string.cmaccount_setup_password_mismatch_label);
        mPasswordInvalidText = getString(R.string.cmaccount_setup_password_invalid_label);
        mEmailUnavailableText = getString(R.string.cmaccount_setup_email_unavailable_label);
        mEmailInvalidText = getString(R.string.cmaccount_setup_email_invalid_label);
        if (mCreateNewAccount) {
            mEmailEdit.setVisibility(View.VISIBLE);
            mTitle.setText(R.string.cmaccount_setup_create_title);
            mPasswordEdit.setHint(R.string.cmaccount_setup_password_create_label);
            mSubmitButton.setText(R.string.create);
        }  else {
            mConfirmPasswordEdit.setVisibility(View.GONE);
            mTitle.setText(R.string.cmaccount_setup_login_title);
            mSubmitButton.setText(R.string.login);

            // Prefill the email field if an account already exists, useful in password reset process.
            Account account = CMAccountUtils.getCMAccountAccount(this);
            if (account != null) {
                mEmailEdit.setText(account.name);
                //Don't allow editing this field if we have an account.
                mEmailEdit.setEnabled(false);
                mPasswordEdit.requestFocus();
            }
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
                if (mCreateNewAccount) {
                    createProfile();
                } else {
                    login();
                }
            }
        });
        boolean showButtonBar = getIntent().getBooleanExtra(CMAccount.EXTRA_SHOW_BUTTON_BAR, false);
        if (showButtonBar) {
            View buttonBar = findViewById(R.id.button_bar);
            buttonBar.setVisibility(View.VISIBLE);
            findViewById(R.id.prev_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setResult(RESULT_CANCELED);
                    finish();
                }
            });
            findViewById(R.id.next_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    setResult(RESULT_FIRST_USER);
                    finish();
                }
            });
        }
        if (savedInstanceState == null && (!CMAccountUtils.isWifiConnected(this)) || !CMAccountUtils.isNetworkConnected(this)) {
            CMAccountUtils.launchWifiSetup(this);
        }
    }

    @Override
    protected void onPostResume() {
        super.onPostResume();
        validateFields();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mInFlightRequest != null) {
            mInFlightRequest.cancel();
            mInFlightRequest = null;
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == CMAccount.REQUEST_CODE_SETUP_WIFI) {
            if (resultCode == Activity.RESULT_OK) {
                checkMinimumAppVersion();
                setResult(Activity.RESULT_OK);
            } else {
                if (!CMAccountUtils.isNetworkConnected(this)) {
                    showDialog(DIALOG_NO_NETWORK_WARNING);
                } else {
                    showDialog(DIALOG_SKIP_WIFI_WARNING);
                }
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onErrorResponse(VolleyError error) {
        hideProgress();
        if (error.networkResponse != null && error.networkResponse.statusCode < 404) {
            String errorJson = new String(error.networkResponse.data);
            if (CMAccount.DEBUG) Log.d(TAG, errorJson);
            showDialog(mCreateNewAccount ? DIALOG_ERROR_CREATING_ACCOUNT : DIALOG_ERROR_LOGIN);
        } else {
            if (CMAccount.DEBUG) Log.e(TAG, "CMAccount Server Error: ", error.fillInStackTrace());
            showDialog(DIALOG_SERVER_ERROR);
        }
        mInFlightRequest = null;
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        super.onPrepareDialog(id, dialog, args);
        mDialog = dialog;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        switch (id) {
            case DIALOG_SERVER_ERROR:
                mDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.cmaccount_server_error_title)
                        .setMessage(R.string.cmaccount_server_error_message)
                        .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setResult(Activity.RESULT_CANCELED);
                                finish();
                            }
                        })
                        .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create();
                return mDialog;
            case DIALOG_NO_NETWORK_WARNING:
                mDialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.setup_msg_no_network)
                        .setNeutralButton(R.string.skip_anyway, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setResult(Activity.RESULT_CANCELED);
                                finish();
                            }
                        })
                        .setPositiveButton(R.string.dont_skip, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                                CMAccountUtils.launchWifiSetup(AuthActivity.this);
                            }
                        }).create();
                return mDialog;
            case DIALOG_LOGIN:
            case DIALOG_CREATE_ACCOUNT:
                final ProgressDialog dialog = new ProgressDialog(this);
                dialog.setMessage(getText(id == DIALOG_CREATE_ACCOUNT ? R.string.cmaccount_creating_profile_message : R.string.cmaccount_login_message));
                dialog.setIndeterminate(true);
                dialog.setCancelable(true);
                dialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        if (mInFlightRequest != null) {
                            mInFlightRequest.cancel();
                            mInFlightRequest = null;
                            hideProgress();
                        }
                    }
                });
                mDialog = dialog;
                return dialog;
            case DIALOG_ERROR_CREATING_ACCOUNT:
                mDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.cmaccount_create_account_error_title)
                        .setMessage(R.string.cmaccount_server_error_message)
                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create();
                return mDialog;
            case DIALOG_ERROR_LOGIN:
                mDialog = new AlertDialog.Builder(this)
                        .setTitle(R.string.cmaccount_login_error_title)
                        .setMessage(R.string.cmaccount_login_error_message)
                        .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.dismiss();
                            }
                        })
                        .create();
                return mDialog;
            case DIALOG_CHECKING_FOR_UPDATES:
                final ProgressDialog updateDialog = new ProgressDialog(this);
                updateDialog.setMessage(getText(R.string.cmaccount_checking_for_updates));
                updateDialog.setIndeterminate(true);
                updateDialog.setCancelable(true);
                updateDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
                    @Override
                    public void onCancel(DialogInterface dialog) {
                        if (mInFlightRequest != null) {
                            mInFlightRequest.cancel();
                            mInFlightRequest = null;
                        }
                        hideProgress();
                    }
                });
                mDialog = updateDialog;
                return mDialog;
            case DIALOG_SKIP_WIFI_WARNING:
                mDialog = new AlertDialog.Builder(this)
                        .setMessage(R.string.setup_warning_skip_wifi)
                        .setNeutralButton(R.string.skip_anyway, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                setResult(Activity.RESULT_CANCELED);
                            }
                        })
                        .setPositiveButton(R.string.dont_skip, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                dialogInterface.cancel();
                                CMAccountUtils.launchWifiSetup(AuthActivity.this);
                            }
                        }).create();
                return mDialog;
            default:
                return super.onCreateDialog(id, args);
        }
    }

    private void validateFields() {
        boolean valid = false;
        mEmail = mEmailEdit.getText().toString();
        mPassword = mPasswordEdit.getText().toString();
        boolean terms = mCheckBox.isChecked();
        if (mCreateNewAccount) {
            valid = mEmail.length() > 0 &&
                    mPassword.length() > 0 &&
                    mConfirmPasswordEdit.getText().toString().length() > 0 &&
                    mEmailAvailable &&
                    terms;
        } else {
            valid = mEmail.length() > 0 &&
                    mPassword.length() > 0 &&
                    terms;
        }
        if (mEmailInvalid) {
            mEmailEdit.setError(mEmailInvalidText);
        } else if (mEmailAvailable) {
            mEmailEdit.setError(null);
        } else {
            mEmailEdit.setError(mEmailUnavailableText);
        }
        mSubmitButton.setEnabled(valid);
    }

    private boolean confirmPasswords() {
        if (!mPassword.equals(mConfirmPasswordEdit.getText().toString())) {
            mConfirmPasswordEdit.setError(mPasswordMismatchText);
            return false;
        } else {
            mConfirmPasswordEdit.setError(null);
            return true;
        }
    }

    private boolean isPasswordValid() {
        if (mPassword.length() < MIN_PASSWORD_LENGTH) {
            mPasswordEdit.setError(mPasswordInvalidText);
            return false;
        } else {
            mPasswordEdit.setError(null);
            return true;
        }
    }

    private boolean validEmail(String email) {
        return CMAccountUtils.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void trimFields() {
        mPassword =  mPassword != null ? mPassword.trim() : "";
        mPasswordHash = CMAccountUtils.digest("SHA512", mPassword);
        mEmail =  mEmail != null ? mEmail.trim() : "";
    }

    private void hideProgress() {
        if (mDialog != null) {
            mDialog.dismiss();
            mDialog = null;
        }
    }

    private void checkProfile() {
        if (mInFlightRequest != null) {
            mInFlightRequest.cancel();
            mInFlightRequest = null;
        }
        if (validEmail(mEmail)) {
            mAuthClient.checkProfile(mEmail, mProfileAvailableResponseListener, mProfileAvailableErrorListener);
        }
    }

    private void createProfile() {
        if (!isPasswordValid()) {
            mPasswordEdit.requestFocus();
        } else if (!confirmPasswords()) {
            mConfirmPasswordEdit.requestFocus();
        } else if (!validEmail(mEmail)) {
            mEmailInvalid = true;
            validateFields();
            mEmailEdit.requestFocus();
        } else {
            showDialog(DIALOG_CREATE_ACCOUNT);
            trimFields();
            mInFlightRequest = mAuthClient.createProfile(mEmail, CMAccountUtils.digest("SHA512", mPasswordHash), mCheckBox.isChecked(), mCreateProfileResponseListener, this);
        }
    }

    private void handleProfileCreation(CreateProfileResponse response) {
        if (response.hasErrors()) {
            for (ErrorResponse errorResponse : response.getErrors()) {
                if (ErrorResponse.ERROR_CODE_INVALID_EMAIL_FORMAT == errorResponse.getCode()) {
                    mEmailInvalid = true;
                    validateFields();
                    mEmailEdit.requestFocus();
                } else if (ErrorResponse.ERROR_CODE_EMAIL_IN_USE == errorResponse.getCode()) {
                    mEmailAvailable = false;
                    validateFields();
                    mEmailEdit.requestFocus();
                } else {
                   showDialog(DIALOG_ERROR_CREATING_ACCOUNT);
                }
            }
        } else {
            login();
        }
    }

    private void login() {
        showDialog(DIALOG_LOGIN);
        trimFields();
        mInFlightRequest =  mAuthClient.login(mEmail, CMAccountUtils.digest("SHA512", mPasswordHash), mAuthTokenResponseListener, this);
    }

    private void handleLogin(AuthTokenResponse response) {
        final Account account = new Account(mEmail, CMAccount.ACCOUNT_TYPE_CMAccount);
        mAuthClient.addLocalAccount(mAccountManager, account, mPasswordHash, response);
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, mEmail);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, CMAccount.ACCOUNT_TYPE_CMAccount);
        setAccountAuthenticatorResult(result);
        Intent intent = new Intent();
        intent.putExtras(result);
        setResult(RESULT_OK, intent);
        finish();
    }

    private SpannableStringBuilder buildTermsLabel() {
        String s = getString(R.string.cmaccount_setup_tos_label, "<u>", "</u>");
        CharSequence label = Html.fromHtml(s);
        SpannableStringBuilder builder = new SpannableStringBuilder(label);
        UnderlineSpan[] underlines = builder.getSpans(0, label.length(), UnderlineSpan.class);
        for(UnderlineSpan span : underlines) {
            int start = builder.getSpanStart(span);
            int end = builder.getSpanEnd(span);
            int flags = builder.getSpanFlags(span);
            ClickableSpan termsLauncher = new ClickableSpan() {
                public void onClick(View view) {
                    WebViewDialogFragment.newInstance().setUri(AuthClient.TOS_URI).show(getFragmentManager(), WebViewDialogFragment.TAG);
                }
            };
            builder.setSpan(termsLauncher, start, end, flags);
        }
        return builder;
    }

    private void checkMinimumAppVersion() {
        final Context context = this;
        final int appVersion = CMAccountUtils.getApplicationVersion(context);
        final int minimumVersion = CMAccountUtils.getMinimumAppVersion(this);
        if (minimumVersion == -1) {
            showDialog(DIALOG_CHECKING_FOR_UPDATES);
            mInFlightRequest = mAuthClient.getMinimumAppVersion(new Response.Listener<GetMinimumAppVersionResponse>() {
                @Override
                public void onResponse(GetMinimumAppVersionResponse response) {
                    CMAccountUtils.setMinimumAppVersion(context, response.getVersion());
                    if (response.getVersion() > appVersion) {
                        startUpdateRequiredActivity();
                    }
                    hideProgress();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    hideProgress();
                }
            });
        } else if (minimumVersion > appVersion) {
            startUpdateRequiredActivity();
        }
    }

    private void startUpdateRequiredActivity() {
        Intent intent = new Intent(this, UpdateRequiredActivity.class);
        startActivity(intent);
    }

}
