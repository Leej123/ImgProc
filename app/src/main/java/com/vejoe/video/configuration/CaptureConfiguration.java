package com.vejoe.video.configuration;

import android.media.MediaRecorder;
import android.os.Parcel;
import android.os.Parcelable;

/**
 * <p>@author: Leej
 * <p>Company: VEJOE
 * <p>Comment: 视频录制的配置
 * <p>Date: 2017/10/28 0028-上午 11:07
 */
public class CaptureConfiguration implements Parcelable {

    public static final int MBYTE_TO_BYTE = 1024 * 1024;
    public static final int MSEC_TO_SEC = 1000;

    public static final int NO_DURATION_LIMIT = -1;
    public static final int NO_FILESIZE_LIMIT = -1;

    /**
     * 视频的宽。
     */
    private int videoWidth = PredefinedCaptureConfigurations.WIDTH_720P;
    /**
     * 视频的高
     */
    private int videoHeight = PredefinedCaptureConfigurations.HEIGHT_720P;
    /**
     * 视频的码率
     */
    private int bitrate = PredefinedCaptureConfigurations.BITRATE_HQ_720P;

    /**
     * 最大持续时间
     */
    private int maxDurationMs = NO_DURATION_LIMIT;
    /**
     * 视频文件最大容量
     */
    private int maxFileSizeBytes = NO_FILESIZE_LIMIT;
    private boolean showTimer = false;
    private boolean allowFrontFacingCamera = true;

    /**
     * 视频帧率。默认30 FPS。
     */
    private int videoFrameRate = PredefinedCaptureConfigurations.FPS_30;

    /**
     * 视频格式
     */
    private int OUTPUT_FORMAT = MediaRecorder.OutputFormat.MPEG_4;
    /**
     * 音频源
     */
    private int AUDIO_SOURCE = MediaRecorder.AudioSource.DEFAULT;
    /**
     * 音频编码
     */
    private int AUDIO_ENCODER = MediaRecorder.AudioEncoder.AAC;
    /**
     * 视频源
     */
    private int VIDEO_SOURCE = MediaRecorder.VideoSource.CAMERA;
    /**
     * 视频编码器
     */
    private int VIDEO_ENCODER = MediaRecorder.VideoEncoder.H264;

    /**
     * 获取默认配置
     * @return
     */
    public static CaptureConfiguration getDefault() {
        return new CaptureConfiguration();
    }

    private CaptureConfiguration() {
        // Default configuration
    }

    @Deprecated
    public CaptureConfiguration(PredefinedCaptureConfigurations.CaptureResolution resolution, PredefinedCaptureConfigurations.CaptureQuality quality) {
        videoWidth = resolution.width;
        videoHeight = resolution.height;
        bitrate = resolution.getBitrate(quality);
    }

    @Deprecated
    public CaptureConfiguration(PredefinedCaptureConfigurations.CaptureResolution resolution, PredefinedCaptureConfigurations.CaptureQuality quality, int maxDurationSecs,
                                int maxFileSizeMb, boolean showTimer) {
        this(resolution, quality, maxDurationSecs, maxFileSizeMb, showTimer, false);
        this.showTimer = showTimer;
    }

    @Deprecated
    public CaptureConfiguration(PredefinedCaptureConfigurations.CaptureResolution resolution, PredefinedCaptureConfigurations.CaptureQuality quality, int maxDurationSecs,
                                int maxFileSizeMb, boolean showTimer, boolean allowFrontFacingCamera) {
        this(resolution, quality, maxDurationSecs, maxFileSizeMb);
        this.showTimer = showTimer;
        this.allowFrontFacingCamera = allowFrontFacingCamera;
    }

    @Deprecated
    public CaptureConfiguration(PredefinedCaptureConfigurations.CaptureResolution resolution, PredefinedCaptureConfigurations.CaptureQuality quality, int maxDurationSecs,
                                int maxFileSizeMb, boolean showTimer, boolean allowFrontFacingCamera,
                                int videoFPS) {
        this(resolution, quality, maxDurationSecs, maxFileSizeMb, showTimer, allowFrontFacingCamera);
        videoFrameRate = videoFPS;
    }

    @Deprecated
    public CaptureConfiguration(PredefinedCaptureConfigurations.CaptureResolution resolution, PredefinedCaptureConfigurations.CaptureQuality quality, int maxDurationSecs,
                                int maxFileSizeMb) {
        this(resolution, quality);
        maxDurationMs = maxDurationSecs * MSEC_TO_SEC;
        maxFileSizeBytes = maxFileSizeMb * MBYTE_TO_BYTE;
    }

    @Deprecated
    public CaptureConfiguration(int videoWidth, int videoHeight, int bitrate) {
        this.videoWidth = videoWidth;
        this.videoHeight = videoHeight;
        this.bitrate = bitrate;
    }

