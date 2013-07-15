package com.cyanogenmod.id.setup;

import com.cyanogenmod.id.R;
import com.cyanogenmod.id.ui.SetupPageFragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.widget.TextView;

public class SimMissingPage extends Page {

    public SimMissingPage(Context context, SetupDataCallbacks callbacks, int titleResourceId, int imageResId) {
        super(context, callbacks, titleResourceId, imageResId);
    }

    @Override
    public Fragment createFragment() {
        Bundle args = new Bundle();
        args.putString(Page.KEY_PAGE_ARGUMENT, getKey());

        SimMissingFragment fragment = new SimMissingFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getNextButtonResId() {
        return R.string.skip;
    }

    public static class SimMissingFragment extends SetupPageFragment {

        @Override
        protected void setUpPage() {
            TextView summaryView = (TextView) mRootView.findViewById(android.R.id.summary);
            summaryView.setText(R.string.sim_missing_summary);
        }

        @Override
        protected int getLayoutResource() {
            return R.layout.setup_fragment_page;
        }

    }
}
