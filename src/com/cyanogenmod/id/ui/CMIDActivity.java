package com.cyanogenmod.id.ui;


import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.auth.AuthActivity;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

public class CMIDActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cmid_setup_standalone);
        findViewById(R.id.existing_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthActivity.showForAuth(CMIDActivity.this, Constants.REQUEST_CODE_SETUP_CMID);

            }
        });
        findViewById(R.id.new_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthActivity.showForCreate(CMIDActivity.this, Constants.REQUEST_CODE_SETUP_CMID);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == Constants.REQUEST_CODE_SETUP_CMID && resultCode == RESULT_OK) {
            finish();
        }
    }
}