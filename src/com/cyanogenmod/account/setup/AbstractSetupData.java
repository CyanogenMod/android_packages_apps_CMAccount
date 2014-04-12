/*
 * Copyright (C) 2013 The CyanogenMod Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cyanogenmod.account.setup;

import android.content.Context;
import android.os.Bundle;

import java.util.ArrayList;

public abstract class AbstractSetupData implements SetupDataCallbacks {

    private static final String TAG = AbstractSetupData.class.getSimpleName();

    protected Context mContext;
    private ArrayList<SetupDataCallbacks> mListeners = new ArrayList<SetupDataCallbacks>();
    private PageList mPageList;

    public AbstractSetupData(Context context) {
        mContext = context;
        mPageList = onNewPageList();
    }

    protected abstract PageList onNewPageList();

    @Override
    public void onPageLoaded(Page page) {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onPageLoaded(page);
        }
    }

    @Override
    public void onPageTreeChanged() {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onPageTreeChanged();
        }
    }

    @Override
    public void onPageFinished(Page page) {
        for (int i = 0; i < mListeners.size(); i++) {
            mListeners.get(i).onPageFinished(page);
        }
    }

    @Override
    public Page getPage(String key) {
        return findPage(key);
    }

    @Override
    public Page getPage(int key) {
        return findPage(key);
    }

    public Page findPage(String key) {
        return mPageList.findPage(key);
    }

    public Page findPage(int key) {
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

    public void addPage(int index, Page page) {
        mPageList.add(index, page);
        onPageTreeChanged();
    }

    public void removePage(Page page) {
        mPageList.remove(page);
        onPageTreeChanged();
    }

    public void registerListener(SetupDataCallbacks listener) {
        mListeners.add(listener);
    }

    public PageList getPageList() {
        return mPageList;
    }

    public void unregisterListener(SetupDataCallbacks listener) {
        mListeners.remove(listener);
    }
}
