package com.cyanogenmod.id.ui;


import com.cyanogenmod.id.R;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

public class FinishFragment extends Fragment {

    public FinishFragment() {
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.setup_fragment_page, container, false);

        TextView titleView = (TextView) rootView.findViewById(android.R.id.title);
        titleView.setText(R.string.setup_complete);

        return rootView;
    }

}
