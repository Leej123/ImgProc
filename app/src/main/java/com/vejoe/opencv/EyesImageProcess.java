package com.vejoe.opencv;

import android.content.SharedPreferences;

import com.vejoe.imgproc.ImgProcApp;
import com.vejoe.utils.Constants;
import com.vejoe.utils.Tools;

import java.io.File;
import java.io.IOException;
import java.util.Set;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * 检测瞳孔距离
 * Created by Administrator on 2017/6/14 0014.
 */

public class EyesImageProcess {
    private static final int FREE_VALID_COUNT = 5;
    private static final int EXISTS_VALID_COUNT = 5;
    private OpenCV openCV;
    private Thread processThread = null;
    private boolean isEnd = true;

    private Object obj = new Object();
    private boolean newFrame = false;
    private byte[] data = null;
    private byte[] buffer = null;
    private int width;
    private int height;
    private int cameraType = 0;
    private float focalLength = 4.26f;

    private int freeCount = 0;
    private int existCount = 0;
    private boolean isExists = false;
    private int lengthSum = 0;
    private int lengthCount = 0;
    private long timeDetect = 0;
    private long freeTimeDetect = 0;
    private int minDistance = 50;
    private int maxDistance = 80;

    private boolean inScopeFlag = false;
    private int durationForDetect = 5000;
    private int durationForNoDetect = 5000;
    private boolean warningInDetection = true;
    private boolean warningNoDetection = false;

    private ProcessCallback callback;

    //日志
    private Logger validLogger;
    private Logger noneLogger;

    private boolean needRotation = true;

    public EyesImageProcess() {
        openCV = new OpenCV();
//        initLogger();
        SharedPreferences prefs = ImgProcApp.getAppSharedPreferences();
        // 设置的距离区间
        minDistance = Integer.parseInt(prefs.getString(Constants.KEY_MIN_DISTANCE, Constants.MIN_DISTANCE_DEFAULT_VALUE));
        maxDistance = Integer.parseInt(prefs.getString(Constants.KEY_MAX_DISTANCE, Constants.MAX_DISTANCE_DEFAULT_VALUE));

        // 检测到瞳孔的持续时间
        durationForDetect = Integer.parseInt(prefs.getString(Constants.KEY_DETECT_DURATION, Constants.DURATION_DEFAULT_VALUE));
        durationForDetect *= 1000;//ms

        // 没有检测到瞳孔的持续时间
        durationForNoDetect = Integer.parseInt(prefs.getString(Constants.KEY_NO_DETECT_DURATION, Constants.DURATION_DEFAULT_VALUE));
        durationForNoDetect *= 1000;//ms

//        int index = Integer.parseInt(prefs.getString(Constants.KEY_WARNING_TYPE, Constants.WARNING_TYPE_DEFAULT_VALUE));
//        warningInDetection = (index == 0);
        Set<String> values = prefs.getStringSet(Constants.KEY_WARNING_TYPE2, Constants.WARNING_TYPE_DEFAULT_SET);
        if (values.contains("0")) {
            warningInDetection = true;
        } else {
            warningInDetection = false;
        }
        if (values.contains("1")) {
            warningNoDetection = true;
        } else {
            warningNoDetection = false;
        }
    }

    public void updateParams() {
        SharedPreferences prefs = ImgProcApp.getAppSharedPreferences();
        minDistance = Integer.parseInt(prefs.getString(Constants.KEY_MIN_DISTANCE, Constants.MIN_DISTANCE_DEFAULT_VALUE));
        maxDistance = Integer.parseInt(prefs.getString(Constants.KEY_MAX_DISTANCE, Constants.MAX_DISTANCE_DEFAULT_VALUE));

        durationForDetect = Integer.parseInt(prefs.getString(Constants.KEY_DETECT_DURATION, Constants.DURATION_DEFAULT_VALUE));
        durationForDetect *= 1000;//ms

        durationForNoDetect = Integer.parseInt(prefs.getString(Constants.KEY_NO_DETECT_DURATION, Constants.DURATION_DEFAULT_VALUE));
        durationForNoDetect *= 1000;//ms

//        int index = Integer.parseInt(prefs.getString(Constants.KEY_WARNING_TYPE, Constants.WARNING_TYPE_DEFAULT_VALUE));
//        warningInDetection = (index == 0);

        Set<String> values = prefs.getStringSet(Constants.KEY_WARNING_TYPE2, Constants.WARNING_TYPE_DEFAULT_SET);
        if (values.contains("0")) {
            warningInDetection = true;
        } else {
            warningInDetection = false;
        }
        if (values.contains("1")) {
            warningNoDetection = true;
        } else {
            warningNoDetection = false;
        }
    }

