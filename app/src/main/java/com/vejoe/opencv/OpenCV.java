package com.vejoe.opencv;

/**
 * Created by Administrator on 2017/6/12 0012.
 */

public class OpenCV {

    static {
        System.loadLibrary("opencv");
    }

    public static native void init(String path);

    public static native String getStringFromNative();

    /**
     * 检测眼睛到摄像头的距离
     * @param img 图像数据
     * @param width 图像宽度
     * @param height 图像高度
     * @param cameraType 摄像头类型：0--后置摄像头；1--前置摄像头
     * @param focalLength 摄像头焦距
     * @param needRotation 是否需要旋转
     * @return 测量的距离：如果返回-1表示没有检测到眼睛。
     */
    public native float detectEyesDistance(byte[] img, int width, int height, int cameraType, float focalLength, boolean needRotation);

    public native int[] detectLines(byte[] img, int width, int height, boolean needRotation);
}
