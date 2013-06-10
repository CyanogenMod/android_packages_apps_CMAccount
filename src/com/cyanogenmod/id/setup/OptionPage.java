package com.cyanogenmod.id.setup;

import android.app.Fragment;
import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.AdapterView;
import com.cyanogenmod.id.ui.OptionFragment;

import java.util.ArrayList;
import java.util.Arrays;

public class OptionPage extends Page {

    protected ArrayList<Option> mChoices = new ArrayList<Option>();

    protected AdapterView.OnItemClickListener mOnItemClickListener;

    public OptionPage(Context context, SetupDataCallbacks callbacks, int titleResourceId, AdapterView.OnItemClickListener onItemClickListener) {
        this(context, callbacks, titleResourceId, -1, onItemClickListener);
    }

    public OptionPage(Context context, SetupDataCallbacks callbacks, int titleResourceId, int imageResourceId, AdapterView.OnItemClickListener onItemClickListener) {
        super(context, callbacks, titleResourceId, imageResourceId);
        mOnItemClickListener = onItemClickListener;
    }

    @Override
    public Fragment createFragment() {
        return OptionFragment.create(getKey());
    }

    public Option getOptionAt(int position) {
        return mChoices.get(position);
    }

    public int getOptionCount() {
        return mChoices.size();
    }

    @Override
    public boolean isCompleted() {
        return !TextUtils.isEmpty(mData.getString(DATA_KEY));
    }

    public OptionPage setChoices(Option... choices) {
        mChoices.addAll(Arrays.asList(choices));
        return this;
    }

    public OptionPage setValue(String value) {
        mData.putString(DATA_KEY, value);
        return this;
    }

    public void onListItemClick(AdapterView adapterView, View v, int position, long id) {
        if (mOnItemClickListener != null) {
            mOnItemClickListener.onItemClick(adapterView, v, position, id);
        }
    }

    public static class Option {
        private int mId;
        private String mTitle;

        public Option(int id, String title) {
            mId = id;
            mTitle = title;
        }

        public int getId() {
            return mId;
        }

        public String getTitle() {
            return mTitle;
        }
    }
}