    public void setProcessCallback(ProcessCallback callback) {
        this.callback = callback;
    }

    /**
     * 处理前置摄像头的一帧数据
     * @param data 前置摄像头数据
     * @param width 图片的宽度
     * @param height 图片的高度
     * @param cameraType 摄像头类型 前置or后置。这里只用前置。
     * @param focalLength 摄像头的焦距
     * @param needRotation 是否需要选择图片来适应处理
     */
    public void processFrame(byte[] data, int width, int height, int cameraType, float focalLength, boolean needRotation) {
        synchronized (obj) {
            if (buffer == null || buffer.length != data.length) {
                buffer = null;
                buffer = new byte[data.length];
            }
            System.arraycopy(data, 0, buffer, 0, data.length);
            this.width = width;
            this.height = height;
            this.cameraType = cameraType;
            this.focalLength = focalLength;
            newFrame = true;
            this.needRotation = needRotation;
        }
    }

    public boolean isStart() {
        return processThread != null;
    }

    public void start() {
        if (processThread != null) return;
        resetParams();
        isEnd = false;
        processThread = new Thread(new Runnable() {
            @Override
            public void run() {
                process();
            }
        });
        processThread.setDaemon(true);
        processThread.start();
    }

    public void stop() {
        isEnd = true;
        if (processThread != null) {
            processThread.interrupt();
            try {
                processThread.join(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            processThread = null;
        }
    }

    private void resetParams() {
        freeCount = 0;
        existCount = 0;
        isExists = false;
        lengthSum = 0;
        lengthCount = 0;
        timeDetect = 0;
        freeTimeDetect = 0;
        inScopeFlag = false;
    }

    private void process() {
        boolean handle;
        freeTimeDetect = System.currentTimeMillis();//开始的时间
        while (!isEnd && !processThread.isInterrupted()) {
            handle = false;
            synchronized (obj) {
                if (buffer != null && newFrame) {
                    if (data == null  || data.length != buffer.length) {
                        data = null;
                        data = new byte[buffer.length];
                    }
                    System.arraycopy(buffer, 0, data, 0, buffer.length);
                    newFrame = false;
                    handle = true;
                }
            }

            if (handle) {
                // 调用底层opencv检测瞳孔距离。
                float distance = openCV.detectEyesDistance(data, width, height, cameraType, focalLength, needRotation);
                //detectDistanceAndLog(distance);
                detectDistance(distance);
            }
        }
    }

    /**
     * 根据检测到的距离和设置条件，来确定是否发出警报
     * @param distance
     */
    private void detectDistance(float distance) {
        if (distance < 0) {
            freeCount ++;
            if (freeCount >= 35500) freeCount = FREE_VALID_COUNT;
            if (freeCount >= FREE_VALID_COUNT) {
                if (isExists) {
                    lengthSum = 0;
                    lengthCount = 0;
                    freeTimeDetect = System.currentTimeMillis();//未检测到的开始时间
                }

                // 当未检测到时，判断是否超过设定的时间。如果超过则发出警报
                if (warningNoDetection) {
                    long duration = System.currentTimeMillis() - freeTimeDetect;
                    if (duration >= durationForNoDetect) {
                        if (callback != null) {
                            callback.onWarning();
                        }
                        freeTimeDetect = System.currentTimeMillis();
                    }
                }

                isExists = false;
                existCount = 0;
            }
        } else {
            existCount ++;
            lengthSum += distance;
            lengthCount ++;
            if (existCount >= 35500) existCount = EXISTS_VALID_COUNT;


            if (existCount >= EXISTS_VALID_COUNT) {
                if (!isExists) {
                    lengthSum = 0;
                    lengthCount = 0;
                    lengthSum += distance;
                    lengthCount ++;
                }

                // 检测到通过报警
                if (warningInDetection) {
                    float length = (float) lengthSum / lengthCount;
                    // 是否处于设定的距离内
                    if (length < minDistance || length > maxDistance) {
                        if (!inScopeFlag) {
                            timeDetect = System.currentTimeMillis();// 首次检测到位于设定的距离的时间
                        }
                        long duration = System.currentTimeMillis() - timeDetect;
                        if (duration >= durationForDetect) {
                            if (callback != null) {
                                callback.onWarning();
                            }
                            timeDetect = System.currentTimeMillis();
                        }

                        inScopeFlag = true;
                    } else {
                        inScopeFlag = false;
                    }
                }

                isExists = true;
                freeCount = 0;
            }
        }

        if (callback != null) {
            callback.onDetected(distance);
        }
    }

    private void detectDistanceAndLog(float distance) {
        if (distance < 0) {
            if (freeCount >= FREE_VALID_COUNT) {
                if (isExists) {
                    float seconds = (System.currentTimeMillis() - timeDetect) / 1000.0f;
                    float length = (float) lengthSum / lengthCount;
                    boolean validFlag = length > minDistance && length < maxDistance;
                    String msg = String.format("%.1f（厘米）持续时间：%.1f（秒）", length, seconds);
                    //log(validFlag, msg);
                    freeTimeDetect = System.currentTimeMillis();
                }
                isExists = false;
                existCount = 0;
            }
            freeCount ++;
            if (freeCount >= 35500) freeCount = FREE_VALID_COUNT;
            return;
        }

        if (existCount >= EXISTS_VALID_COUNT) {
            if (!isExists) {
                lengthSum = 0;
                lengthCount = 0;
                timeDetect = System.currentTimeMillis();
                float seconds = (timeDetect - freeTimeDetect) / 1000.0f;
                String msg = String.format("未检测到人,持续时间：%.1f（秒）", seconds);
                //log(false, msg);
            }
            isExists = true;
            freeCount = 0;
        }
        existCount ++;
        lengthSum += distance;
        lengthCount ++;
        if (callback != null) {
            callback.onDetected(distance);
        }
    }

    private void log(boolean valid, String msg) {
        if (valid) {
            validLogger.log(Level.INFO, msg);
        } else {
            noneLogger.log(Level.INFO, msg);
        }
    }

    private void initLogger() {
        validLogger = Logger.getLogger("valid");
        noneLogger = Logger.getLogger("none");

        try {
            String dir = ImgProcApp.getWorkDirectory();
            FileHandler validHandler = new FileHandler(dir + File.separator + "valid.log", true);
            validHandler.setLevel(Level.ALL);
            validHandler.setFormatter(new MsgFormatter());
            validLogger.addHandler(validHandler);

            FileHandler noneHandler = new FileHandler(dir + File.separator + "none.log", true);
            noneHandler.setLevel(Level.ALL);
            noneHandler.setFormatter(new MsgFormatter());
            noneLogger.addHandler(noneHandler);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private class MsgFormatter extends Formatter {
        private StringBuilder strBuilder;
        public MsgFormatter() {
            strBuilder = new StringBuilder();
        }
        @Override
        public String format(LogRecord record) {
            strBuilder.setLength(0);
            strBuilder.append(record.getLevel().toString());
            strBuilder.append("：");
            strBuilder.append(record.getMessage());
            strBuilder.append("\t[");
            strBuilder.append(Tools.getDateTimeFromMillisecond(record.getMillis()));
            strBuilder.append("]\n");
            return strBuilder.toString();
        }
    }

    public interface ProcessCallback {

        void onDetected(float distance);
        void onWarning();
    }
}
