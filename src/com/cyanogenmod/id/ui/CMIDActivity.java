package com.cyanogenmod.id.ui;


import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.auth.AuthActivity;
import com.cyanogenmod.id.auth.AuthClient;
import com.cyanogenmod.id.util.CMIDUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;

public class CMIDActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cmid_setup_standalone);
        findViewById(R.id.existing_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthActivity.showForAuth(CMIDActivity.this, CMID.REQUEST_CODE_SETUP_CMID);

            }
        });
        findViewById(R.id.new_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthActivity.showForCreate(CMIDActivity.this, CMID.REQUEST_CODE_SETUP_CMID);

            }
        });
        findViewById(R.id.learn_more_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!CMIDUtils.isNetworkConnected(CMIDActivity.this)) {
                    CMIDUtils.launchWifiSetup(CMIDActivity.this);
                } else {
                    CMIDUtils.showLearnMoreDialog(CMIDActivity.this);
                }
            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CMID.REQUEST_CODE_SETUP_CMID && resultCode == RESULT_OK) {
            finish();
        } else if (requestCode == CMID.REQUEST_CODE_SETUP_WIFI && resultCode == Activity.RESULT_OK) {
            CMIDUtils.showLearnMoreDialog(this);
        }
    }
}