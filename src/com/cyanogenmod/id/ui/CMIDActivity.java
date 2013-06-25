package com.cyanogenmod.id.ui;


import com.cyanogenmod.id.R;
import com.cyanogenmod.id.auth.AuthActivity;

import android.app.Activity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class CMIDActivity extends Activity {

    private Button mExistingButton;
    private Button mNewButton;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cmid_account);
        mExistingButton = (Button)findViewById(R.id.existing_button);
        mExistingButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthActivity.showForAuth(CMIDActivity.this);
                finish();
            }
        });
        mNewButton = (Button)findViewById(R.id.new_button);
        mNewButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthActivity.showForCreate(CMIDActivity.this);
                finish();
            }
        });
    }
}