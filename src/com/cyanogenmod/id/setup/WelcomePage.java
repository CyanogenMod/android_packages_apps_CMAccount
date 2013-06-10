package com.cyanogenmod.id.setup;


import android.app.Fragment;
import android.content.Context;
import com.cyanogenmod.id.ui.WelcomeFragment;

public class WelcomePage extends Page {

    public WelcomePage(Context context, SetupDataCallbacks callbacks, int titleResourceId, int imageResourceId) {
        super(context, callbacks, titleResourceId, imageResourceId);
    }

    @Override
    public Fragment createFragment() {
        return WelcomeFragment.create(getKey());
    }


}
