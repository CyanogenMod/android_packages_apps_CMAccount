package com.cyanogenmod.id.setup;

import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.ui.SetupPageFragment;

import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

public class GoogleAccountPage extends Page {

    private static final String TAG = GoogleAccountPage.class.getSimpleName();

    public GoogleAccountPage(Context context, SetupDataCallbacks callbacks, int titleResourceId, int imageResId) {
        super(context, callbacks, titleResourceId, imageResId);
    }

    @Override
    public Fragment createFragment() {
        Bundle args = new Bundle();
        args.putString(Page.KEY_PAGE_ARGUMENT, getKey());

        GoogleAccountFragment fragment = new GoogleAccountFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getNextButtonResId() {
        return R.string.skip;
    }

    public static class GoogleAccountFragment extends SetupPageFragment {

        @Override
        protected void setUpPage() {
            TextView summaryView = (TextView) mRootView.findViewById(android.R.id.summary);
            summaryView.setText(R.string.google_account_summary);
            mRootView.findViewById(R.id.google_button).setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    launchGoogleAccountSetup();
                }
            });
        }

        private void launchGoogleAccountSetup() {
            Bundle bundle = new Bundle();
            bundle.putBoolean(Constants.EXTRA_FIRST_RUN, true);
            bundle.putBoolean(Constants.EXTRA_ALLOW_SKIP, true);
            AccountManager.get(getActivity()).addAccount(Constants.ACCOUNT_TYPE_GOOGLE, null, null, bundle, getActivity(), new AccountManagerCallback<Bundle>() {
                @Override
                public void run(AccountManagerFuture<Bundle> bundleAccountManagerFuture) {
                    mCallbacks.onPageFinished(mPage);
                }
            }, null);
        }

        @Override
        protected int getLayoutResource() {
            return R.layout.setup_google_account_page;
        }

    }
}
