package com.vejoe.imgproc;

import android.Manifest;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Point;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.cameraview.CameraView;
import com.vejoe.opencv.EyesImageProcess;
import com.vejoe.opencv.LineImageProcess;
import com.vejoe.opencv.OpenCV;
import com.vejoe.utils.Constants;
import com.vejoe.utils.Tools;
import com.vejoe.widget.Line;
import com.vejoe.widget.MaskLineView;

import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

public class CameraActivity extends AppCompatActivity implements View.OnClickListener{
    private final int REQUEST_CAMERA_PERMISSION = 0;
    private static final String FRAGMENT_DIALOG = "dialog";

    private ImageButton imgFlash;
    private CameraView cameraView;
    private CameraView frontCameraView;

    private int flash = CameraView.FLASH_OFF;

    private EyesImageProcess imgProcess;
    private LineImageProcess lineImageProcess;
//    private SoundPool soundPool = null;
//    private int soundId = 0;
//    private int streamId = 0;
    private SoundPlayer soundPlayer;

    private TextView tvDistance;
    private String distanceFormat;

    private boolean autoSwitchCamera;
    private int timerInterval = 0;
    private Timer timer = null;
    private Object switchLock = new Object();
    private boolean isCameraOpened = false;

    private MaskLineView maskLineView;
    private boolean calibrationLine = false;

    private OrientationDetector orientationDetector;
    private boolean needRotation = true;

