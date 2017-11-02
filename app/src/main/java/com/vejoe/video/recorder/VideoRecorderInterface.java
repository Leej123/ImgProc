package com.vejoe.video.recorder;

/**
 * <p>@author: Leej
 * <p>Company: VEJOE
 * <p>Comment: 视频录制监听
 * <p>Date: 2017/10/28 0028-上午 11:33
 */
public interface VideoRecorderInterface {

    /**
     * 视频录制停止
     * @param message
     */
	void onRecordingStopped(String message);

    /**
     * 视频录制开始
     */
	void onRecordingStarted();

    /**
     * 视频录制成功
     */
	void onRecordingSuccess();

    /**
     * 视频录制失败
     * @param message
     */
	void onRecordingFailed(String message);

}