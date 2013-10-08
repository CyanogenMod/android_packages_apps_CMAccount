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

public class CMAccountActivity extends Activity {

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.cmaccount_setup_standalone);
        findViewById(R.id.existing_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthActivity.showForAuth(CMAccountActivity.this, CMAccount.REQUEST_CODE_SETUP_CMAccount);

            }
        });
        findViewById(R.id.new_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                AuthActivity.showForCreate(CMAccountActivity.this, CMAccount.REQUEST_CODE_SETUP_CMAccount);

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
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CMAccount.REQUEST_CODE_SETUP_CMAccount && resultCode == RESULT_OK) {
            finish();
        } else if (requestCode == CMAccount.REQUEST_CODE_SETUP_WIFI) {
            if (resultCode == Activity.RESULT_OK || CMAccountUtils.isNetworkConnected(CMAccountActivity.this)) {
                CMAccountUtils.showLearnMoreDialog(this);
            }
        }
    }
}