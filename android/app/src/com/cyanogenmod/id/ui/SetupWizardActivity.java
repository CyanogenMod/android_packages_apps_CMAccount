package com.cyanogenmod.id.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;
import android.support.v13.app.FragmentStatePagerAdapter;
import android.support.v4.view.ViewPager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.setup.AbstractSetupData;
import com.cyanogenmod.id.setup.CMSetupWizardData;
import com.cyanogenmod.id.setup.SetupDataCallbacks;
import com.cyanogenmod.id.setup.Page;

import java.util.List;

public class SetupWizardActivity extends Activity implements SetupDataCallbacks {

    private static final int RESULT_CODE_SETUP_WIFI = 0;
    private static final int RESULT_CODE_SETUP_GOOGLE_ACCOUNT = 1;

    private ViewPager mViewPager;
    private CMPagerAdapter mPagerAdapter;

    private Button mNextButton;
    private Button mPrevButton;

    private List<Page> mPageList;

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
                updateButtonBar();
            }
        });
        mNextButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                doNextForPage(mViewPager.getCurrentItem());
            }
        });
        mPrevButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mViewPager.setCurrentItem(mViewPager.getCurrentItem() - 1);
            }
        });
        onPageTreeChanged();
        updateButtonBar();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        mSetupData.unregisterListener(this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == RESULT_CODE_SETUP_WIFI && resultCode == RESULT_OK) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
        } else if (requestCode == RESULT_CODE_SETUP_GOOGLE_ACCOUNT) {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
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

    private void doNextForPage(int currentPage) {
        // XXX TODO: Implement an elegant way to control next behavior.
        if (currentPage == mPageList.size()) {
            Settings.Global.putInt(getContentResolver(), Settings.Global.DEVICE_PROVISIONED, 1);
            Settings.Secure.putInt(getContentResolver(), Settings.Secure.USER_SETUP_COMPLETE, 1);
            //XXX: Commented out for devvin'
//                    PackageManager pm = getPackageManager();
//                    pm.setComponentEnabledSetting(getComponentName(), PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP);

            finish();
        } else if (currentPage == 0) {
            Intent intent = new Intent(Constants.ACTION_SETUP_WIFI);
            intent.putExtra(Constants.EXTRA_FIRST_RUN, true);
            intent.putExtra(Constants.EXTRA_SHOW_BUTTON_BAR, true);
            intent.putExtra(Constants.EXTRA_SHOW_WIFI_MENU, true);
            startActivityForResult(intent, RESULT_CODE_SETUP_WIFI);
        } else if (currentPage == 1) {
            launchGoogleAccountSetup();
        } else {
            mViewPager.setCurrentItem(mViewPager.getCurrentItem() + 1);
        }
    }

    private void updateButtonBar() {
        int position = mViewPager.getCurrentItem();
        if (position == mPageList.size()) {
            mNextButton.setText(R.string.finish);
        } else {
            mNextButton.setText(R.string.next);
            mNextButton.setEnabled(position != mPagerAdapter.getCutOffPage());
        }

        mPrevButton.setVisibility(position <= 0 ? View.INVISIBLE : View.VISIBLE);
    }

    @Override
    public void onPageDataChanged(Page page) {
        if (page.isRequired()) {
            if (recalculateCutOffPage()) {
                mPagerAdapter.notifyDataSetChanged();
                updateButtonBar();
            }
        }
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
        int cutOffPage = mPageList.size() + 1;
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

    private void launchGoogleAccountSetup() {
        Intent intent = new Intent(Constants.ACTION_SETUP_GOOGLE_ACCOUNT);
        startActivityForResult(intent, RESULT_CODE_SETUP_GOOGLE_ACCOUNT);
    }

    private class CMPagerAdapter extends FragmentStatePagerAdapter {

        private int mCutOffPage;
        private Fragment mPrimaryItem;

        private CMPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int i) {
            if (i == mPageList.size()) {
                return new FinishFragment();
            }

            return mPageList.get(i).createFragment();
        }

        @Override
        public int getItemPosition(Object object) {
            if (object == mPrimaryItem) {
                return POSITION_UNCHANGED;
            }

            return POSITION_NONE;
        }

        @Override
        public void setPrimaryItem(ViewGroup container, int position, Object object) {
            super.setPrimaryItem(container, position, object);
            mPrimaryItem = (Fragment) object;
        }

        @Override
        public int getCount() {
            return Math.min(mCutOffPage + 1, mPageList.size() + 1);
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