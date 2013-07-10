package com.cyanogenmod.id.setup;

import com.cyanogenmod.id.R;

import android.content.Context;

public class CMSetupWizardData extends AbstractSetupData {

    public CMSetupWizardData(Context context) {
        super(context);
    }

    @Override
    protected PageList onNewPageList() {
        return new PageList(new WelcomePage(mContext, this, R.string.setup_welcome, R.drawable.cid_welcome),
               new SimMissingPage(mContext, this, R.string.setup_sim_missing),
               new CMIDPage(mContext, this, R.string.setup_cmid, R.drawable.cid_welcome),
               new GoogleAccountPage(mContext, this, R.string.setup_google_account),
               new FinishPage(mContext, this, R.string.setup_complete, R.drawable.cid_welcome)
        );
    }


}
