package com.vejoe.video.configuration;

/**
 * <p>@author: Leej
 * <p>Company: VEJOE
 * <p>Comment: 摄像头的分辨率
 * <p>Date: 2017/10/28 0028-上午 10:37
 */
public class CameraSize {

    private final int width;
    private final int height;

    public CameraSize(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}
