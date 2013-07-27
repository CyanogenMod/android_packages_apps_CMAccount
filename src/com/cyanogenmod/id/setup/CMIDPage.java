package com.cyanogenmod.id.setup;

import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.auth.AuthActivity;
import com.cyanogenmod.id.gcm.GCMService;
import com.cyanogenmod.id.ui.SetupPageFragment;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.Account;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

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
        public void onActivityResult(int requestCode, int resultCode, Intent data) {
            if (requestCode == CMID.REQUEST_CODE_SETUP_CMID && resultCode == Activity.RESULT_OK) {
                GCMService.registerClient(getActivity());
                mCallbacks.onPageFinished(mPage);
            }
        }

        @Override
        protected void setUpPage() {
            mRootView.findViewById(R.id.existing_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    loginCMID();
                }
            });
            mRootView.findViewById(R.id.new_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    createCMID();
                }
            });
        }

        private void createCMID() {
            Intent intent = new Intent(getActivity(), AuthActivity.class);
            intent.putExtra(AuthActivity.EXTRA_PARAM_CREATE_ACCOUNT, true);
            startActivityForResult(intent, CMID.REQUEST_CODE_SETUP_CMID);
        }

        private void loginCMID() {
            startActivityForResult(new Intent(getActivity(), AuthActivity.class), CMID.REQUEST_CODE_SETUP_CMID);
        }

        @Override
        protected int getLayoutResource() {
            return R.layout.cmid_account;
        }

    }
}
