package com.cyanogenmod.id.ui;

import com.cyanogenmod.id.CMID;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.gcm.GCMUtil;
import com.cyanogenmod.id.setup.AbstractSetupData;
import com.cyanogenmod.id.setup.CMSetupWizardData;
import com.cyanogenmod.id.setup.Page;
import com.cyanogenmod.id.setup.PageList;
import com.cyanogenmod.id.setup.SetupDataCallbacks;
import com.cyanogenmod.id.util.CMIDUtils;

import android.accounts.AccountManager;
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
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;

import java.util.List;

public class SetupWizardActivity extends Activity implements SetupDataCallbacks {

    private static final String TAG = SetupWizardActivity.class.getSimpleName();

    private static final String GOOGLE_SETUPWIZARD_PACKAGE = "com.google.android.setupwizard";

    private ViewPager mViewPager;
    private CMPagerAdapter mPagerAdapter;

    private MenuItem mNextMenuItem;

    private PageList mPageList;

    private AbstractSetupData mSetupData;

    private final Handler mHandler = new Handler();

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
        mViewPager.setPageTransformer(true, new DepthPageTransformer());
        mViewPager.setAdapter(mPagerAdapter);
        mViewPager.setOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageSelected(int position) {
                if (position < mPageList.size()) {
                    onPageLoaded(mPageList.get(position));
                }
            }
        });
        onPageTreeChanged();
        removeUnNeededPages();
    }

    @Override
    protected void onResume() {
        super.onResume();
        onPageTreeChanged();
        if (!CMIDUtils.isNetworkConnected(this)) {
            CMIDUtils.tryEnablingWifi(this);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSetupData.unregisterListener(this);
    }

    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.setup, menu);
        mNextMenuItem = menu.findItem(R.id.action_next);
        updateNextPreviousState();
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_next:
                doNext();
                break;
        }
        return super.onOptionsItemSelected(item);
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
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final int currentItem = mViewPager.getCurrentItem();
                final Page currentPage = mPageList.get(currentItem);
                if (currentPage.getId() == R.string.setup_sim_missing) {
                    removeSetupPage(currentPage, true);
                } else if (currentPage.getId() == R.string.setup_complete) {
                    finishSetup();
                } else {
                    mViewPager.setCurrentItem(currentItem + 1, true);
                }
            }
        });
    }

    public void doPrevious() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                final int currentItem = mViewPager.getCurrentItem();
                if (currentItem > 0) {
                    mViewPager.setCurrentItem(currentItem - 1, true);
                }
            }
        });
    }

    private void removeSetupPage(final Page page, boolean animate) {
        if (page == null || getPage(page.getKey()) == null || page.getId() == R.string.setup_complete) return;
        if (animate) {
            final int position = mViewPager.getCurrentItem();
            mViewPager.setCurrentItem(0);
            mSetupData.removePage(page);
            mViewPager.setCurrentItem(position, true);
        } else {
            mSetupData.removePage(page);
        }
    }

    private void updateNextPreviousState() {
        final int position = mViewPager.getCurrentItem();
        final Page currentPage = mPageList.get(position);
        final int nextButtonResId = currentPage.getNextButtonResId();
        // onCreateOptionsMenu happens post create/resume. So this could initially be null;
        if (mNextMenuItem != null) {
            if (nextButtonResId != -1) {
                mNextMenuItem.setEnabled(true);
                mNextMenuItem.setTitle(nextButtonResId);
            } else {
                mNextMenuItem.setEnabled(false);
                mNextMenuItem.setTitle("");
            }
        }
    }

    @Override
    public void onPageLoaded(Page page) {
        if (page.isRequired()) {
            if (recalculateCutOffPage()) {
                mPagerAdapter.notifyDataSetChanged();
            }
        }
        updateNextPreviousState();
    }

    @Override
    public void onPageTreeChanged() {
        mPageList = mSetupData.getPageList();
        recalculateCutOffPage();
        mPagerAdapter.notifyDataSetChanged();
        updateNextPreviousState();
    }

    @Override
    public Page getPage(String key) {
        return mSetupData.findPage(key);
    }

    @Override
    public void onPageFinished(final Page page) {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                switch (page.getId()) {
                    case R.string.setup_cmid:
                        if (CMIDUtils.getCMIDAccount(SetupWizardActivity.this) != null) {
                            removeSetupPage(page, false);
                        } else {
                            doNext();
                        }
                        break;
                    case R.string.setup_google_account:
                        if (accountExists(CMID.ACCOUNT_TYPE_GOOGLE)) {
                            removeSetupPage(page, false);
                        } else {
                            doNext();
                        }
                        break;
                }
                onPageTreeChanged();
            }
        });
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

    private void removeUnNeededPages() {
        mHandler.post(new Runnable() {
            @Override
            public void run() {
                Page page = mPageList.findPage(R.string.setup_cmid);
                if (page != null && accountExists(CMID.ACCOUNT_TYPE_CMID)) {
                    removeSetupPage(page, false);
                }
                page = mPageList.findPage(R.string.setup_google_account);
                if (page != null && (!GCMUtil.googleServicesExist(SetupWizardActivity.this) || accountExists(CMID.ACCOUNT_TYPE_GOOGLE))) {
                    removeSetupPage(page, false);
                }
                if (!CMIDUtils.isGSMPhone(SetupWizardActivity.this) || !CMIDUtils.isSimMissing(SetupWizardActivity.this)) {
                    page = mPageList.findPage(R.string.setup_sim_missing);
                    if (page != null) {
                        removeSetupPage(page, false);
                    }
                }
                onPageTreeChanged();
            }
        });
    }

    private void disableSetupWizards(Intent intent) {
        final PackageManager pm = getPackageManager();
        final List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        for (ResolveInfo info : resolveInfos) {
            if (GOOGLE_SETUPWIZARD_PACKAGE.equals(info.activityInfo.packageName)) {
                final ComponentName componentName = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                pm.setComponentEnabledSetting(componentName, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
            }
        }
        pm.setComponentEnabledSetting(getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);
    }

    private void finishSetup() {
        Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
        Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);
        Intent intent = new Intent("android.intent.action.MAIN");
        intent.addCategory("android.intent.category.HOME");
        disableSetupWizards(intent);
        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | intent.getFlags());
        startActivity(intent);
        finish();
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
            if (mPageList == null)
                return 0;
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



    public static class DepthPageTransformer implements ViewPager.PageTransformer {
        private static float MIN_SCALE = 0.5f;

        public void transformPage(View view, float position) {
            int pageWidth = view.getWidth();

            if (position < -1) {
                view.setAlpha(0);

            } else if (position <= 0) { // [-1,0]
                // Use the default slide transition when moving to the left page
                view.setAlpha(1);
                view.setTranslationX(0);
                view.setScaleX(1);
                view.setScaleY(1);

            } else if (position <= 1) { // (0,1]
                // Fade the page out.
                view.setAlpha(1 - position);

                // Counteract the default slide transition
                view.setTranslationX(pageWidth * -position);

                // Scale the page down (between MIN_SCALE and 1)
                float scaleFactor = MIN_SCALE
                        + (1 - MIN_SCALE) * (1 - Math.abs(position));
                view.setScaleX(scaleFactor);
                view.setScaleY(scaleFactor);

            } else { // (1,+Infinity]
                // This page is way off-screen to the right.
                view.setAlpha(0);
            }
        }
    }
}