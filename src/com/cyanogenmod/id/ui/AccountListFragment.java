package com.cyanogenmod.id.ui;

import com.cyanogenmod.id.Constants;
import com.cyanogenmod.id.R;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;


public class AccountListFragment extends ListFragment {

    private AccountSettingsActivity mActivity;
    private View mRootView;
    private AccountAdapter mAccountAdapter;

    private AccountManager mAccountManager;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAccountManager = AccountManager.get(getActivity());
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Account[] accounts = mAccountManager.getAccountsByType(Constants.ACCOUNT_TYPE);
        ListView listView = (ListView)mRootView.findViewById(android.R.id.list);
        mAccountAdapter = new AccountAdapter(mActivity, accounts);
        listView.setAdapter(mAccountAdapter);
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (!(activity instanceof AccountSettingsActivity)) {
            throw new ClassCastException("Activity must be AccountSettingsActivity");
        }
        mActivity = (AccountSettingsActivity) activity;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        mActivity = null;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        mRootView = inflater.inflate(R.layout.fragment_account_list, container, false);
        return mRootView;
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        mActivity.onAccountSelected(mAccountAdapter.getItem(position));
    }

    private class AccountAdapter extends ArrayAdapter<Account> {

        private LayoutInflater mInflater;

        private AccountAdapter(Context context, Account[] accounts) {
            super(context, 0, accounts);
            mInflater = LayoutInflater.from(context);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            View view;

            if (convertView == null) {
                view = mInflater.inflate(android.R.layout.simple_list_item_1, parent, false);
            } else {
                view = convertView;
            }

            TextView accountName = (TextView) view.findViewById(android.R.id.text1);
            Account account = getItem(position);
            accountName.setText(account.name);

            return view;
        }
    }

}