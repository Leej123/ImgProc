package com.vejoe.imgproc;


import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Color;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.SwitchPreference;
import android.support.v7.app.ActionBar;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.RingtonePreference;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.ContentFrameLayout;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.Toast;

import com.flask.colorpicker.ColorPickerPreference;
import com.vejoe.utils.Constants;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * A {@link PreferenceActivity} that presents a set of application settings. On
 * handset devices, settings are presented as a single list. On tablets,
 * settings are split by category, with category headers shown to the left of
 * the list of settings.
 * <p>
 * See <a href="http://developer.android.com/design/patterns/settings.html">
 * Android Design: Settings</a> for design guidelines and the <a
 * href="http://developer.android.com/guide/topics/ui/settings.html">Settings
 * API Guide</a> for more information on developing a Settings UI.
 */
public class SettingsActivity extends AppCompatPreferenceActivity {
    private static String minDistanceValue = "20";
    private static String maxDistanceValue = "80";
    private static PreferenceCategory detectCategory;
    private static PreferenceCategory noDetectCategory;
    private static Preference cameraSwitchFreqPrefs;
    private static Preference cameraSwitchPrefs;
//    private static ListPreference ringtoneListPrefs;
    /**
     * A preference value change listener that updates the preference's summary
     * to reflect its new value.
     */
    private static Preference.OnPreferenceChangeListener sBindPreferenceSummaryToValueListener = new Preference.OnPreferenceChangeListener() {
        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference instanceof SwitchPreference) {
               String key = preference.getKey();
                boolean booleanValue = (boolean) value;
                if (key.contains(Constants.KEY_SHOW_BOTH_CAMERA)) {
                    cameraSwitchPrefs.setEnabled(!booleanValue);
                    if (booleanValue) {
                        ((SwitchPreference) cameraSwitchPrefs).setChecked(false);
                        sBindPreferenceSummaryToValueListener.onPreferenceChange(cameraSwitchPrefs, false);
                    }
                } else if (key.contains(Constants.KEY_AUTO_SWITCH_CAMERA)) {
                    cameraSwitchFreqPrefs.setEnabled(booleanValue);
                } else if (key.contains(Constants.KEY_USE_SYSTEM_RINGTONE)) {
//                    ringtoneListPrefs.setEnabled(booleanValue);
                }
                return true;
            }

            String stringValue = value.toString();

