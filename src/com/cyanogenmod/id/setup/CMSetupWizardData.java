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

package com.cyanogenmod.id.setup;

import com.cyanogenmod.id.R;

import android.content.Context;

public class CMSetupWizardData extends AbstractSetupData {

    public CMSetupWizardData(Context context) {
        super(context);
    }

    @Override
    protected PageList onNewPageList() {
        return new PageList(new WelcomePage(mContext, this, R.string.setup_welcome),
               new CMIDPage(mContext, this, R.string.setup_cmid),
               new GoogleAccountPage(mContext, this, R.string.setup_google_account),
               new LocationSettingsPage(mContext, this, R.string.setup_location),
               new DateTimePage(mContext, this, R.string.setup_datetime),
               new FinishPage(mContext, this, R.string.setup_complete)
        );
    }


}
