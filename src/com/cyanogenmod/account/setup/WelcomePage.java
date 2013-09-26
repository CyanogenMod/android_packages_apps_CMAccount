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

import com.android.internal.app.LocalePicker;
import com.cyanogenmod.account.R;
import com.cyanogenmod.account.ui.SetupPageFragment;
import com.cyanogenmod.account.ui.SetupWizardActivity;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import java.util.Locale;

public class WelcomePage extends Page {

    public WelcomePage(Context context, SetupDataCallbacks callbacks, int titleResourceId) {
        super(context, callbacks, titleResourceId);
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
            final Spinner spinner = (Spinner) mRootView.findViewById(R.id.locale_list);
            final ArrayAdapter<LocalePicker.LocaleInfo> adapter = LocalePicker.constructAdapter(getActivity(), R.layout.locale_picker_item, R.id.locale);
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
            return R.layout.setup_welcome_page;
        }

    }

}