            if (preference instanceof ListPreference) {
                // For list preferences, look up the correct display value in
                // the preference's 'entries' list.
                ListPreference listPreference = (ListPreference) preference;
                int index = listPreference.findIndexOfValue(stringValue);
//
//                String key = listPreference.getKey();
//                if (Constants.KEY_WARNING_TYPE.equals(key)) {
//                    if (index == 0) {
//                        detectCategory.setEnabled(true);
//                        noDetectCategory.setEnabled(false);
//                    } else {
//                        detectCategory.setEnabled(false);
//                        noDetectCategory.setEnabled(true);
//                    }
//                }
//
                // Set the summary to reflect the new value.
                preference.setSummary(
                        index >= 0
                                ? listPreference.getEntries()[index]
                                : null);

            } else if (preference instanceof MultiSelectListPreference) {
                MultiSelectListPreference msp = (MultiSelectListPreference) preference;
                Set<String> setValues = (Set<String>) value;

                if (preference.getKey().contains(Constants.KEY_WARNING_TYPE2)) {
                    boolean type1 = setValues.contains("0");
                    boolean type2 = setValues.contains("1");
                    detectCategory.setEnabled(type1);
                    noDetectCategory.setEnabled(type2);
                }

                StringBuilder stringBuilder = new StringBuilder();
                CharSequence[] entries = msp.getEntries();
                CharSequence[] values = msp.getEntryValues();

                for (int i = 0; i < entries.length; i ++) {
                    if (setValues.contains(values[i])) {
                        if (stringBuilder.length() > 0) {
                            stringBuilder.append(",");
                        }
                        stringBuilder.append(entries[i]);
                    }
                }
                preference.setSummary(stringBuilder.toString());

            } else if (preference instanceof RingtonePreference) {
                // For ringtone preferences, look up the correct display value
                // using RingtoneManager.
//                if (TextUtils.isEmpty(stringValue)) {
//                    // Empty values correspond to 'silent' (no ringtone).
//                    preference.setSummary(R.string.pref_ringtone_silent);
//
//                } else {
//                    Ringtone ringtone = RingtoneManager.getRingtone(
//                            preference.getContext(), Uri.parse(stringValue));
//
//                    if (ringtone == null) {
//                        // Clear the summary if there was a lookup error.
//                        preference.setSummary(null);
//                    } else {
//                        // Set the summary to reflect the new ringtone display
//                        // name.
//                        String name = ringtone.getTitle(preference.getContext());
//                        preference.setSummary(name);
//                    }
//                }

            } else if (preference instanceof EditTextPreference) {
                if (TextUtils.isEmpty(stringValue)) return false;
                String key = preference.getKey();
                if (Constants.KEY_MIN_DISTANCE.equals(key)) {
                    if (!TextUtils.isDigitsOnly(stringValue)) return false;
                    int setValue = Integer.parseInt(stringValue);
                    int maxValue = Integer.parseInt(maxDistanceValue);
                    if (setValue > maxValue) {
                        Toast.makeText(preference.getContext(), "最小距离不能超过最大距离", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    minDistanceValue = stringValue;
                } else if (Constants.KEY_MAX_DISTANCE.equals(key)) {
                    if (!TextUtils.isDigitsOnly(stringValue)) return false;
                    int setValue = Integer.parseInt(stringValue);
                    int minValue = Integer.parseInt(minDistanceValue);
                    if (setValue < minValue) {
                        Toast.makeText(preference.getContext(), "最大距离不能低于最小距离", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    maxDistanceValue = stringValue;
                } else if (Constants.KEY_WARNING_DISTANCE.equals(key)) {
                    if (!TextUtils.isDigitsOnly(stringValue)) return false;
                    int setValue = Integer.parseInt(stringValue);
                    int minValue = Integer.parseInt(minDistanceValue);
                    int maxValue = Integer.parseInt(maxDistanceValue);
                    if (setValue < minValue || setValue > maxValue) {
                        Toast.makeText(preference.getContext(), "警报距离不能超过设置范围", Toast.LENGTH_SHORT).show();
                        return false;
                    }
                }

                preference.setSummary(stringValue);
            }
            else {
                // For all other preferences, set the summary to the value's
                // simple string representation.
                preference.setSummary(stringValue);
            }
            return true;
        }
    };

    /**
     * Helper method to determine if the device has an extra-large screen. For
     * example, 10" tablets are extra-large.
     */
    private static boolean isXLargeTablet(Context context) {
        return (context.getResources().getConfiguration().screenLayout
                & Configuration.SCREENLAYOUT_SIZE_MASK) >= Configuration.SCREENLAYOUT_SIZE_XLARGE;
    }

    /**
     * Binds a preference's summary to its value. More specifically, when the
     * preference's value is changed, its summary (line of text below the
     * preference title) is updated to reflect the value. The summary is also
     * immediately updated upon calling this method. The exact display format is
     * dependent on the type of preference.
     *
     * @see #sBindPreferenceSummaryToValueListener
     */
    private static void bindPreferenceSummaryToValue(Preference preference) {
        // Set the listener to watch for value changes.
        preference.setOnPreferenceChangeListener(sBindPreferenceSummaryToValueListener);

        // Trigger the listener immediately with the preference's
        // current value.
        if (preference instanceof SwitchPreference) {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getBoolean(preference.getKey(), true));
        } else if (preference instanceof MultiSelectListPreference) {
            if (preference.getKey().contains(Constants.KEY_WARNING_TYPE2)) {
                Set<String> defaultValue = new HashSet<>();
                defaultValue.add("0");
                sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                        PreferenceManager
                                .getDefaultSharedPreferences(preference.getContext())
                                .getStringSet(preference.getKey(), defaultValue));
            }
        }  else {
            sBindPreferenceSummaryToValueListener.onPreferenceChange(preference,
                    PreferenceManager
                            .getDefaultSharedPreferences(preference.getContext())
                            .getString(preference.getKey(), ""));
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        String fragment = ParamsPreferenceFragment.class.getName();
//        Intent intent = getIntent();
//        intent.putExtra(EXTRA_SHOW_FRAGMENT, fragment);
//        intent.putExtra(EXTRA_NO_HEADERS, true);
        super.onCreate(savedInstanceState);
        setupActionBar();
        View decorView = getWindow().getDecorView();
        ContentFrameLayout contentView = (ContentFrameLayout) decorView.findViewById(android.R.id.content);
        contentView.setBackgroundColor(Color.parseColor("#FF334154"));
        findViewById(android.R.id.list).setBackgroundColor(Color.parseColor("#FF334154"));
    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    /**
     * Set up the {@link android.app.ActionBar}, if the API is available.
     */
    private void setupActionBar() {
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            // Show the Up button in the action bar.
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean onIsMultiPane() {
        return isXLargeTablet(this);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    @TargetApi(Build.VERSION_CODES.HONEYCOMB)
    public void onBuildHeaders(List<Header> target) {
        loadHeadersFromResource(R.xml.pref_headers, target);
    }

    /**
     * This method stops fragment injection in malicious applications.
     * Make sure to deny any unknown fragments here.
     */
    protected boolean isValidFragment(String fragmentName) {
        return PreferenceFragment.class.getName().equals(fragmentName)
                || ParamsPreferenceFragment.class.getName().equals(fragmentName)
                || SwitchCameraPreferenceFragment.class.getName().equals(fragmentName)
                || MaskLinePreferenceFragment.class.getName().equals(fragmentName);
    }

    private final static HashMap<String, String> ringtoneMap = new HashMap<>();
    public static HashMap<String, String> getSystemRingtoneList(Context context) {
        if (!ringtoneMap.isEmpty())
            return ringtoneMap;

        RingtoneManager manager = new RingtoneManager(context);
        manager.setType(RingtoneManager.TYPE_RINGTONE);
        Cursor cursor = manager.getCursor();

        boolean suc = cursor.moveToFirst();
        while (suc) {
            String name = cursor.getString(RingtoneManager.TITLE_COLUMN_INDEX);
            String uriStr = cursor.getString(RingtoneManager.URI_COLUMN_INDEX);
            long id = cursor.getLong(RingtoneManager.ID_COLUMN_INDEX);
            Uri uri = ContentUris.withAppendedId(Uri.parse(uriStr), id);
            ringtoneMap.put(name, uri.toString());

            suc = cursor.moveToNext();
        }

        return ringtoneMap;
    }

    public static class ParamsPreferenceFragment extends  PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_params);
            setHasOptionsMenu(true);

            SharedPreferences prefs =  PreferenceManager.getDefaultSharedPreferences(this.getActivity());
            minDistanceValue = prefs.getString(Constants.KEY_MIN_DISTANCE, Constants.MIN_DISTANCE_DEFAULT_VALUE);
            maxDistanceValue = prefs.getString(Constants.KEY_MAX_DISTANCE, Constants.MAX_DISTANCE_DEFAULT_VALUE);

            detectCategory = (PreferenceCategory) findPreference(Constants.KEY_WARNING_TYPE_DETECT);
            noDetectCategory = (PreferenceCategory) findPreference(Constants.KEY_WARNING_TYPE_NO_DETECT);
//            ringtoneListPrefs = (ListPreference) findPreference(Constants.KEY_RINGTONE);
//            HashMap<String, String> ringtoneMap = getSystemRingtoneList(getActivity());
//            if (!ringtoneMap.isEmpty()) {
//                CharSequence[] entries = new CharSequence[ringtoneMap.size()];
//                ringtoneMap.keySet().toArray(entries);
//                ringtoneListPrefs.setEntries(entries);
//                ringtoneListPrefs.setEntryValues(entries);
//            }

            bindPreferenceSummaryToValue(findPreference(Constants.KEY_WARNING_TYPE2));
            bindPreferenceSummaryToValue(findPreference(Constants.KEY_MIN_DISTANCE));
            bindPreferenceSummaryToValue(findPreference(Constants.KEY_MAX_DISTANCE));
            bindPreferenceSummaryToValue(findPreference(Constants.KEY_DETECT_DURATION));
            bindPreferenceSummaryToValue(findPreference(Constants.KEY_NO_DETECT_DURATION));
            bindPreferenceSummaryToValue(findPreference(Constants.KEY_USE_SYSTEM_RINGTONE));
//            bindPreferenceSummaryToValue(ringtoneListPrefs);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public static class SwitchCameraPreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_switch_camera);
            setHasOptionsMenu(true);

            cameraSwitchFreqPrefs = findPreference(Constants.KEY_AUTO_SWITCH_CAMERA_FREQUENCY);
            cameraSwitchPrefs = findPreference(Constants.KEY_AUTO_SWITCH_CAMERA);
            bindPreferenceSummaryToValue(findPreference(Constants.KEY_SHOW_BOTH_CAMERA));
            bindPreferenceSummaryToValue(cameraSwitchPrefs);
            bindPreferenceSummaryToValue(cameraSwitchFreqPrefs);
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }

    public static class MaskLinePreferenceFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_mask_line);
            setHasOptionsMenu(true);

            Preference prefs = findPreference(Constants.KEY_CALIBRATION_LINE);
            prefs.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    Intent intent = new Intent(getActivity(), CameraActivity.class);
                    intent.putExtra(Constants.KEY_CALIBRATION_LINE, true);
                    startActivity(intent);
                    return true;
                }
            });

            findPreference(Constants.KEY_CLEAR_CALIBRATION_LINE).setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference preference) {
                    SharedPreferences prefs = ImgProcApp.getAppSharedPreferences();
                    SharedPreferences.Editor editor = prefs.edit();
                    editor.putString(Constants.KEY_LINE_ONE_POINTS, "");
                    editor.putString(Constants.KEY_LINE_TWO_POINTS, "");
                    editor.apply();
                    preference.setEnabled(false);
                    return true;
                }
            });

            bindPreferenceSummaryToValue(findPreference(Constants.KEY_LINE_WIDTH));
        }

        @Override
        public void onResume() {
            super.onResume();
            SharedPreferences sp = ImgProcApp.getAppSharedPreferences();
            String lineOne = sp.getString(Constants.KEY_LINE_ONE_POINTS, "");
            String lineTwo = sp.getString(Constants.KEY_LINE_TWO_POINTS, "");
            if (TextUtils.isEmpty(lineOne) && TextUtils.isEmpty(lineTwo)) {
                findPreference(Constants.KEY_CLEAR_CALIBRATION_LINE).setEnabled(false);
            } else {
                findPreference(Constants.KEY_CLEAR_CALIBRATION_LINE).setEnabled(true);
            }
        }

        @Override
        public boolean onOptionsItemSelected(MenuItem item) {
            int id = item.getItemId();
            if (id == android.R.id.home) {
                startActivity(new Intent(getActivity(), SettingsActivity.class));
                return true;
            }
            return super.onOptionsItemSelected(item);
        }
    }
}
