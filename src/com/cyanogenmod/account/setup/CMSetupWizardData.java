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

import com.cyanogenmod.account.R;
import com.cyanogenmod.account.util.CMAccountUtils;

import android.content.Context;

import java.util.ArrayList;

public class CMSetupWizardData extends AbstractSetupData {

    public CMSetupWizardData(Context context) {
        super(context);
    }

    @Override
    protected PageList onNewPageList() {
        ArrayList<Page> pages = new ArrayList<Page>();
        pages.add(new WelcomePage(mContext, this, R.string.setup_welcome));
        if (CMAccountUtils.hasTelephony(mContext) &&
                !CMAccountUtils.isMobileDataEnabled(mContext)) {
            pages.add(new MobileDataPage(mContext, this, R.string.setup_mobile_data));
        }
        pages.add(new GoogleAccountPage(mContext, this, R.string.setup_google_account));
        pages.add(new CMAccountPage(mContext, this, R.string.setup_cmaccount));
        pages.add(new LocationSettingsPage(mContext, this, R.string.setup_location));
        pages.add(new PersonalizationPage(mContext, this, R.string.setup_personalization));
        pages.add(new DateTimePage(mContext, this, R.string.setup_datetime));
        pages.add(new FinishPage(mContext, this, R.string.setup_complete));
        return new PageList(pages.toArray(new Page[pages.size()]));
    }


}