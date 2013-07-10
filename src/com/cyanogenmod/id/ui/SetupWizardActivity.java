package com.cyanogenmod.id.ui;

import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.setup.AbstractSetupData;
import com.cyanogenmod.id.setup.CMSetupWizardData;
import com.cyanogenmod.id.setup.GoogleAccountPage;
import com.cyanogenmod.id.setup.Page;
import com.cyanogenmod.id.setup.PageList;
import com.cyanogenmod.id.setup.SetupDataCallbacks;
import com.cyanogenmod.id.setup.SimMissingPage;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AccountManagerCallback;
import android.accounts.AccountManagerFuture;
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.Bundle;
import android.os.Handler;
import android.provider.Settings;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import java.util.List;

public class SetupWizardActivity extends Activity implements SetupDataCallbacks {

    private static final String TAG = SetupWizardActivity.class.getSimpleName();

    private static final String DEFAULT_LAUNCHER = "com.cyanogenmod.trebuchet.Launcher";

    private ViewPager mViewPager;
    private CMPagerAdapter mPagerAdapter;

    private Button mNextButton;
    private Button mPrevButton;

    private PageList mPageList;

    private AbstractSetupData mSetupData;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.setup_main);
        mSetupData = (AbstractSetupData)getLastNonConfigurationInstance();
        if (mSetupData == null) {
            mSetupData = new CMSetupWizardData(this);
        }

        if (savedInstanceState != null) {
            mSetupData.load(savedInstanceState.getBundle("data"));
        }
        mSetupData.registerListener(this);
        mPagerAdapter = new CMPagerAdapter(getFragmentManager());
        mViewPager = (ViewPager) findViewById(R.id.pager);
        mViewPager.setAdapter(mPagerAdapter);

        mNextButton = (Button) findViewById(R.id.next_button);
        mPrevButton = (Button) findViewById(R.id.prev_button);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position < mPageList.size()) {
                    onPageLoaded(mPageList.get(position));
                }
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doNext();
            }
        });
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doPrevious();
            }
        });
        doSimCheck();
        onPageTreeChanged();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSetupData.unregisterListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == Constants.REQUEST_CODE_SETUP_WIFI && resultCode != RESULT_OK) {
            doNext();
        } else if (requestCode == Constants.REQUEST_CODE_SETUP_CMID && resultCode == RESULT_OK) {
            Page cmidPage = mPageList.findPage(R.string.setup_cmid);
            removeSetupPage(cmidPage);
        }
    }

    @Override
    public Object onRetainNonConfigurationInstance() {
        return mSetupData;
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBundle("data", mSetupData.save());
    }

    public void doNext() {
        final int currentItem = mViewPager.getCurrentItem();
        final Page currentPage = mPageList.get(currentItem);
        if (currentPage.getId() == R.string.setup_sim_missing) {
            removeSetupPage(currentPage);
        } else if (currentPage.getId() == R.string.setup_complete) {
            finishSetup();
        } else {
            mViewPager.setCurrentItem(currentItem + 1);
        }
    }

    public void doPrevious() {
        final int currentItem = mViewPager.getCurrentItem();
        if (currentItem > 0 ) {
            mViewPager.setCurrentItem(currentItem - 1);
        }
    }

    private void removeSetupPage(final Page page) {
        final int position = mViewPager.getCurrentItem();
        mViewPager.setCurrentItem(0);
        mSetupData.removePage(page);
        mViewPager.setCurrentItem(position);
    }

    private void updateButtonBar() {
        final int position = mViewPager.getCurrentItem();
        mNextButton.setEnabled(position != mPagerAdapter.getCutOffPage());
        mPrevButton.setVisibility(position <= 0 ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public void onPageLoaded(Page page) {
        mNextButton.setText(page.getNextButtonResId());
        switch (page.getId()) {
            case R.string.setup_cmid:
                if (accountExists(Constants.ACCOUNT_TYPE_CMID)) {
                    removeSetupPage(page);
                } else if (!CMIDUtils.isNetworkConnected(this)) {
                    launchWifiSetup();
                }
                break;
            case R.string.setup_google_account:
                if (accountExists(Constants.ACCOUNT_TYPE_GOOGLE)) {
                    removeSetupPage(page);
                }
                break;
        }
        if (page.isRequired()) {
            if (recalculateCutOffPage()) {
                mPagerAdapter.notifyDataSetChanged();
            }
        }
        updateButtonBar();
    }

    @Override
    public void onPageTreeChanged() {
        mPageList = mSetupData.getPageList();
        recalculateCutOffPage();
        mPagerAdapter.notifyDataSetChanged();
        updateButtonBar();
    }

    public Page getPage(String key) {
        return mSetupData.findPage(key);
    }

    private boolean recalculateCutOffPage() {
        // Cut off the pager adapter at first required page that isn't completed
        int cutOffPage = mPageList.size();
        for (int i = 0; i < mPageList.size(); i++) {
            Page page = mPageList.get(i);
            if (page.isRequired() && !page.isCompleted()) {
                cutOffPage = i;
                break;
            }
        }

        if (mPagerAdapter.getCutOffPage() != cutOffPage) {
            mPagerAdapter.setCutOffPage(cutOffPage);
            return true;
        }

        return false;
    }

    private void launchWifiSetup() {
        CMIDUtils.tryEnablingWifi(this);
        Intent intent = new Intent(Constants.ACTION_SETUP_WIFI);
        intent.putExtra(Constants.EXTRA_FIRST_RUN, true);
        intent.putExtra(Constants.EXTRA_SHOW_BUTTON_BAR, true);
        intent.putExtra(Constants.EXTRA_SHOW_WIFI_MENU, true);
        startActivityForResult(intent, Constants.REQUEST_CODE_SETUP_WIFI);
    }

    private void enableDefaultHome(Intent intent) {
        final PackageManager pm = getPackageManager();
        final List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : resolveInfos) {
            if (!DEFAULT_LAUNCHER.equals(info.activityInfo.name)) {
                final ComponentName componentName = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
        }
    }

    public void onGoogleAccountSetupLaunched() {
        if (accountExists(Constants.ACCOUNT_TYPE_GOOGLE)) {
            removeSetupPage(mPageList.findPage(R.string.setup_google_account));
        } else {
            doNext();
        }
    }

    private void finishSetup() {
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        enableDefaultHome(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | intent.getFlags());
        startActivity(intent);
        finish();
    }

    private void doSimCheck() {
        if (!CMIDUtils.isGSMPhone(this) || !CMIDUtils.isSimMissing(this)) {
            Page page = mPageList.findPage(R.string.setup_sim_missing);
            if (page != null) {
                mSetupData.removePage(page);
            }
        }
    }

    private boolean accountExists(String accountType) {
        return AccountManager.get(this).getAccountsByType(accountType).length > 0;
    }

    private class CMPagerAdapter extends FragmentStatePagerAdapter {

        private int mCutOffPage;

        private CMPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            return mPageList.get(i).createFragment();
        }

        @Override
        public int getItemPosition(Object object) {
            return POSITION_NONE;
        }

        @Override
        public int getCount() {
            return Math.min(mCutOffPage, mPageList.size());
        }

        public void setCutOffPage(int cutOffPage) {
            if (cutOffPage < 0) {
                cutOffPage = Integer.MAX_VALUE;
            }
            mCutOffPage = cutOffPage;
        }

        public int getCutOffPage() {
            return mCutOffPage;
        }
    }
}