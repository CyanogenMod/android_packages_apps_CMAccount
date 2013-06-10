package com.cyanogenmod.id.ui;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import com.android.internal.app.LocalePicker;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.setup.Page;

import java.util.Locale;

public class WelcomeFragment extends Fragment {

    protected SetupWizardActivity mActivity;
    protected String mKey;
    protected Page mPage;

    private View mRootView;

    public static WelcomeFragment create(String key) {
        Bundle args = new Bundle();
        args.putString(Page.KEY_PAGE_ARGUMENT, key);

        WelcomeFragment fragment = new WelcomeFragment();
        fragment.setArguments(args);
        return fragment;
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
        mPage = mActivity.getPage(mKey);
        ((TextView) mRootView.findViewById(android.R.id.title)).setText(mPage.getTitle());
        int imageId = mPage.getImageResourceId();
        if (imageId != -1) {
            ((ImageView) mRootView.findViewById(R.id.setup_img)).setImageResource(imageId);
        }
        final Spinner spinner = (Spinner) mRootView.findViewById(R.id.locale_list);
        final ArrayAdapter<LocalePicker.LocaleInfo> adapter = LocalePicker.constructAdapter(getActivity(), android.R.layout.simple_list_item_1, android.R.id.text1);
        spinner.setAdapter(adapter);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Locale locale = adapter.getItem(i).getLocale();
                LocalePicker.updateLocale(locale);
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {
            }
        });

        // Pre-select current locale
        new Handler().post(new Runnable() {
            @Override
            public void run() {
                int count = adapter.getCount();
                Locale current = Locale.getDefault();
                for (int i=0; i<count; i++) {
                    Locale locale = adapter.getItem(i).getLocale();
                    if (current.equals(locale)) {
                        spinner.setSelection(i);
                        break;
                    }
                }
                spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                    @Override
                    public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                        Locale locale = adapter.getItem(i).getLocale();
                        LocalePicker.updateLocale(locale);
                    }

                    @Override
                    public void onNothingSelected(AdapterView<?> adapterView) {
                    }
                });
            }
        });
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_welcome_page, container, false);


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

}
