package com.cyanogenmod.id.setup;

import com.cyanogenmod.id.R;
import com.cyanogenmod.id.ui.SetupPageFragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FinishPage extends Page {

    public FinishPage(Context context, SetupDataCallbacks callbacks, int titleResourceId, int imageResId) {
        super(context, callbacks, titleResourceId, imageResId);
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
        return R.string.finish;
    }


    public static class FinishFragment extends SetupPageFragment {

        @Override
        protected void setUpPage() {}

        @Override
        protected int getLayoutResource() {
            return R.layout.setup_fragment_page;
        }

    }

}
