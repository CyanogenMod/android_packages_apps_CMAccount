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

package com.cyanogenmod.account.setup;

import com.cyanogenmod.account.CMAccount;
import com.cyanogenmod.account.R;
import com.cyanogenmod.account.auth.AuthActivity;
import com.cyanogenmod.account.auth.AuthClient;
import com.cyanogenmod.account.ui.WebViewDialogFragment;
import com.cyanogenmod.account.ui.SetupPageFragment;
import com.cyanogenmod.account.util.CMAccountUtils;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;

public class CMAccountPage extends Page {

    public CMAccountPage(Context context, SetupDataCallbacks callbacks, int titleResourceId) {
        super(context, callbacks, titleResourceId);
    }

    @Override
    public Fragment createFragment() {
        Bundle args = new Bundle();
        args.putString(Page.KEY_PAGE_ARGUMENT, getKey());

        CMAccountFragment fragment = new CMAccountFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getNextButtonResId() {
        return R.string.skip;
    }

    public static class CMAccountFragment extends SetupPageFragment {

        @Override
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == CMAccount.REQUEST_CODE_SETUP_CMAccount && resultCode == Activity.RESULT_OK) {
                mCallbacks.onPageFinished(mPage);
            } else if (requestCode == CMAccount.REQUEST_CODE_SETUP_WIFI) {
                if (resultCode == Activity.RESULT_OK || CMAccountUtils.isNetworkConnected(getActivity())) {
                    CMAccountUtils.showLearnMoreDialog(getActivity());
                }
            }
        }

        @Override
        protected void setUpPage() {
            mRootView.findViewById(R.id.existing_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loginCMAccount();
                }
            });
            mRootView.findViewById(R.id.new_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    createCMAccount();
                }
            });
            mRootView.findViewById(R.id.learn_more_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!CMAccountUtils.isWifiConnected(getActivity()) || !CMAccountUtils.isNetworkConnected(getActivity())) {
                        CMAccountUtils.launchWifiSetup(CMAccountFragment.this);
                    } else {
                        CMAccountUtils.showLearnMoreDialog(getActivity());
                    }
                }
            });
        }

        private void createCMAccount() {
            Intent intent = new Intent(getActivity(), AuthActivity.class);
            intent.putExtra(AuthActivity.EXTRA_PARAM_CREATE_ACCOUNT, true);
            startActivityForResult(intent, CMAccount.REQUEST_CODE_SETUP_CMAccount);
        }

        private void loginCMAccount() {
            startActivityForResult(new Intent(getActivity(), AuthActivity.class), CMAccount.REQUEST_CODE_SETUP_CMAccount);
        }

        @Override
        protected int getLayoutResource() {
            return R.layout.cmaccount;
        }

        @Override
        protected int getTitleResource() {
            return R.string.setup_cmaccount;
        }
    }
}
