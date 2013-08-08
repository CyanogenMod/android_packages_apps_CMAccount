package com.cyanogenmod.id.setup;

import com.cyanogenmod.id.R;
import com.cyanogenmod.id.ui.SetupPageFragment;
import com.cyanogenmod.id.ui.SetupWizardActivity;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.View;

public class FinishPage extends Page {

    public FinishPage(Context context, SetupDataCallbacks callbacks, int titleResourceId) {
        super(context, callbacks, titleResourceId);
    }

    @Override
    public Fragment createFragment() {
        Bundle args = new Bundle();
        args.putString(Page.KEY_PAGE_ARGUMENT, getKey());

        FinishFragment fragment = new FinishFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getNextButtonResId() {
        return R.string.bacon;
    }


    public static class FinishFragment extends SetupPageFragment {

        @Override
        protected void setUpPage() {
            mRootView.findViewById(R.id.btn_finish).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    ((SetupWizardActivity)getActivity()).doNext();
                }
            });
        }

        @Override
        protected int getLayoutResource() {
            return R.layout.setup_finished_page;
        }

    }

}
