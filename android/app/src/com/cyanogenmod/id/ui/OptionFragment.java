package com.cyanogenmod.id.ui;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.cyanogenmod.id.setup.OptionPage;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.setup.Page;

import java.util.ArrayList;
import java.util.List;

public class OptionFragment extends ListFragment {

    protected SetupWizardActivity mActivity;
    protected List<OptionPage.Option> mChoices;
    protected String mKey;
    protected OptionPage mPage;

    private View mRootView;

    public static OptionFragment create(String key) {
        Bundle args = new Bundle();
        args.putString(Page.KEY_PAGE_ARGUMENT, key);
        OptionFragment fragment = new OptionFragment();
        fragment.setArguments(args);
        return fragment;
    }

    public OptionFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        mKey = args.getString(Page.KEY_PAGE_ARGUMENT);

    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        mPage = (OptionPage)mActivity.getPage(mKey);

        mChoices = new ArrayList<OptionPage.Option>();
        for (int i = 0; i < mPage.getOptionCount(); i++) {
            mChoices.add(mPage.getOptionAt(i));
        }
        ((TextView) mRootView.findViewById(android.R.id.title)).setText(mPage.getTitle());
        int imageId = mPage.getImageResourceId();
        if (imageId != -1) {
            ((ImageView) mRootView.findViewById(R.id.setup_img)).setImageResource(imageId);
        }

        setListAdapter(new OptionAdapter(getActivity(),
                android.R.layout.simple_list_item_1,
                mChoices));
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_page, container, false);
        return mRootView;
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof SetupWizardActivity)) {
            throw new ClassCastException("Activity must be SetupWizardActivity");
        }
        mActivity = (SetupWizardActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
          mPage.onListItemClick(l, v, position, id);
    }

    private class OptionAdapter extends ArrayAdapter<OptionPage.Option> {

        private LayoutInflater mLayoutInflater;
        private int mLayoutResourceId;

        private OptionAdapter(Context context, int resource, List<OptionPage.Option> objects) {
            super(context, resource, objects);
            mLayoutInflater = LayoutInflater.from(context);
            mLayoutResourceId = resource;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            return createViewFromResource(position, convertView, parent);
        }

        private View createViewFromResource(int position, View convertView, ViewGroup parent) {
            View view;
            TextView text;

            if (convertView == null) {
                view = mLayoutInflater.inflate(mLayoutResourceId, parent, false);
            } else {
                view = convertView;
            }

            text = (TextView) view.findViewById(android.R.id.text1);

            OptionPage.Option item = getItem(position);
            text.setText(item.getTitle());

            return view;
        }

    }
}
