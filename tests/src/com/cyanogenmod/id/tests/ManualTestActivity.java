package com.cyanogenmod.id.tests;

import com.cyanogenmod.id.Constants;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.accounts.AuthenticatorException;
import android.accounts.OperationCanceledException;
import android.app.Activity;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.Toast;

import java.io.IOException;

public class ManualTestActivity extends Activity {

    private AccountManager mAccountManager;
    private Account mSelectedAccount;
    private Spinner mSpinner;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cmid_test);
        mSpinner = (Spinner) findViewById(R.id.accounts);
        mAccountManager = AccountManager.get(this);
        final Account[] accounts = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
        ArrayAdapter<Account> adapter = new ArrayAdapter<Account>(this, android.R.layout.simple_list_item_1, android.R.id.text1, accounts);
        mSpinner.setAdapter(adapter);
        mSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                mSelectedAccount = accounts[position];
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        findViewById(R.id.get_token).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                getToken();
            }
        });
        findViewById(R.id.invalidate_token).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                invalidateAuthToken();
            }
        });
        findViewById(R.id.expire_token).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                expireAuthToken();
            }
        });
    }

    private void getToken() {
        mAccountManager.getAuthToken(mSelectedAccount, Constants.AUTHTOKEN_TYPE_ACCESS, null, this, new AccountManagerCallback<Bundle>() {
            @Override
            public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture) {
                try {
                    Bundle bundle =  bundleAccountManagerFuture.getResult();
                    String token = bundle.getString(AccountManager.KEY_AUTHTOKEN);
                    Log.d("AccountManagerTest", "token=" + token);
                    Toast.makeText(ManualTestActivity.this, "AuthToken\n" + token, Toast.LENGTH_SHORT).show();
                } catch (OperationCanceledException e) {
                    e.printStackTrace();
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (AuthenticatorException e) {
                    e.printStackTrace();
                }
            }
        }, new Handler());
    }

    private void invalidateAuthToken() {
        String token = mAccountManager.peekAuthToken(mSelectedAccount, Constants.AUTHTOKEN_TYPE_ACCESS);
        if (token != null) {
            mAccountManager.invalidateAuthToken(Constants.AUTHTOKEN_TYPE_ACCESS, token);
            Toast.makeText(ManualTestActivity.this, "AuthToken\n" + token + "\nInvalidated", Toast.LENGTH_SHORT).show();
        } else {
            Toast.makeText(ManualTestActivity.this, "No AuthToken", Toast.LENGTH_SHORT).show();
        }
    }

    private void expireAuthToken() {
        mAccountManager.setUserData(mSelectedAccount, Constants.AUTHTOKEN_EXPIRES_IN, "0");
        String token = mAccountManager.peekAuthToken(mSelectedAccount, Constants.AUTHTOKEN_TYPE_ACCESS);
        if (token != null) {
            mAccountManager.invalidateAuthToken(Constants.AUTHTOKEN_TYPE_ACCESS, token);
        }
        Toast.makeText(ManualTestActivity.this, "AuthToken expired", Toast.LENGTH_SHORT).show();
    }


}