    private MyTimer myTimer;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(this.getClass().getSimpleName(), "onCreate");
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_camera);

        orientationDetector = new OrientationDetector(this, SensorManager.SENSOR_DELAY_NORMAL);

        cameraView = (CameraView) findViewById(R.id.camera_view);
        frontCameraView = (CameraView) findViewById(R.id.front_camera_view);
        if (cameraView != null) {
            //cameraView.addCallback(callback);
            cameraView.setFlash(CameraView.FLASH_OFF);
        }

        maskLineView = (MaskLineView) findViewById(R.id.mask_line_view);

        Intent intent = getIntent();
        if (intent != null) {
            calibrationLine = intent.getBooleanExtra(Constants.KEY_CALIBRATION_LINE, false);
            if (calibrationLine) {
                findViewById(R.id.mask_line_menu).setVisibility(View.VISIBLE);
                findViewById(R.id.camera_menu).setVisibility(View.GONE);
                findViewById(R.id.front_camera_view).setVisibility(View.GONE);
                cameraView.setFacing(CameraView.FACING_BACK);
            }
            maskLineView.setCalibrationLine(calibrationLine);
        }
        maskLineView.setMaskLineDoneListener(new MaskLineView.MaskLineDoneListener() {
            @Override
            public void onMaskLineDone(boolean done) {
                View v = findViewById(R.id.btn_mask_done);
                v.setVisibility(done? View.VISIBLE : View.INVISIBLE);
            }
        });

        imgFlash = (ImageButton) findViewById(R.id.btn_flash_ctrl);
        tvDistance = (TextView) findViewById(R.id.tv_detect_distance);

        findViewById(R.id.btn_switch_camera).setOnClickListener(this);
        findViewById(R.id.btn_flash_ctrl).setOnClickListener(this);
        findViewById(R.id.btn_settings).setOnClickListener(this);
        findViewById(R.id.btn_alarm).setOnClickListener(this);

        findViewById(R.id.btn_mask_cancel).setOnClickListener(this);
        findViewById(R.id.btn_mask_done).setOnClickListener(this);
        findViewById(R.id.btn_mask_undo).setOnClickListener(this);
        findViewById(R.id.btn_mask_done).setVisibility(View.INVISIBLE);

        OpenCV.init(ImgProcApp.getHaarFilePath());

        distanceFormat = getResources().getString(R.string.detect_distance_label);
        initSoundPool();
        imgProcess = new EyesImageProcess();
        imgProcess.setProcessCallback(processCallback);

        lineImageProcess = new LineImageProcess();
        lineImageProcess.setOnProcessListener(processListener);

        myTimer = new MyTimer(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        Log.d(this.getClass().getSimpleName(), "onResume");
        if (orientationDetector.canDetectOrientation())
            orientationDetector.enable();
        soundPlayer.resume();
        imgProcess.updateParams();

        if (!calibrationLine) {
            autoSwitchCamera();
        }

        // 设置直线的参数
        SharedPreferences prefs = ImgProcApp.getAppSharedPreferences();
        boolean useSystemRingtone = prefs.getBoolean(Constants.KEY_USE_SYSTEM_RINGTONE, false);
        if (useSystemRingtone != soundPlayer.isUesSytemRingtone()) {
            if (useSystemRingtone)
                soundPlayer.initWithSystemDefaultRingtone(this);
            else
                soundPlayer.init(this);
        }

        int color = prefs.getInt(Constants.KEY_LINE_COLOR, Color.WHITE);
        String width = prefs.getString(Constants.KEY_LINE_WIDTH, Constants.LINE_WIDTH_DEFAULT_VALUE);
        maskLineView.setColor(color);
        maskLineView.setLineStrokeWidth(Float.parseFloat(width));
        String lineOnePoints = prefs.getString(Constants.KEY_LINE_ONE_POINTS, "");
        String lineTwoPoints = prefs.getString(Constants.KEY_LINE_TWO_POINTS, "");
        Line lineOne = null;
        Line lineTwo = null;
        if (!TextUtils.isEmpty(lineOnePoints) && !TextUtils.isEmpty(lineTwoPoints)) {
            lineOne = new Line(new Point(), new Point());
            lineTwo = new Line(new Point(), new Point());
            updateMaskLine(lineOnePoints, lineTwoPoints, lineOne, lineTwo);
        } else {
            maskLineView.clearLines();
        }
        int[] screenSize = Tools.getScreenSize(this);
        int viewWidth = prefs.getInt(Constants.KEY_CALIBRATION_VIEW_WIDTH, screenSize[0]);
        int viewHeight = prefs.getInt(Constants.KEY_CALIBRATION_VIEW_HEIGHT, screenSize[1]);
        lineImageProcess.setCalibrationParams(lineOne, lineTwo, viewWidth, viewHeight);

        boolean openBoth = prefs.getBoolean(Constants.KEY_SHOW_BOTH_CAMERA, false);
        boolean showSwitchBtn = !openBoth && !autoSwitchCamera;
        findViewById(R.id.btn_switch_camera).setVisibility(showSwitchBtn? View.VISIBLE : View.INVISIBLE);

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED) {
            if (!calibrationLine) {
                if (openBoth) {
                    try {
                        cameraView.addCallback(backCameraCallback);
                        frontCameraView.setVisibility(View.VISIBLE);
                        frontCameraView.addCallback(frontCameraCallback);
                        frontCameraView.start();
                    } catch (RuntimeException e) {
                        e.printStackTrace();
                        frontCameraView.removeCallback(frontCameraCallback);
                        frontCameraView.stop();
                        SharedPreferences.Editor editor = prefs.edit();
                        editor.putBoolean(Constants.KEY_SHOW_BOTH_CAMERA, false);
                        editor.commit();
                    }
                } else {
                    frontCameraView.stop();
                    frontCameraView.setVisibility(View.GONE);

                    cameraView.addCallback(callback);
                }
            }
            cameraView.start();

        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this,
                Manifest.permission.CAMERA)) {
            ConfirmationDialogFragment
                    .newInstance(R.string.camera_permission_confirmation,
                            new String[]{Manifest.permission.CAMERA},
                            REQUEST_CAMERA_PERMISSION,
                            R.string.camera_permission_not_granted)
                    .show(getSupportFragmentManager(), FRAGMENT_DIALOG);
        } else {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA},
                    REQUEST_CAMERA_PERMISSION);
        }
    }

    private void updateMaskLine(String lineOnePoints, String lineTwoPoints, Line lineOne, Line lineTwo) {
        String[] points = lineOnePoints.split(",");
        String[] points2 = lineTwoPoints.split(",");
        if (points.length < 4 || points2.length < 4)
            return;

        try {
            Point startPoint = new Point(Integer.parseInt(points[0]), Integer.parseInt(points[1]));
            Point endPoint = new Point(Integer.parseInt(points[2]), Integer.parseInt(points[3]));
            maskLineView.setLineOne(startPoint, endPoint);
            lineOne.startPoint.set(startPoint.x, startPoint.y);
            lineOne.endPoint.set(endPoint.x, endPoint.y);

            startPoint = new Point(Integer.parseInt(points2[0]), Integer.parseInt(points2[1]));
            endPoint = new Point(Integer.parseInt(points2[2]), Integer.parseInt(points2[3]));
            maskLineView.setLineTwo(startPoint, endPoint);
            lineTwo.startPoint.set(startPoint.x, startPoint.y);
            lineTwo.endPoint.set(endPoint.x, endPoint.y);
        } catch (NumberFormatException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        Log.d(this.getClass().getSimpleName(), "onPause");
        orientationDetector.disable();
        if (timer != null) {
            synchronized (switchLock) {
                switchLock.notify();
            }
            timer.cancel();
            timer = null;
        }
        if (cameraView.isCameraOpened())
            cameraView.stop();
        if (frontCameraView.isCameraOpened())
            frontCameraView.stop();

        SharedPreferences prefs = ImgProcApp.getAppSharedPreferences();
        boolean openBoth = prefs.getBoolean(Constants.KEY_SHOW_BOTH_CAMERA, false);
        if (openBoth) {
            frontCameraView.removeCallback(frontCameraCallback);
            cameraView.removeCallback(backCameraCallback);
        } else {
            cameraView.removeCallback(callback);
        }

        soundPlayer.pause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(this.getClass().getSimpleName(), "onDestroy");
        imgProcess.stop();
        lineImageProcess.stop();
//        if (streamId != 0) {
//            soundPool.stop(streamId);
//        }
//        soundPool.release();
//        soundPool = null;
        soundPlayer.release();
        if (timer != null) {
            timer.cancel();
            timer = null;
        }

        myTimer.stop();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        finish();
    }

//    @Override
//    public void onConfigurationChanged(Configuration newConfig) {
//        super.onConfigurationChanged(newConfig);
//        Log.d(this.getClass().getSimpleName(), "onConfigurationChanged");
//    }

    private CameraView.Callback backCameraCallback = new CameraView.Callback() {
        @Override
        public void onPreviewFrame(byte[] data, int width, int height, int cameraType, float focalLength) {
            super.onPreviewFrame(data, width, height, cameraType, focalLength);

            if (calibrationLine)
                return;

            if (!lineImageProcess.isStart()) {
                lineImageProcess.start();
            }
            lineImageProcess.processFrame(data, width, height, needRotation);
        }
    };

    private CameraView.Callback frontCameraCallback = new CameraView.Callback() {
        @Override
        public void onPreviewFrame(byte[] data, int width, int height, int cameraType, float focalLength) {
            super.onPreviewFrame(data, width, height, cameraType, focalLength);
            if (calibrationLine)
                return;

            if (!imgProcess.isStart()) {
                imgProcess.start();
            }
            imgProcess.processFrame(data, width, height, cameraType, focalLength, needRotation);
        }
    };

    private CameraView.Callback callback = new CameraView.Callback() {
        @Override
        public void onCameraOpened(CameraView cameraView) {
            super.onCameraOpened(cameraView);
        }

        @Override
        public void onCameraClosed(CameraView cameraView) {
            super.onCameraClosed(cameraView);
            synchronized (switchLock) {
                isCameraOpened = false;
            }
        }

        @Override
        public void onPictureTaken(CameraView cameraView, byte[] data) {
            super.onPictureTaken(cameraView, data);
        }

        @Override
        public void onPreviewFrame(byte[] data, int width, int height, int cameraType, float focalLength) {
            synchronized (switchLock) {
                isCameraOpened = true;
                switchLock.notify();
            }

            if (calibrationLine)
                return;

            if (cameraType == CameraView.FACING_BACK) {
                if (!lineImageProcess.isStart()) {
                    lineImageProcess.start();
                }
                lineImageProcess.processFrame(data, width, height, needRotation);
            } else {
                if (!imgProcess.isStart()) {
                    imgProcess.start();
                }
                imgProcess.processFrame(data, width, height, cameraType, focalLength, needRotation);
            }
        }
    };

    private void autoSwitchCamera() {
        SharedPreferences prefs = ImgProcApp.getAppSharedPreferences();
        autoSwitchCamera = prefs.getBoolean(Constants.KEY_AUTO_SWITCH_CAMERA, true);
        if (autoSwitchCamera) {
            String value = prefs.getString(Constants.KEY_AUTO_SWITCH_CAMERA_FREQUENCY, Constants.CAMERA_SWITCH_FREQUENCY_DEFAULT_VALUE);
            int freq = Integer.parseInt(value) * 1000;
            if (freq != timerInterval) {
                if (timer != null) {
                    timer.cancel();
                    timer = null;
                }
                timerInterval = freq;
            }
            if (timer == null) {
                timer = new Timer();
                timer.schedule(new TimerTask() {
                    @Override
                    public void run() {
                        synchronized (switchLock) {
                            if (!isCameraOpened) {
                                try {
                                    switchLock.wait();
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }
                            }
                        }
                        isCameraOpened = false;
                        int facing = cameraView.getFacing();
                        if (facing == CameraView.FACING_FRONT) {
                            cameraView.setFacing(CameraView.FACING_BACK);
                        } else {
                            cameraView.setFacing(CameraView.FACING_FRONT);
                        }
                    }
                }, 0, timerInterval);
            }
        } else {
            if (timer != null) {
                timer.cancel();
                timer = null;
            }
        }
    }

    private EyesImageProcess.ProcessCallback processCallback = new EyesImageProcess.ProcessCallback() {
        @Override
        public void onDetected(final float distance) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (distance > 0) {
                        String text = String.format(distanceFormat, distance);
                        tvDistance.setText(text);
                    } else {
                        tvDistance.setText("");
                    }
                }
            });
        }

        @Override
        public void onWarning() {
            soundTheAlarm();
        }
    };

    private void soundTheAlarm() {
//        if (streamId != 0) {
//            soundPool.stop(streamId); // 停止上次播放
//        }
//        streamId = soundPool.play(soundId, 1, 1, 0, 0, 1);
        soundPlayer.play();
    }

    private void initSoundPool() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            SoundPool.Builder builder = new SoundPool.Builder();
