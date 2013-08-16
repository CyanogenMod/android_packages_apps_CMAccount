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

package com.cyanogenmod.id.ui;

import com.cyanogenmod.id.R;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.webkit.WebView;

public class WebViewDialogFragment extends DialogFragment {

        public static String TAG = WebViewDialogFragment.class.getSimpleName();

        private String mUri;

        public static WebViewDialogFragment newInstance() {
            return new WebViewDialogFragment();
        }

        public WebViewDialogFragment setUri(String uri) {
            mUri = uri;
            return this;
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            WebView webView = (WebView)getActivity().getLayoutInflater().inflate(R.layout.terms_webview, null, false);
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setUseWideViewPort(true);
            webView.loadUrl(mUri);
            return new AlertDialog.Builder(getActivity())
                    .setView(webView)
                    .setNeutralButton(R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            dialogInterface.dismiss();
                        }
                    })
                    .create();
        }
    }

