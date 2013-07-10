package com.cyanogenmod.id.ui;

import com.cyanogenmod.id.setup.Page;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public abstract class SetupPageFragment extends Fragment {

    protected SetupWizardActivity mActivity;
    protected String mKey;
    protected Page mPage;
    protected View mRootView;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Bundle args = getArguments();
        mKey = args.getString(Page.KEY_PAGE_ARGUMENT);
        if (mKey == null) {
            throw new IllegalArgumentException("No KEY_PAGE_ARGUMENT given");
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(getLayoutResource(), container, false);
        TextView titleView = (TextView) mRootView.findViewById(android.R.id.title);
        titleView.setText(mKey);
        return mRootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPage = mActivity.getPage(mKey);
        setUpPage();
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof SetupWizardActivity)) {
            throw new ClassCastException("Activity must be SetupWizardActivity");
        }
        mActivity = (SetupWizardActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    protected abstract void setUpPage();
    protected abstract int getLayoutResource();
}