//            builder.setMaxStreams(2);
//            AudioAttributes.Builder attrBuilder = new AudioAttributes.Builder();
//            attrBuilder.setLegacyStreamType(AudioManager.STREAM_MUSIC);
//            builder.setAudioAttributes(attrBuilder.build());
//            soundPool = builder.build();
//        } else {
//            soundPool = new SoundPool(2, AudioManager.STREAM_MUSIC, 5);
//        }
//        soundId = soundPool.load(this, R.raw.warning, 1);

        SharedPreferences prefs = ImgProcApp.getAppSharedPreferences();
        boolean useSystemRingtone = prefs.getBoolean(Constants.KEY_USE_SYSTEM_RINGTONE, false);
        soundPlayer = new SoundPlayer();
        if (useSystemRingtone) {
            soundPlayer.initWithSystemDefaultRingtone(this);
        } else {
            soundPlayer.init(this);
        }
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.btn_switch_camera) {
            if (cameraView != null) {
                int facing = cameraView.getFacing();
                cameraView.setFacing(facing == CameraView.FACING_FRONT? CameraView.FACING_BACK : CameraView.FACING_FRONT);
            }
            return;
        }

        if (v.getId() == R.id.btn_flash_ctrl) {
            if (cameraView != null) {
                flash = cameraView.getFlash();
                if (flash == CameraView.FLASH_TORCH) {
                    flash = CameraView.FLASH_OFF;
                    cameraView.setFlash(CameraView.FLASH_OFF);
                    imgFlash.setImageResource(R.drawable.ic_flash_off);
                } else {
                    flash = CameraView.FLASH_TORCH;
                    cameraView.setFlash(CameraView.FLASH_TORCH);
                    imgFlash.setImageResource(R.drawable.ic_flash_torch);
                }
            }
            return;
        }

        if (v.getId() == R.id.btn_settings) {
            Intent intent = new Intent(CameraActivity.this, SettingsActivity.class);
            startActivity(intent);
            return;
        }

        if (v.getId() == R.id.btn_alarm) {
            myTimer.displayOnView((TextView) findViewById(R.id.tv_count_down));
            myTimer.start();
            return;
        }

        if (v.getId() == R.id.btn_mask_undo) {
            if (maskLineView != null) {
                maskLineView.undo();
            }
            return;
        }

        if (v.getId() == R.id.btn_mask_cancel) {
            onBackPressed();
            return;
        }

        if (v.getId() == R.id.btn_mask_done) {
            Point startPoint1 = new Point();
            Point endPoint1 = new Point();
            Point startPoint2 = new Point();
            Point endPoint2 = new Point();
            maskLineView.getLines(startPoint1, endPoint1, startPoint2, endPoint2);

            SharedPreferences prefs = ImgProcApp.getAppSharedPreferences();
            SharedPreferences.Editor editor = prefs.edit();

            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(startPoint1.x);
            stringBuilder.append(",");
            stringBuilder.append(startPoint1.y);
            stringBuilder.append(",");
            stringBuilder.append(endPoint1.x);
            stringBuilder.append(",");
            stringBuilder.append(endPoint1.y);
            editor.putString(Constants.KEY_LINE_ONE_POINTS, stringBuilder.toString());

            stringBuilder.setLength(0);
            stringBuilder.append(startPoint2.x);
            stringBuilder.append(",");
            stringBuilder.append(startPoint2.y);
            stringBuilder.append(",");
            stringBuilder.append(endPoint2.x);
            stringBuilder.append(",");
            stringBuilder.append(endPoint2.y);
            editor.putString(Constants.KEY_LINE_TWO_POINTS, stringBuilder.toString());

            editor.putInt(Constants.KEY_CALIBRATION_VIEW_WIDTH, maskLineView.getWidth());
            editor.putInt(Constants.KEY_CALIBRATION_VIEW_HEIGHT, maskLineView.getHeight());

            editor.apply();

            onBackPressed();
            return;
        }
    }

    private LineImageProcess.OnProcessListener processListener = new LineImageProcess.OnProcessListener() {
        @Override
        public void onProcess(Object result, int with, int height) {
            List<Line> lines = (List<Line>) result;
            if (lines == null || lines.size() == 0) {
                maskLineView.clearDetectLines();
            } else {
                //maskLineView.setDetectLines(points, with, height);
                maskLineView.setDetectLines(lines);
            }
        }

        @Override
        public void onWarning() {
            soundTheAlarm();
        }
    };

    public static class ConfirmationDialogFragment extends DialogFragment {

        private static final String ARG_MESSAGE = "message";
        private static final String ARG_PERMISSIONS = "permissions";
        private static final String ARG_REQUEST_CODE = "request_code";
        private static final String ARG_NOT_GRANTED_MESSAGE = "not_granted_message";

        public static ConfirmationDialogFragment newInstance(@StringRes int message,
                                                             String[] permissions, int requestCode, @StringRes int notGrantedMessage) {
            ConfirmationDialogFragment fragment = new ConfirmationDialogFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_MESSAGE, message);
            args.putStringArray(ARG_PERMISSIONS, permissions);
            args.putInt(ARG_REQUEST_CODE, requestCode);
            args.putInt(ARG_NOT_GRANTED_MESSAGE, notGrantedMessage);
            fragment.setArguments(args);
            return fragment;
        }

        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            final Bundle args = getArguments();
            return new AlertDialog.Builder(getActivity())
                    .setMessage(args.getInt(ARG_MESSAGE))
                    .setPositiveButton(android.R.string.ok,
                            new DialogInterface.OnClickListener() {

                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    String[] permissions = args.getStringArray(ARG_PERMISSIONS);
                                    if (permissions == null) {
                                        throw new IllegalArgumentException();
                                    }
                                    ActivityCompat.requestPermissions(getActivity(),
                                            permissions, args.getInt(ARG_REQUEST_CODE));
                                }
                            })
                    .setNegativeButton(android.R.string.cancel,
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int which) {
                                    Toast.makeText(getActivity(),
                                            args.getInt(ARG_NOT_GRANTED_MESSAGE),
                                            Toast.LENGTH_SHORT).show();
                                }
                            })
                    .create();
        }
    }

    protected class OrientationDetector extends OrientationEventListener {

        public OrientationDetector(Context context, int rate) {
            super(context, rate);
        }

        @Override
        public void onOrientationChanged(int orientation) {
            if (orientation == ORIENTATION_UNKNOWN)
                return;

            //只检测是否有四个角度的改变
            if (orientation > 345 || orientation < 15) { //0度
                //orientation = 0;
                needRotation = true;
            } else if (orientation > 75 && orientation < 105) { //90度
                //orientation = 90;
                needRotation = true;
            } else if (orientation > 165 && orientation < 195) { //180度
                //orientation = 180;
                needRotation = false;
            } else if (orientation > 255 && orientation < 285) { //270度
                //orientation = 270;
                needRotation = false;
            } else {
                return;
            }
        }
    }
}
