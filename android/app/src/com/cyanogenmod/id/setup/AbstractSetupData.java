package com.cyanogenmod.id.setup;

import android.content.Context;
import android.os.Bundle;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents the wizard data, including the pages/steps in the wizard.
 */
public abstract class AbstractSetupData implements SetupDataCallbacks {
    protected Context mContext;

    private List<SetupDataCallbacks> mListeners = new ArrayList<SetupDataCallbacks>();
    private PageList mPageList;

    public AbstractSetupData(Context context) {
        mContext = context;
        mPageList = onNewPageList();
    }

    protected abstract PageList onNewPageList();

    @Override
    public void onPageDataChanged(Page page) {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onPageDataChanged(page);
        }
    }

    @Override
    public void onPageTreeChanged() {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onPageTreeChanged();
        }
    }

    public Page findPage(String key) {
        return mPageList.findPage(key);
    }

    public void load(Bundle savedValues) {
        for (String key : savedValues.keySet()) {
            mPageList.findPage(key).resetData(savedValues.getBundle(key));
        }
    }

    public Bundle save() {
        Bundle bundle = new Bundle();
        for (Page page : getPageList()) {
            bundle.putBundle(page.getKey(), page.getData());
        }
        return bundle;
    }

    public void registerListener(SetupDataCallbacks listener) {
        mListeners.add(listener);
    }

    public List<Page> getPageList() {
        return mPageList;
    }

    public void unregisterListener(SetupDataCallbacks listener) {
        mListeners.remove(listener);
    }
}
