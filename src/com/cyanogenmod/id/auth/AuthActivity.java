package com.cyanogenmod.id.auth;

import com.google.gson.Gson;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.api.AuthTokenResponse;
import com.cyanogenmod.id.api.CheckProfileResponse;
import com.cyanogenmod.id.api.CreateProfileResponse;
import com.cyanogenmod.id.api.ProfileAvailableResponse;
import com.cyanogenmod.id.gcm.GCMService;

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
import android.content.SharedPreferences;
import android.graphics.Color;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.TextView;

public class AuthActivity extends AccountAuthenticatorActivity implements Response.ErrorListener {

    private static final String TAG = "AuthActivity";

    private static final String PARAM_CREATE_ACCOUNT = "create-account";

    private static final int DIALOG_LOGIN = 0;
    private static final int DIALOG_CREATE_ACCOUNT = 1;
    private static final int DIALOG_SERVER_ERROR = 2;

    private AccountManager mAccountManager;
    private AuthClient mAuthClient;

    private TextView mTitle;
    private TextView mFirstNameText;
    private EditText mFirstNameEdit;
    private TextView mLastNameText;
    private EditText mLastNameEdit;
    private TextView mEmailText;
    private EditText mEmailEdit;
    private TextView mUsernameText;
    private EditText mUsernameEdit;
    private EditText mPasswordEdit;
    private CheckBox mCheckBox;
    private Button mCancelButton;
    private Button mSubmitButton;


    private boolean mCreateNewAccount = false;

    private boolean mUsernameAvailable = true;
    private boolean mEmailAvailable = true;
    private boolean mEmailInvalid = false;

    private String mFirstName;
    private String mLastName;
    private String mEmail;
    private String mUsername;
    private String mPassword;

    private String mUsernameAvailableText;
    private String mUsernameUnavailableText;
    private String mEmailAvailableText;
    private String mEmailInvalidText;
    private String mEmailUnavailableText;

    private Dialog mDialog;
    private AuthServerError mAuthServerError;

