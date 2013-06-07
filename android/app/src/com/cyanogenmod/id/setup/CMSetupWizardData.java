package com.cyanogenmod.id.setup;

import android.app.Activity;
import android.content.Context;
import android.view.View;
import android.widget.AdapterView;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.auth.AuthActivity;

public class CMSetupWizardData extends AbstractSetupData {

    public CMSetupWizardData(Context context) {
        super(context);
    }

    @Override
    protected PageList onNewPageList() {
        AdapterView.OnItemClickListener cmidItemClickListener = new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
                OptionPage.Option option = (OptionPage.Option)adapterView.getItemAtPosition(position);
                switch (option.getId()) {
                    case R.string.setup_option_create_cmid:
                        AuthActivity.showForCreate((Activity)mContext, 0);
                    case R.string.setup_option_login_cmid:
                        AuthActivity.showForAuth((Activity)mContext, 0);
                }
            }
        };
        return new PageList(new WelcomePage(mContext, this, R.string.setup_welcome, R.drawable.cid_welcome),
            new OptionPage(mContext, this, R.string.setup_cmid, R.drawable.cid_welcome, cmidItemClickListener)
                    .setChoices(new OptionPage.Option(R.string.setup_option_login_cmid, mContext.getString(R.string.setup_option_login_cmid)),
                            new OptionPage.Option(R.string.setup_option_create_cmid, mContext.getString(R.string.setup_option_create_cmid)))
        );
    }


}
