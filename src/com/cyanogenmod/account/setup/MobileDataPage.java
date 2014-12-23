/*
 * Copyright (C) 2014 The CyanogenMod Project
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

import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.Switch;

import com.cyanogenmod.account.R;
import com.cyanogenmod.account.ui.SetupPageFragment;
import com.cyanogenmod.account.util.CMAccountUtils;

public class MobileDataPage extends Page {

    private static final String TAG = MobileDataPage.class.getSimpleName();

    public MobileDataPage(Context context, SetupDataCallbacks callbacks, int titleResourceId) {
        super(context, callbacks, titleResourceId);
    }

    @Override
    public Fragment createFragment() {
        Bundle args = new Bundle();
        args.putString(Page.KEY_PAGE_ARGUMENT, getKey());

        MobileDataFragment fragment = new MobileDataFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getNextButtonResId() {
        return R.string.next;
    }

    public static class MobileDataFragment extends SetupPageFragment {

        private View mEnableDataRow;
        private Switch mEnableMobileData;

        private View.OnClickListener mEnableDataClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                boolean checked = !mEnableMobileData.isChecked();
                CMAccountUtils.setMobileDataEnabled(getActivity(), checked);
                mEnableMobileData.setChecked(checked);
            }
        };

        @Override
        protected void setUpPage() {
            mEnableDataRow = mRootView.findViewById(R.id.data);
            mEnableDataRow.setOnClickListener(mEnableDataClickListener);
            mEnableMobileData = (Switch) mRootView.findViewById(R.id.data_switch);
            updateDataConnectionStatus();
        }

        @Override
        protected int getLayoutResource() {
            return R.layout.mobile_data_settings;
        }

        @Override
        protected int getTitleResource() {
            return R.string.setup_mobile_data;
        }

        @Override
        public void onResume() {
            super.onResume();
            updateDataConnectionStatus();
        }

        private void updateDataConnectionStatus() {
            mEnableMobileData.setChecked(CMAccountUtils.isMobileDataEnabled(getActivity()));
        }

    }
}