    private SharedPreferences mPreferences;

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
            handleCheckProfileResponse(profileAvailableResponse);
            mInFlightRequest = null;
        }
    };

    private Response.ErrorListener mProfileAvailableErrorListener = new Response.ErrorListener() {
        @Override
        public void onErrorResponse(VolleyError volleyError) {
             Log.e(TAG, String.valueOf(volleyError.networkResponse.data), volleyError);
        }
    };

    public static void showForCreate(Activity context) {
        Intent intent = new Intent(context, AuthActivity.class);
        intent.putExtra(PARAM_CREATE_ACCOUNT, true);
        context.startActivity(intent);
    }

    public static void showForAuth(Activity context) {
        context.startActivity(new Intent(context, AuthActivity.class));
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cmid_auth);
        mAccountManager = AccountManager.get(this);
        mAuthClient = AuthClient.getInstance(getApplicationContext());
        mPreferences = getSharedPreferences(Constants.SETTINGS_PREFERENCES, Context.MODE_PRIVATE);
        mTitle = (TextView) findViewById(android.R.id.title);
        mFirstNameText = (TextView) findViewById(R.id.cmid_firstname_label);
        mFirstNameEdit = (EditText) findViewById(R.id.cmid_firstname);
        mFirstNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                validateFields();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mLastNameText = (TextView) findViewById(R.id.cmid_lastname_label);
        mLastNameEdit = (EditText) findViewById(R.id.cmid_lastname);
        mLastNameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                validateFields();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mEmailText = (TextView) findViewById(R.id.cmid_email_label);
        mEmailEdit = (EditText) findViewById(R.id.cmid_email);
        mEmailEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                mEmailAvailable = true;
                if (validEmail(text.toString())) {
                    mEmailInvalid = false;
                    validateFields();
                    checkProfile();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mUsernameText = (TextView) findViewById(R.id.cmid_username_label);
        mUsernameEdit = (EditText) findViewById(R.id.cmid_username);
        mUsernameEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                validateFields();
                if (mCreateNewAccount) {
                    mUsernameAvailable = true;
                    checkProfile();
                }
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mPasswordEdit = (EditText) findViewById(R.id.cmid_password);
        mPasswordEdit.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence text, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence text, int start, int before, int count) {
                validateFields();
            }

            @Override
            public void afterTextChanged(Editable editable) {}
        });
        mCancelButton = (Button) findViewById(R.id.cancel_button);
        mSubmitButton = (Button) findViewById(R.id.submit_button);
        mCheckBox = (CheckBox) findViewById(R.id.cmid_tos);
        mCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean b) {
                validateFields();
            }
        });
        mCreateNewAccount = getIntent().getBooleanExtra(PARAM_CREATE_ACCOUNT, false);
        mUsernameAvailableText = getString(R.string.cmid_setup_username_label);
        mUsernameUnavailableText  = getString(R.string.cmid_setup_username_unavailable_label);
        mEmailAvailableText = getString(R.string.cmid_setup_email_label);
        mEmailUnavailableText = getString(R.string.cmid_setup_email_unavailable_label);
        mEmailInvalidText = getString(R.string.cmid_setup_email_invalid_label);
        if (mCreateNewAccount) {
            mFirstNameText.setVisibility(View.VISIBLE);
            mFirstNameEdit.setVisibility(View.VISIBLE);
            mLastNameText.setVisibility(View.VISIBLE);
            mLastNameEdit.setVisibility(View.VISIBLE);
            mEmailText.setVisibility(View.VISIBLE);
            mEmailEdit.setVisibility(View.VISIBLE);
            mCheckBox.setVisibility(View.VISIBLE);
            mTitle.setText(R.string.cmid_setup_create_title);
            mSubmitButton.setText(R.string.create);
        }  else {
            mFirstNameText.setVisibility(View.GONE);
            mFirstNameEdit.setVisibility(View.GONE);
            mLastNameText.setVisibility(View.GONE);
            mLastNameEdit.setVisibility(View.GONE);
            mEmailText.setVisibility(View.GONE);
            mEmailEdit.setVisibility(View.GONE);
            mCheckBox.setVisibility(View.GONE);
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
                if (mCreateNewAccount) {
                    createProfile();
                } else {
                    login();
                }
            }
        });
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
    public void onErrorResponse(VolleyError error) {
        hideProgress();
        if (error.networkResponse.statusCode != 500) {
            String errorJson = new String(error.networkResponse.data);
            if (Constants.DEBUG) Log.d(TAG, errorJson);
            final Gson gson = new Gson();
            mAuthServerError = gson.fromJson(errorJson, AuthServerError.class);
        } else {
            if (Constants.DEBUG) Log.d(TAG, "Response = " + new String(error.networkResponse.data));
            final String errorMessage = error.getMessage();
            mAuthServerError = new AuthServerError(getString(R.string.cmid_server_error_title), errorMessage == null ? getString(R.string.cmid_server_error_message) : error.getMessage());
        }
        mInFlightRequest = null;
        showDialog(DIALOG_SERVER_ERROR);
    }

    @Override
    protected void onPrepareDialog(int id, Dialog dialog, Bundle args) {
        super.onPrepareDialog(id, dialog, args);
        mDialog = dialog;
    }

    @Override
    protected Dialog onCreateDialog(int id, Bundle args) {
        if (id == DIALOG_SERVER_ERROR) {
            mDialog = new AlertDialog.Builder(this)
                    .setTitle(mAuthServerError.getError())
                    .setMessage(mAuthServerError.getErrorDescription()).show();
            return mDialog;
        } else {
            final ProgressDialog dialog = new ProgressDialog(this);
            dialog.setMessage(getText(id == DIALOG_CREATE_ACCOUNT ? R.string.cmid_creating_profile_message : R.string.cmid_login_message));
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
    }
    }

    private void validateFields() {
        boolean valid = false;
        mFirstName = mFirstNameEdit.getText().toString();
        mLastName = mLastNameEdit.getText().toString();
        mEmail = mEmailEdit.getText().toString();
        mUsername = mUsernameEdit.getText().toString();
        mPassword = mPasswordEdit.getText().toString();
        boolean terms = mCheckBox.isChecked();
        if (mCreateNewAccount) {
            valid = mFirstName.length() > 0 &&
                    mLastName.length() > 0 &&
                    mEmail.length() > 0 &&
                    mUsername.length() > 0 &&
                    mPassword.length() > 0 &&
                    mUsernameAvailable &&
                    mEmailAvailable &&
                    terms;
        } else {
            valid = mUsername.length() > 0 &&
                    mPassword.length() > 0;
        }
        if (mUsernameAvailable) {
            mUsernameText.setText(mUsernameAvailableText);
            mUsernameText.setTextColor(Color.WHITE);
        } else {
            mUsernameText.setText(mUsernameUnavailableText);
            mUsernameText.setTextColor(Color.RED);
        }
        if (mEmailInvalid) {
            mEmailText.setText(mEmailInvalidText);
            mEmailText.setTextColor(Color.RED);
        } else if (mEmailAvailable) {
            mEmailText.setText(mEmailAvailableText);
            mEmailText.setTextColor(Color.WHITE);
        } else {
            mEmailText.setText(mEmailUnavailableText);
            mEmailText.setTextColor(Color.RED);
        }
        mSubmitButton.setEnabled(valid);
    }

    private boolean validEmail(String email) {
        return Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

    private void trimFields() {
        mFirstName =  mFirstName != null ? mFirstName.trim() : "";
        mLastName =  mLastName != null ? mLastName.trim() : "";
        mUsername =  mUsername != null ? mUsername.trim() : "";
        mPassword =  mPassword != null ? mPassword.trim() : "";
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
        mAuthClient.checkProfile(validEmail(mEmail) ? mEmail : null, mUsername, mProfileAvailableResponseListener, mProfileAvailableErrorListener);
    }

    private void createProfile() {
        if (!validEmail(mEmail)) {
            mEmailInvalid = true;
            validateFields();
            mEmailEdit.requestFocus();
        } else {
            showDialog(DIALOG_CREATE_ACCOUNT);
            trimFields();
            mInFlightRequest = mAuthClient.createProfile(mFirstName, mLastName, mEmail, mUsername, mPassword, mCheckBox.isChecked(), mCreateProfileResponseListener, this);
        }
    }

    private void handleCheckProfileResponse(CheckProfileResponse checkProfileResponse) {
        mEmailAvailable = checkProfileResponse.emailAvailable();
        mUsernameAvailable = checkProfileResponse.usernameAvailable();
        validateFields();
    }

    private void handleProfileCreation(CreateProfileResponse response) {
        if (response.hasErrors()) {
            handleCheckProfileResponse(response.getErrors());
        } else {
            login();
        }
    }

    private void login() {
        showDialog(DIALOG_LOGIN);
        trimFields();
        mInFlightRequest =  mAuthClient.login(mUsername, mPassword, mAuthTokenResponseListener, this);
    }

    private void handleLogin(AuthTokenResponse response) {
        final Account account = new Account(mUsername, Constants.ACCOUNT_TYPE);
        mAuthClient.addLocalAccount(mAccountManager, account, response);
        if (mPreferences.getBoolean(Constants.KEY_FIND_DEVICE_PREF, false)) {
            GCMService.registerClient(getApplicationContext(), account);
        }
        Bundle result = new Bundle();
        result.putString(AccountManager.KEY_ACCOUNT_NAME, mUsername);
        result.putString(AccountManager.KEY_ACCOUNT_TYPE, Constants.ACCOUNT_TYPE);
        setAccountAuthenticatorResult(result);
        Intent intent = new Intent();
        intent.putExtras(result);
        setResult(RESULT_OK, intent);
        finish();
    }

}
