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
