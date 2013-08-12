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

