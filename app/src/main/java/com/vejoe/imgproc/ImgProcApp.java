package com.vejoe.imgproc;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.vejoe.utils.Tools;

import java.io.File;

/**
 * Created by Administrator on 2017/6/14 0014.
 */

public class ImgProcApp extends Application {
    public static final String APP_PREF_NAME = "params_prefs";
    public static final String HAAR_FILENAME = "haar.xml";
    private static SharedPreferences preferences;
    @Override
    public void onCreate() {
        super.onCreate();
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }

    public static String getHaarFilePath() {
        String workDir = getWorkDirectory();
        return workDir + "/" + HAAR_FILENAME;
    }

    public static String getWorkDirectory() {
        File dir = Tools.createDirectory("vejoe");
        return dir == null? null : dir.getAbsolutePath();
    }

    public static SharedPreferences getAppSharedPreferences() {
        return preferences;
    }
}
