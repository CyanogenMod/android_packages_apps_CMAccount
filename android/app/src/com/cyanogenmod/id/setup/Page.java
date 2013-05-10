package com.cyanogenmod.id.setup;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;


public abstract class Page implements PageNode {

    public static final String DATA_KEY = "data-value";
    public static final String KEY_PAGE_ARGUMENT = "key_arg";

    protected SetupDataCallbacks mCallbacks;

    protected Bundle mData = new Bundle();
    protected String mTitle;
    protected int mTitleResourceId;
    protected int mImageResourceId = -1;
    protected boolean mRequired = false;

    protected Page(Context context, SetupDataCallbacks callbacks, int titleResourceId) {
        this(context, callbacks, titleResourceId, -1);
    }

    protected Page(Context context, SetupDataCallbacks callbacks, int titleResourceId, int imageResourceId) {
        mCallbacks = callbacks;
        mTitleResourceId = titleResourceId;
        mImageResourceId = imageResourceId;
        mTitle = context.getString(mTitleResourceId);
    }

    public Bundle getData() {
        return mData;
    }

    public String getTitle() {
        return mTitle;
    }

    public boolean isRequired() {
        return mRequired;
    }

    @Override
    public Page findPage(String key) {
        return getKey().equals(key) ? this : null;
    }

    public abstract Fragment createFragment();

    public int getId() {
        return mTitleResourceId;
    }

    public String getKey() {
        return mTitle;
    }

    public boolean isCompleted() {
        return true;
    }

    public void resetData(Bundle data) {
        mData = data;
        notifyDataChanged();
    }

    public void notifyDataChanged() {
        mCallbacks.onPageDataChanged(this);
    }

    public Page setRequired(boolean required) {
        mRequired = required;
        return this;
    }

    public Page setImageResourceId(int imageResourceId) {
        mImageResourceId = imageResourceId;
        return this;
    }

    public int getImageResourceId() {
        return mImageResourceId;
    }

}
