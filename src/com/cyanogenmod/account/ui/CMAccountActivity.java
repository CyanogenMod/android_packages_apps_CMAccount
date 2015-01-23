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

package com.cyanogenmod.account.ui;

import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.R;
import com.cyanogenmod.account.auth.AuthActivity;
import com.cyanogenmod.account.auth.AuthClient;
import com.cyanogenmod.account.util.CMAccountUtils;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class CMAccountActivity extends Activity {

    private boolean mShowButtonBar;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cmaccount_setup_standalone);
        ((TextView)findViewById(android.R.id.title)).setText(R.string.cmaccount_add_title);
        findViewById(R.id.existing_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthActivity.showForAuth(CMAccountActivity.this,
                        CMAccount.REQUEST_CODE_SETUP_CMAccount,
                        mShowButtonBar);

            }
        });
        findViewById(R.id.new_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                /* DEPRECATED */
                // AuthActivity.showForCreate(CMAccountActivity.this, CMAccount.REQUEST_CODE_SETUP_CMAccount);
                Toast.makeText(CMAccountActivity.this,
                        R.string.cmaccount_deprecated, Toast.LENGTH_SHORT).show();

            }
        });
        findViewById(R.id.learn_more_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!CMAccountUtils.isNetworkConnected(CMAccountActivity.this) || !CMAccountUtils.isWifiConnected(CMAccountActivity.this)) {
                    CMAccountUtils.launchWifiSetup(CMAccountActivity.this);
                } else {
                    CMAccountUtils.showLearnMoreDialog(CMAccountActivity.this);
                }
            }
        });
        mShowButtonBar = getIntent().getBooleanExtra(CMAccount.EXTRA_SHOW_BUTTON_BAR, false);
        if (mShowButtonBar) {
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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CMAccount.REQUEST_CODE_SETUP_CMAccount &&
                (resultCode == RESULT_OK || resultCode == RESULT_FIRST_USER)) {
            finish();
        } else if (requestCode == CMAccount.REQUEST_CODE_SETUP_WIFI) {
            if (resultCode == Activity.RESULT_OK || CMAccountUtils.isNetworkConnected(CMAccountActivity.this)) {
                CMAccountUtils.showLearnMoreDialog(this);
            }
        }
    }
}