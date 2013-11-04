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

import com.cyanogenmod.account.R;
import com.cyanogenmod.account.ui.SetupPageFragment;

import org.xmlpull.v1.XmlPullParserException;

import android.app.Activity;
import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.app.Fragment;
import android.app.TimePickerDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.XmlResourceParser;
import android.os.Bundle;
import android.os.Handler;
import android.text.format.DateFormat;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.DatePicker;
import android.widget.SimpleAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimeZone;

public class DateTimePage extends Page {

    private static final String TAG = DateTimePage.class.getSimpleName();

    private static final String KEY_ID = "id";  // value: String
    private static final String KEY_DISPLAYNAME = "name";  // value: String
    private static final String KEY_GMT = "gmt";  // value: String
    private static final String KEY_OFFSET = "offset";  // value: int (Integer)
    private static final String XMLTAG_TIMEZONE = "timezone";

    private static final int HOURS_1 = 60 * 60000;


    public DateTimePage(Context context, SetupDataCallbacks callbacks, int titleResourceId) {
        super(context, callbacks, titleResourceId);
    }

    @Override
    public Fragment createFragment() {
        Bundle args = new Bundle();
        args.putString(Page.KEY_PAGE_ARGUMENT, getKey());

        DateTimeFragment fragment = new DateTimeFragment();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public int getNextButtonResId() {
        return R.string.next;
    }

    public static class DateTimeFragment extends SetupPageFragment implements TimePickerDialog.OnTimeSetListener, DatePickerDialog.OnDateSetListener {

        private TimeZone mCurrentTimeZone;
        private View mDateView;
        private View mTimeView;
        private TextView mDateTextView;
        private TextView mTimeTextView;


        private final Handler mHandler = new Handler();

        @Override
        public void onResume() {
            super.onResume();
            // Register for time ticks and other reasons for time change
            IntentFilter filter = new IntentFilter();
            filter.addAction(Intent.ACTION_TIME_TICK);
            filter.addAction(Intent.ACTION_TIME_CHANGED);
            filter.addAction(Intent.ACTION_TIMEZONE_CHANGED);
            getActivity().registerReceiver(mIntentReceiver, filter, null, null);

            updateTimeAndDateDisplay(getActivity());
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivity().unregisterReceiver(mIntentReceiver);
        }

        @Override
        protected void setUpPage() {
            final Spinner spinner = (Spinner) mRootView.findViewById(R.id.timezone_list);
            final SimpleAdapter adapter = constructTimezoneAdapter(getActivity(), false);
            mCurrentTimeZone = TimeZone.getDefault();
            mDateView = mRootView.findViewById(R.id.date_item);
            mDateView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showDatePicker();
                }
            });
            mTimeView = mRootView.findViewById(R.id.time_item);
            mTimeView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    showTimePicker();
                }
            });
            mDateTextView = (TextView)mRootView.findViewById(R.id.date_text);
            mTimeTextView = (TextView)mRootView.findViewById(R.id.time_text);
            // Pre-select current/default timezone
            mHandler.post(new Runnable() {
                @Override
                public void run() {
                    int tzIndex = getTimeZoneIndex(adapter, mCurrentTimeZone);
                    spinner.setAdapter(adapter);
                    if (tzIndex != -1) {
                        spinner.setSelection(tzIndex);
                    }
                    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
                        @Override
                        public void onItemSelected(AdapterView<?> adapterView, View view, int position, long id) {
                            final Map<?, ?> map = (Map<?, ?>) adapterView.getItemAtPosition(position);
                            final String tzId = (String) map.get(KEY_ID);
                            if (mCurrentTimeZone != null && !mCurrentTimeZone.getID().equals(tzId)) {
                                // Update the system timezone value
                                final Activity activity = getActivity();
                                final AlarmManager alarm = (AlarmManager) activity.getSystemService(Context.ALARM_SERVICE);
                                alarm.setTimeZone(tzId);
                                mCurrentTimeZone = TimeZone.getTimeZone(tzId);
                            }

                        }

                        @Override
                        public void onNothingSelected(AdapterView<?> adapterView) {
                        }
                    });
                }
            });
        }

        private void showDatePicker() {
            DatePickerFragment datePickerFragment = DatePickerFragment.newInstance();
            datePickerFragment.setOnDateSetListener(this);
            datePickerFragment.show(getFragmentManager(), DatePickerFragment.TAG);
        }

        private void showTimePicker() {
            TimePickerFragment timePickerFragment = TimePickerFragment.newInstance();
            timePickerFragment.setOnTimeSetListener(this);
            timePickerFragment.show(getFragmentManager(), TimePickerFragment.TAG);
        }

        public void updateTimeAndDateDisplay(Context context) {
            java.text.DateFormat shortDateFormat = DateFormat.getDateFormat(context);
            final Calendar now = Calendar.getInstance();
            mTimeTextView.setText(DateFormat.getTimeFormat(getActivity()).format(now.getTime()));
            mDateTextView.setText(shortDateFormat.format(now.getTime()));
        }

        @Override
        protected int getLayoutResource() {
            return R.layout.setup_datetime_page;
        }

        @Override
        protected int getTitleResource() {
            return R.string.setup_datetime;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            final Activity activity = getActivity();
            if (activity != null) {
                setDate(activity, year, month, day);
                updateTimeAndDateDisplay(activity);
            }
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            final Activity activity = getActivity();
            if (activity != null) {
                setTime(activity, hourOfDay, minute);
                updateTimeAndDateDisplay(activity);
            }
        }

        private BroadcastReceiver mIntentReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                final Activity activity = getActivity();
                if (activity != null) {
                    updateTimeAndDateDisplay(activity);
                }
            }
        };

    }

    private static SimpleAdapter constructTimezoneAdapter(Context context,
            boolean sortedByName) {
        final String[] from = new String[] {KEY_DISPLAYNAME, KEY_GMT};
        final int[] to = new int[] {android.R.id.text1, android.R.id.text2};

        final String sortKey = (sortedByName ? KEY_DISPLAYNAME : KEY_OFFSET);
        final TimeZoneComparator comparator = new TimeZoneComparator(sortKey);
        final List<HashMap<String, Object>> sortedList = getZones(context);
        Collections.sort(sortedList, comparator);
        final SimpleAdapter adapter = new SimpleAdapter(context,
                sortedList,
                R.layout.date_time_setup_custom_list_item_2,
                from,
                to);

        return adapter;
    }

    private static List<HashMap<String, Object>> getZones(Context context) {
        final List<HashMap<String, Object>> myData = new ArrayList<HashMap<String, Object>>();
        final long date = Calendar.getInstance().getTimeInMillis();
        try {
            XmlResourceParser xrp = context.getResources().getXml(R.xml.timezones);
            while (xrp.next() != XmlResourceParser.START_TAG)
                continue;
            xrp.next();
            while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                while (xrp.getEventType() != XmlResourceParser.START_TAG) {
                    if (xrp.getEventType() == XmlResourceParser.END_DOCUMENT) {
                        return myData;
                    }
                    xrp.next();
                }
                if (xrp.getName().equals(XMLTAG_TIMEZONE)) {
                    String id = xrp.getAttributeValue(0);
                    String displayName = xrp.nextText();
                    addItem(myData, id, displayName, date);
                }
                while (xrp.getEventType() != XmlResourceParser.END_TAG) {
                    xrp.next();
                }
                xrp.next();
            }
            xrp.close();
        } catch (XmlPullParserException xppe) {
            Log.e(TAG, "Ill-formatted timezones.xml file");
        } catch (java.io.IOException ioe) {
            Log.e(TAG, "Unable to read timezones.xml file");
        }

        return myData;
    }

    private static void addItem(
            List<HashMap<String, Object>> myData, String id, String displayName, long date) {
        final HashMap<String, Object> map = new HashMap<String, Object>();
        map.put(KEY_ID, id);
        map.put(KEY_DISPLAYNAME, displayName);
        final TimeZone tz = TimeZone.getTimeZone(id);
        final int offset = tz.getOffset(date);
        final int p = Math.abs(offset);
        final StringBuilder name = new StringBuilder();
        name.append("GMT");

        if (offset < 0) {
            name.append('-');
        } else {
            name.append('+');
        }

        name.append(p / (HOURS_1));
        name.append(':');

        int min = p / 60000;
        min %= 60;

        if (min < 10) {
            name.append('0');
        }
        name.append(min);

        map.put(KEY_GMT, name.toString());
        map.put(KEY_OFFSET, offset);

        myData.add(map);
    }

    private static int getTimeZoneIndex(SimpleAdapter adapter, TimeZone tz) {
        final String defaultId = tz.getID();
        final int listSize = adapter.getCount();
        for (int i = 0; i < listSize; i++) {
            // Using HashMap<String, Object> induces unnecessary warning.
            final HashMap<?,?> map = (HashMap<?,?>)adapter.getItem(i);
            final String id = (String)map.get(KEY_ID);
            if (defaultId.equals(id)) {
                // If current timezone is in this list, move focus to it
                return i;
            }
        }
        return -1;
    }

    private static void setDate(Context context, int year, int month, int day) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.YEAR, year);
        c.set(Calendar.MONTH, month);
        c.set(Calendar.DAY_OF_MONTH, day);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTime(when);
        }
    }

    private static void setTime(Context context, int hourOfDay, int minute) {
        Calendar c = Calendar.getInstance();

        c.set(Calendar.HOUR_OF_DAY, hourOfDay);
        c.set(Calendar.MINUTE, minute);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);
        long when = c.getTimeInMillis();

        if (when / 1000 < Integer.MAX_VALUE) {
            ((AlarmManager) context.getSystemService(Context.ALARM_SERVICE)).setTime(when);
        }
    }

    private static class TimeZoneComparator implements Comparator<HashMap<?, ?>> {
        private String mSortingKey;

        public TimeZoneComparator(String sortingKey) {
            mSortingKey = sortingKey;
        }

        public void setSortingKey(String sortingKey) {
            mSortingKey = sortingKey;
        }

        public int compare(HashMap<?, ?> map1, HashMap<?, ?> map2) {
            Object value1 = map1.get(mSortingKey);
            Object value2 = map2.get(mSortingKey);

            /*
             * This should never happen, but just in-case, put non-comparable
             * items at the end.
             */
            if (!isComparable(value1)) {
                return isComparable(value2) ? 1 : 0;
            } else if (!isComparable(value2)) {
                return -1;
            }

            return ((Comparable) value1).compareTo(value2);
        }

        private boolean isComparable(Object value) {
            return (value != null) && (value instanceof Comparable);
        }
    }

    private static class TimePickerFragment extends DialogFragment implements TimePickerDialog.OnTimeSetListener {

        private static String TAG = TimePickerFragment.class.getSimpleName();

        private TimePickerDialog.OnTimeSetListener mOnTimeSetListener;

        public static TimePickerFragment newInstance() {
            TimePickerFragment frag = new TimePickerFragment();
            return frag;
        }

        private void setOnTimeSetListener(TimePickerDialog.OnTimeSetListener onTimeSetListener) {
            mOnTimeSetListener = onTimeSetListener;
        }

        @Override
        public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
            if (mOnTimeSetListener != null) {
                mOnTimeSetListener.onTimeSet(view, hourOfDay, minute);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar calendar = Calendar.getInstance();
            return new TimePickerDialog(
                    getActivity(),
                    this,
                    calendar.get(Calendar.HOUR_OF_DAY),
                    calendar.get(Calendar.MINUTE),
                    DateFormat.is24HourFormat(getActivity()));

        }
    }

    private static class DatePickerFragment extends DialogFragment implements DatePickerDialog.OnDateSetListener {

        private static String TAG = DatePickerFragment.class.getSimpleName();

        private DatePickerDialog.OnDateSetListener mOnDateSetListener;

        public static DatePickerFragment newInstance() {
            DatePickerFragment frag = new DatePickerFragment();
            return frag;
        }

        private void setOnDateSetListener(DatePickerDialog.OnDateSetListener onDateSetListener) {
            mOnDateSetListener = onDateSetListener;
        }

        @Override
        public void onDateSet(DatePicker view, int year, int month, int day) {
            if (mOnDateSetListener != null) {
                mOnDateSetListener.onDateSet(view, year, month, day);
            }
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Calendar calendar = Calendar.getInstance();
            return new DatePickerDialog(
                    getActivity(),
                    this,
                    calendar.get(Calendar.YEAR),
                    calendar.get(Calendar.MONTH),
                    calendar.get(Calendar.DAY_OF_MONTH));

        }
    }

}
