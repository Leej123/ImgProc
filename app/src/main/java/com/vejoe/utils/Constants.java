package com.vejoe.utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Administrator on 2017/6/16 0016.
 */

public class Constants {
    public static final String KEY_MIN_DISTANCE = "min_distance";
    public static final String KEY_MAX_DISTANCE = "max_distance";
    public static final String KEY_WARNING_TYPE = "warning_type";
    public static final String KEY_WARNING_TYPE2 = "warning_type2";
    public static final String KEY_DETECT_DURATION = "detect_duration";
    public static final String KEY_NO_DETECT_DURATION = "no_detect_duration";
    public static final String KEY_WARNING_TYPE_DETECT = "type_detect";
    public static final String KEY_WARNING_TYPE_NO_DETECT = "type_no_detect";
    public static final String KEY_USE_SYSTEM_RINGTONE = "use_system_ringtone";
    public static final String KEY_RINGTONE = "ringtone";

    public static final String KEY_WARNING_DISTANCE = "waring_distance";
    public static final String KEY_AUTO_SWITCH_CAMERA = "auto_switch_camera";
    public static final String KEY_AUTO_SWITCH_CAMERA_FREQUENCY = "switch_camera_frequency";
    public static final String KEY_CALIBRATION_LINE = "calibration_line";
    public static final String KEY_CLEAR_CALIBRATION_LINE = "clear_calibration_line";
    public static final String KEY_LINE_COLOR = "line_color";
    public static final String KEY_LINE_WIDTH = "line_width";
    public static final String KEY_LINE_ONE_POINTS = "line_one_points";
    public static final String KEY_LINE_TWO_POINTS = "line_two_points";
    public static final String KEY_SHOW_BOTH_CAMERA = "show_both_camera";
    public static final String KEY_CALIBRATION_VIEW_WIDTH = "calibration_view_width";
    public static final String KEY_CALIBRATION_VIEW_HEIGHT = "calibration_view_height";

    public static final String MIN_DISTANCE_DEFAULT_VALUE = "50";
    public static final String MAX_DISTANCE_DEFAULT_VALUE = "80";
    public static final String DURATION_DEFAULT_VALUE = "5";//单位秒
    public static final String WARNING_TYPE_DEFAULT_VALUE = "0";
    public static final String CAMERA_SWITCH_FREQUENCY_DEFAULT_VALUE = "5";
    public static final String LINE_WIDTH_DEFAULT_VALUE = "8";
    public static final Set<String> WARNING_TYPE_DEFAULT_SET = new HashSet<>();
    static {
        WARNING_TYPE_DEFAULT_SET.add("0");
    }
}
