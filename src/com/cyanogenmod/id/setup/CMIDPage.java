package com.cyanogenmod.id.setup;

import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.auth.AuthActivity;
import com.cyanogenmod.id.ui.SetupPageFragment;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

public class CMIDPage extends Page {

    public CMIDPage(Context context, SetupDataCallbacks callbacks, int titleResourceId, int imageResourceId) {
        super(context, callbacks, titleResourceId, imageResourceId);
    }

    @Override
    public Fragment createFragment() {
        Bundle args = new Bundle();
        args.putString(Page.KEY_PAGE_ARGUMENT, getKey());

        CMIDFragment fragment = new CMIDFragment();
        fragment.setArguments(args);
        return fragment;
    }


    @Override
    public int getNextButtonResId() {
        return R.string.skip;
    }

    public static class CMIDFragment extends SetupPageFragment {

        @Override
        protected void setUpPage() {
            mRootView.findViewById(R.id.existing_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AuthActivity.showForAuth(getActivity(), Constants.REQUEST_CODE_SETUP_CMID);

                }
            });
            mRootView.findViewById(R.id.new_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    AuthActivity.showForCreate(getActivity(), Constants.REQUEST_CODE_SETUP_CMID);

                }
            });
        }

        @Override
        protected int getLayoutResource() {
            return R.layout.cmid_account;
        }

    }
}
