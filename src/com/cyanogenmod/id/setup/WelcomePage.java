package com.cyanogenmod.id.setup;


import com.android.internal.app.LocalePicker;
import com.cyanogenmod.id.R;
import com.cyanogenmod.id.ui.SetupPageFragment;
import com.cyanogenmod.id.ui.SetupWizardActivity;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;

import java.util.Locale;

public class WelcomePage extends Page {

    public WelcomePage(Context context, SetupDataCallbacks callbacks, int titleResourceId, int imageResourceId) {
        super(context, callbacks, titleResourceId, imageResourceId);
    }

    @Override
    public Fragment createFragment() {
        Bundle args = new Bundle();
        args.putString(Page.KEY_PAGE_ARGUMENT, getKey());

        WelcomeFragment fragment = new WelcomeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getNextButtonResId() {
        return R.string.next;
    }

    public static class WelcomeFragment extends SetupPageFragment {

        @Override
        protected void setUpPage() {
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
        protected int getLayoutResource() {
            return R.layout.fragment_welcome_page;
        }

    }

}
