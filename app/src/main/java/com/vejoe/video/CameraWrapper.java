package com.vejoe.video;

import android.hardware.Camera;
import android.media.CamcorderProfile;
import android.os.Build;

import com.vejoe.video.configuration.CameraSize;
import com.vejoe.video.configuration.RecordingSize;

import java.util.List;

/**
 * <p>@author: Leej
 * <p>Company: VEJOE
 * <p>Comment: //TODO
 * <p>Date: 2017/11/2 0002-上午 10:53
 */

public class CameraWrapper {
    final Camera camera;
    private final int displayRotation;
    private boolean useFrontFacingCamera;

    public CameraWrapper(Camera camera, int displayRotation, boolean useFrontFacingCamera) {
        this.camera = camera;
        this.displayRotation = displayRotation;
        this.useFrontFacingCamera = useFrontFacingCamera;
    }

    public Camera getCamera() {
        return camera;
    }

    public void prepareCameraForRecording() throws PrepareCameraException {
        try {
            camera.unlock();
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new PrepareCameraException();
        }
    }

    public void releaseCamera() {
        if (getCamera() == null) {
            return;
        }

        camera.release();
    }

    public CamcorderProfile getBaseRecordingProfile() {
        CamcorderProfile profile;
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) {
            profile = getDefaultRecordingProfile();
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_720P)) {
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_720P);
        } else if (CamcorderProfile.hasProfile(CamcorderProfile.QUALITY_480P)) {
            profile = CamcorderProfile.get(CamcorderProfile.QUALITY_480P);
        } else {
            profile = getDefaultRecordingProfile();
        }

        return profile;
    }

    private CamcorderProfile getDefaultRecordingProfile() {
        CamcorderProfile highProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_HIGH);
        if (highProfile != null) {
            return highProfile;
        }
        CamcorderProfile lowProfile = CamcorderProfile.get(CamcorderProfile.QUALITY_LOW);
        if (lowProfile != null) {
            return lowProfile;
        }
        throw  new RuntimeException("No quality level found");
    }

    public int getRotationCorrection() {
        int rotation = displayRotation * 90;
        if (useFrontFacingCamera) {
            int mirroredRotation = (getCameraOrientation() + rotation) % 360;
            return (360 - mirroredRotation) % 360;
        } else {
            return (getCameraOrientation() - rotation + 360) % 360;
        }
    }

    public int getCameraOrientation() {
        Camera.CameraInfo camInfo = new Camera.CameraInfo();
        Camera.getCameraInfo(getCurrentCameraId(), camInfo);
        return camInfo.orientation;
    }

    private int getCurrentCameraId() {
        int cameraId = -1;
        int numberOfCameras = Camera.getNumberOfCameras();
        for (int i = 0; i < numberOfCameras; i++) {
            Camera.CameraInfo info = new Camera.CameraInfo();
            Camera.getCameraInfo(i, info);
            if (info.facing == getCurrentCameraFacing()) {
                cameraId = i;
                break;
            }
        }
        return cameraId;
    }

    private int getCurrentCameraFacing() {
        return useFrontFacingCamera ? Camera.CameraInfo.CAMERA_FACING_FRONT : Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    public RecordingSize getSupportedRecordingSize(int width, int height) {
        CameraSize recordingSize = getOptimalSize(getSupportedVideoSizes(Build.VERSION.SDK_INT), width, height);
        if (recordingSize == null) {
            return new RecordingSize(width, height);
        }
        return new RecordingSize(recordingSize.getWidth(), recordingSize.getHeight());
    }

    public List<Camera.Size> getSupportedVideoSizes(int currentSdkInt) {
        Camera.Parameters params = camera.getParameters();
        List<Camera.Size> supportedVideoSizes;
        if (currentSdkInt < Build.VERSION_CODES.HONEYCOMB) {
            supportedVideoSizes = params.getSupportedPreviewSizes();
        } else if (params.getSupportedVideoSizes() == null) {
            supportedVideoSizes = params.getSupportedPreviewSizes();
        } else {
            supportedVideoSizes = params.getSupportedVideoSizes();
        }
        return supportedVideoSizes;
    }

    public CameraSize getOptimalSize(List<Camera.Size> sizes, int w, int h) {
        final double ASPECT_TOLERANCE = 0.1;
        final double targetRatio = (double) w / h;
        if (sizes == null) {
            return null;
        }
        Camera.Size optimalSize = null;

        double minDiff = Double.MAX_VALUE;
        final int targetHeight = h;
        for (final Camera.Size size : sizes) {
            final  double ratio = (double) size.width / size.height;
            if (Math.abs(ratio - targetRatio) > ASPECT_TOLERANCE) {
                continue;
            }
            if (Math.abs(size.height - targetHeight) < minDiff) {
                optimalSize = size;
                minDiff = Math.abs(size.height - targetHeight);
            }
        }

        if (optimalSize == null) {
            minDiff = Double.MAX_VALUE;
            for (final Camera.Size size : sizes) {
                if (Math.abs(size.height -  targetHeight) < minDiff) {
                    optimalSize = size;
                    minDiff = Math.abs(size.height - targetHeight);
                }
            }
        }

        return new CameraSize(optimalSize.width, optimalSize.height);
    }

}