    @Deprecated
    public CaptureConfiguration(int videoWidth, int videoHeight, int bitrate, int maxDurationSecs, int maxFilesizeMb) {
        this(videoWidth, videoHeight, bitrate);
        maxDurationMs = maxDurationSecs * MSEC_TO_SEC;
        maxFileSizeBytes = maxFilesizeMb * MBYTE_TO_BYTE;
    }

    /**
     * @return 获取视频的宽（像素）
     */
    public int getVideoWidth() {
        return videoWidth;
    }

    /**
     * @return 获取视频的高（像素）
     */
    public int getVideoHeight() {
        return videoHeight;
    }

    /**
     * @return 视频的码率
     */
    public int getVideoBitrate() {
        return bitrate;
    }

    /**
     * @return 视频录制的最大持续时间
     */
    public int getMaxCaptureDuration() {
        return maxDurationMs;
    }

    /**
     * @return 视频文件的最大字节数
     */
    public int getMaxCaptureFileSize() {
        return maxFileSizeBytes;
    }

    /**
     * @return 是否在录制视频时显示定时
     */
    public boolean getShowTimer() {
        return showTimer;
    }

    /**
     * @return 是否允许前置摄像头
     */
    public boolean getAllowFrontFacingCamera() {
        return allowFrontFacingCamera;
    }

    /**
     * @return 输出视频的格式
     */
    public int getOutputFormat() {
        return OUTPUT_FORMAT;
    }

    /**
     * @return 音频源
     */
    public int getAudioSource() {
        return AUDIO_SOURCE;
    }

    /**
     * @return 音频编码器
     */
    public int getAudioEncoder() {
        return AUDIO_ENCODER;
    }

    /**
     * @return 视频源
     */
    public int getVideoSource() {
        return VIDEO_SOURCE;
    }

    /**
     * @return 视频编码器
     */
    public int getVideoEncoder() {
        return VIDEO_ENCODER;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(videoWidth);
        dest.writeInt(videoHeight);
        dest.writeInt(bitrate);
        dest.writeInt(maxDurationMs);
        dest.writeInt(maxFileSizeBytes);
        dest.writeInt(videoFrameRate);
        dest.writeByte((byte) (showTimer ? 1 : 0));
        dest.writeByte((byte) (allowFrontFacingCamera ? 1 : 0));

        dest.writeInt(OUTPUT_FORMAT);
        dest.writeInt(AUDIO_SOURCE);
        dest.writeInt(AUDIO_ENCODER);
        dest.writeInt(VIDEO_SOURCE);
        dest.writeInt(VIDEO_ENCODER);
    }

    public static final Creator<CaptureConfiguration> CREATOR = new Creator<CaptureConfiguration>() {
        @Override
        public CaptureConfiguration createFromParcel(
                Parcel in) {
            return new CaptureConfiguration(in);
        }

        @Override
        public CaptureConfiguration[] newArray(
                int size) {
            return new CaptureConfiguration[size];
        }
    };

    private CaptureConfiguration(Parcel in) {
        videoWidth = in.readInt();
        videoHeight = in.readInt();
        bitrate = in.readInt();
        maxDurationMs = in.readInt();
        maxFileSizeBytes = in.readInt();
        videoFrameRate = in.readInt();
        showTimer = in.readByte() != 0;
        allowFrontFacingCamera = in.readByte() != 0;

        OUTPUT_FORMAT = in.readInt();
        AUDIO_SOURCE = in.readInt();
        AUDIO_ENCODER = in.readInt();
        VIDEO_SOURCE = in.readInt();
        VIDEO_ENCODER = in.readInt();
    }

    public int getVideoFPS() {
        return videoFrameRate;
    }

    /**
     * 配置的建造起
     */
    public static class Builder {

        private final CaptureConfiguration configuration;

        public Builder(PredefinedCaptureConfigurations.CaptureResolution resolution, PredefinedCaptureConfigurations.CaptureQuality quality) {
            configuration = new CaptureConfiguration();
            configuration.videoWidth = resolution.width;
            configuration.videoHeight = resolution.height;
            configuration.bitrate = resolution.getBitrate(quality);
        }

        public Builder(int width, int height, int bitrate) {
            configuration = new CaptureConfiguration();
            configuration.videoWidth = width;
            configuration.videoHeight = height;
            configuration.bitrate = bitrate;
        }

        public CaptureConfiguration build() {
            return configuration;
        }

        public Builder maxDuration(int maxDurationSec) {
            configuration.maxDurationMs = maxDurationSec * MSEC_TO_SEC;
            return this;
        }

        public Builder maxFileSize(int maxFileSizeMb) {
            configuration.maxFileSizeBytes = maxFileSizeMb * MBYTE_TO_BYTE;
            return this;
        }

        public Builder frameRate(int framesPerSec) {
            configuration.videoFrameRate = framesPerSec;
            return this;
        }

        public Builder showRecordingTime() {
            configuration.showTimer = true;
            return this;
        }

        public Builder noCameraToggle() {
            configuration.allowFrontFacingCamera = false;
            return this;
        }
    }
}