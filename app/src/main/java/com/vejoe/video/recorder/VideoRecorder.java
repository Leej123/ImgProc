package com.vejoe.video.recorder;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.media.MediaRecorder;

import com.vejoe.video.CameraWrapper;
import com.vejoe.video.PrepareCameraException;
import com.vejoe.video.VideoFile;
import com.vejoe.video.configuration.CaptureConfiguration;
import com.vejoe.video.configuration.RecordingSize;

import java.io.IOException;

/**
 * <p>@author: Leej
 * <p>Company: VEJOE
 * <p>Comment: //TODO
 * <p>Date: 2017/11/2 0002-上午 10:11
 */

public class VideoRecorder implements MediaRecorder.OnInfoListener {
    private final VideoRecorderInterface videoRecorderInterface;
    private final CaptureConfiguration captureConfiguration;
    private final VideoFile videoFile;
    private CameraWrapper cameraWrapper = null;
    private MediaRecorder recorder;
    private boolean recording  = false;

    public VideoRecorder(CameraWrapper cameraWrapper, VideoRecorderInterface videoRecorderInterface, CaptureConfiguration captureConfiguration, VideoFile videoFile, boolean useFrontFacingCamera) {
        this.cameraWrapper = cameraWrapper;
        this.videoRecorderInterface = videoRecorderInterface;
        this.captureConfiguration = captureConfiguration;
        this.videoFile = videoFile;
    }

    public void toggleRecording() {
        if (cameraWrapper == null) {
            return;
        }

        if (isRecording()) {
            stopRecording(null);
        } else {
            startRecording();
        }
    }

    protected void startRecording() {
        recording = false;

        if (!initRecorder()) {
            return;
        }

        if (!prepareRecorder()) {
            return;
        }

        if (!startRecorder()) {
            return;
        }

        recording = true;
        videoRecorderInterface.onRecordingStarted();

    }

    /**
     * 停止录制
     * @param message
     */
    public void stopRecording(String message) {
        if (!isRecording()) {
            return;
        }

        try {
            getMediaRecorder().stop();
            videoRecorderInterface.onRecordingSuccess();
        } catch (final RuntimeException e) {
            e.printStackTrace();
        }

        recording = false;
        videoRecorderInterface.onRecordingStopped(message);
    }

    protected boolean isRecording() {
        return recording;
    }

    private boolean initRecorder() {
        try {
            cameraWrapper.prepareCameraForRecording();
        } catch (PrepareCameraException e) {
            e.printStackTrace();
            videoRecorderInterface.onRecordingFailed("Unable to record video");
            return false;
        }

        setMediaRecorder(new MediaRecorder());
        configureMediaRecorder(getMediaRecorder(), cameraWrapper.getCamera());

        return true;

    }

    protected void setMediaRecorder(MediaRecorder recorder) {
        this.recorder = recorder;
    }

    protected MediaRecorder getMediaRecorder() {
        return recorder;
    }

    @SuppressWarnings({"deprecation", "AliDeprecation"})
    protected void configureMediaRecorder(final MediaRecorder recorder, Camera camera)
        throws IllegalStateException, IllegalArgumentException {
        recorder.setCamera(camera);
        recorder.setAudioSource(captureConfiguration.getAudioSource());
        recorder.setVideoSource(captureConfiguration.getVideoSource());

        CamcorderProfile baseProfile = cameraWrapper.getBaseRecordingProfile();
        baseProfile.fileFormat = captureConfiguration.getOutputFormat();

        RecordingSize size = cameraWrapper.getSupportedRecordingSize(captureConfiguration.getVideoWidth(), captureConfiguration.getVideoHeight());
        baseProfile.videoFrameWidth = size.width;
        baseProfile.videoFrameHeight = size.height;
        baseProfile.videoBitRate = captureConfiguration.getVideoBitrate();

        baseProfile.audioCodec = captureConfiguration.getAudioEncoder();
        baseProfile.videoCodec = captureConfiguration.getVideoEncoder();

        recorder.setProfile(baseProfile);
        recorder.setMaxDuration(captureConfiguration.getMaxCaptureDuration());
        recorder.setOutputFile(videoFile.getFullPath());
        recorder.setOrientationHint(cameraWrapper.getRotationCorrection());
        recorder.setVideoFrameRate(captureConfiguration.getVideoFPS());

        try {
            recorder.setMaxFileSize(captureConfiguration.getMaxCaptureFileSize());
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
        } catch (RuntimeException e) {
            e.printStackTrace();
        }
        recorder.setOnInfoListener(this);
    }

    private boolean prepareRecorder() {
        try {
            getMediaRecorder().prepare();
            return true;
        } catch (final IllegalStateException e) {
            e.printStackTrace();
            return false;
        } catch (final IOException e) {
            e.printStackTrace();
            return false;
        }
    }

    private boolean startRecorder() {
        try {
            getMediaRecorder().start();
            return true;
        } catch (final IllegalStateException e) {
            e.printStackTrace();
            return false;
        } catch (final RuntimeException e2) {
            e2.printStackTrace();
            videoRecorderInterface.onRecordingFailed("Unable to record video with given settings");
            return false;
        }
    }

    public void releaseAllResources() {
        if (cameraWrapper != null) {
            cameraWrapper.releaseCamera();
            cameraWrapper = null;
        }
        releaseRecorderResources();
    }

    private void releaseRecorderResources() {
        MediaRecorder recorder = getMediaRecorder();
        if (recorder != null) {
            recorder.release();
            setMediaRecorder(null);
        }
    }

    @Override
    public void onInfo(MediaRecorder mr, int what, int extra) {
        switch (what) {
            case MediaRecorder.MEDIA_RECORDER_INFO_UNKNOWN:
                // NOP
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_DURATION_REACHED:
                stopRecording("Capture stopped - Max duration reached");
                break;
            case MediaRecorder.MEDIA_RECORDER_INFO_MAX_FILESIZE_REACHED:
                stopRecording("Capture stopped - Max file size reached");
                break;
            default:
                break;
        }
    }
}
