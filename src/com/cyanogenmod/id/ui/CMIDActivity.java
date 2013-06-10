package com.cyanogenmod.id.ui;


import com.cyanogenmod.id.R;
import com.cyanogenmod.id.auth.AuthActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;

public class CMIDActivity extends Activity {

    private static final int REQUEST_CODE_LOGIN = 0;
    private static final int REQUEST_CODE_CREATE = 1;

    private Button mExistingButton;
    private Button mNewButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cmid_account);
        mExistingButton = (Button)findViewById(R.id.existing_button);
        mExistingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthActivity.showForAuth(CMIDActivity.this, REQUEST_CODE_LOGIN);
            }
        });
        mNewButton = (Button)findViewById(R.id.new_button);
        mNewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthActivity.showForCreate(CMIDActivity.this, REQUEST_CODE_CREATE);
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("CMIDActivity", "onActivityResult()" + requestCode);
        switch (requestCode) {
            case REQUEST_CODE_LOGIN:
            case REQUEST_CODE_CREATE:
                if (resultCode == RESULT_OK) {
                    finish();
                }
            break;
        }
    }
}