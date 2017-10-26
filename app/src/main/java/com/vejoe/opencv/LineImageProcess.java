package com.vejoe.opencv;

import android.graphics.Point;
import android.graphics.Rect;

import com.vejoe.widget.Line;

import java.util.ArrayList;
import java.util.List;

/**
 * 检测直线
 * Created by Administrator on 2017/7/20 0020.
 */

public class LineImageProcess {
    private static final int AREA_MARGIN = 20;
    private static final float WARNING_ANGLE = 30;
    private static final float PROPORTION = 0.90F;

    private OpenCV openCV;
    private Thread processThread = null;
    private boolean isEnd = true;

    private Object obj = new Object();
    private boolean newFrame = false;
    private byte[] data = null;
    private byte[] buffer = null;
    private int width;
    private int height;

    private OnProcessListener processListener;

    // 标定的直线
    private Line lineOne;
    private Line lineTwo;
    // 标定的直线视图大小，即标定执行所处的坐标空间
    private int calibrationViewWidth;
    private int calibrationViewHeight;
    // 检测到的直线线段
    private List<Line> detectLines;
    // 标定直线的区间，以线段的两端点确定一个区域
    private Rect lineOneArea;
    private Rect lineTwoArea;
    // 检测区间
    private Rect detectArea;
    // 落入到标定直线区间中线段
    private List<Line> lineInAreaOne;
    private List<Line> lineInAreaTwo;

    private boolean needRotation = true;

    public LineImageProcess() {
        openCV = new OpenCV();
    }

    public void setOnProcessListener(OnProcessListener processListener) {
        this.processListener = processListener;
    }

    /**
     * 设置标定的直线
     * @param lineOne 直线1
     * @param lineTwo 直线2
     * @param width 标定直线时视图的宽度
     * @param height 标定直线时视图的高度
     */
    public void setCalibrationParams(Line lineOne, Line lineTwo, int width, int height) {
        this.lineOne = lineOne;
        this.lineTwo = lineTwo;
        this.calibrationViewWidth = width;
        this.calibrationViewHeight = height;

        if (lineOneArea == null)
            lineOneArea = new Rect();
        setLineArea(lineOne, lineOneArea);

        if (lineTwoArea == null)
            lineTwoArea = new Rect();
        setLineArea(lineTwo, lineTwoArea);

        if (detectArea == null) {
            detectArea = new Rect();
        }
        detectArea.left = 0;
        detectArea.right = width;
        detectArea.top = height / 2;
        detectArea.bottom = height;
    }

    public void processFrame(byte[] data, int width, int height, boolean needRotation) {
        synchronized (obj) {
            if (buffer == null || buffer.length != data.length) {
                buffer = null;
                buffer = new byte[data.length];
            }
            System.arraycopy(data, 0, buffer, 0, data.length);
            this.width = width;
            this.height = height;
            newFrame = true;
            this.needRotation = needRotation;
        }
    }

    public boolean isStart() {
        return processThread != null;
    }

    public void start() {
        if (processThread != null) return;
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

    private void process() {
        boolean handle;
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
                // 调用底层opencv检测线段
                int[] points = openCV.detectLines(data, width, height, needRotation);
                detectLines(points);
                if (processListener != null) {
                    // 将检测到的线段返回给调用者，调用者将直线绘制到界面显示
                    processListener.onProcess(detectLines, height, width);// 高宽互调
                }
                if (lineOne != null && lineTwo != null && detectLines.size() > 0) {
                    processCalibrationLines();
                }
            }
        }
    }

    /**
     * 获取检测到的直线
     * @param points
     */
    private void detectLines(int[] points) {
        if (detectLines == null) {
            detectLines = new ArrayList<>();
        }
        detectLines.clear();

        if (points == null) {
            return;
        }

        float ratioX = (float) calibrationViewWidth / height; //高宽互调
        float ratioY = (float) calibrationViewHeight / width;

        float ratio = Math.max(ratioX, ratioY);

        //List<Line> temp = new ArrayList<>();
        for (int i = 0; i < points.length; i += 4) {
            Point start = new Point((int) (points[i] * ratio), (int) (points[i + 1] * ratio));
            Point end = new Point((int) (points[i + 2] * ratio), (int) (points[i + 3] * ratio));
            Line line = new Line(start, end);
            detectLines.add(line);
        }

//        if (temp.size() > 0) {
//            calculateLinesInArea(detectLines, temp, detectArea);
//        }
    }

    /**
     * 计算标定直线与检测直线的夹角。
     */
    private void processCalibrationLines() {
        if (lineInAreaOne == null) {
            lineInAreaOne = new ArrayList<>();
        }

        if (lineInAreaTwo == null) {
            lineInAreaTwo = new ArrayList<>();
        }

        List<Line> temp = new ArrayList<>();
        temp.addAll(detectLines);

        // 计算落入标定直线区域的线段
        calculateLinesInArea(lineInAreaOne, temp, lineOneArea);
        calculateLinesInArea(lineInAreaTwo, temp, lineTwoArea);

        boolean warning = false;
        if (lineInAreaOne.size() > 0) {
            // 计算夹角
            warning = calculateAngles(lineInAreaOne, lineOne);
        }

        if (lineInAreaTwo.size() > 0) {
            // 计算夹角
            warning = calculateAngles(lineInAreaTwo, lineTwo);
        }

        if (warning) {
            if (processListener != null) {
                processListener.onWarning();// 发出警报
            }
        }
    }

    private boolean calculateAngles(List<Line> lineInArea, Line calibrationLine) {
        int count = 0;

        for (Line line : lineInArea) {
            float theta;
            if (calibrationLine.startPoint.x == calibrationLine.endPoint.x) { // 标定线竖直
                if (line.startPoint.x == line.endPoint.x) {
                    theta = 90;
                } else {
                    float k = Math.abs((line.startPoint.y - line.endPoint.y) / (line.startPoint.x - line.endPoint.x));
                    theta = 90 - (float) Math.toDegrees(Math.atan(k));
                }
            } else {
                if (line.startPoint.x == line.endPoint.x) {
                    float k = Math.abs((calibrationLine.startPoint.y - calibrationLine.endPoint.y) / (calibrationLine.startPoint.x - calibrationLine.endPoint.x));
                    theta = 90 - (float) Math.toDegrees(Math.atan(k));
                } else {
                    float k1 = Math.abs((calibrationLine.startPoint.y - calibrationLine.endPoint.y) / (calibrationLine.startPoint.x - calibrationLine.endPoint.x));
                    float k2 = Math.abs((line.startPoint.y - line.endPoint.y) / (line.startPoint.x - line.endPoint.x));
                    float value = Math.abs((k2 - k1) / (1 + k1 * k2));
                    theta = (float) Math.toDegrees(Math.atan(value));
                }
            }

            if (theta >= WARNING_ANGLE) {
                count ++;
            }
        }

        float ratio = (float) count / lineInArea.size();
        return ratio >= PROPORTION? true : false;
    }

    /**
     * 计算落入区域的线段
     * @param lineInArea 保存区域中的线段
     * @param lines 需要检查的线段
     * @param area 指定的区域
     */
    private void calculateLinesInArea(List<Line> lineInArea, List<Line> lines, Rect area) {
        lineInArea.clear();

        List<Line> removedLines = new ArrayList<>();

        for (Line line : lines) {
            Point startPoint = line.startPoint;
            Point endPoint = line.endPoint;

            // 区域包含线段的任意一个端点
            if (area.contains(startPoint.x, startPoint.y) || area.contains(endPoint.x, endPoint.y)) {
                lineInArea.add(new Line(new Point(startPoint), new Point(endPoint)));
                removedLines.add(line);
                continue;
            }

            // 线段不存在区域
            if ((startPoint.x < area.left && endPoint.x < area.left)
                    || (startPoint.x > area.right && endPoint.x > area.right)
                    || (startPoint.y < area.top && endPoint.y < area.top)
                    || (startPoint.y > area.bottom && endPoint.y > area.bottom)) {

                continue;
            }

            // 左右或上下穿透区域
            if ((startPoint.x < area.left && endPoint.x > area.right) || (endPoint.x < area.left && startPoint.x > area.right) //左右
                    ||(startPoint.y < area.top && endPoint.y > area.bottom) || (endPoint.y < area.top && startPoint.y > area.bottom)) {

                lineInArea.add(new Line(new Point(startPoint), new Point(endPoint)));
                removedLines.add(line);
                continue;
            }

            // 剩下的情况，不存在斜率为0或者无穷大的线段
            float k = (endPoint.y - startPoint.y) / (endPoint.x - startPoint.x);
            float b = startPoint.y - k * startPoint.x;

            float y = k * area.left + b;
            float y2 = k * area.right + b;

            if (k < 0) {
                if (y >= area.top && y2 <= area.bottom) {
                    lineInArea.add(line);
                    removedLines.add(line);
                }
            } else {
                if (y >= area.bottom && y2 <= area.top) {
                    lineInArea.add(line);
                    removedLines.add(line);
                }
            }
        }

        if (removedLines.size() > 0) {
            lines.removeAll(removedLines);
        }
    }

    private void setLineArea(Line line, Rect area) {
        if (line == null) {
            area.left = 0;
            area.right = 0;
            area.bottom = 0;
            area.top = 0;
            return;
        }

        Point startPoint = line.startPoint;
        Point endPoint = line.endPoint;

        area.left = startPoint.x <= endPoint.x? (startPoint.x - AREA_MARGIN) : (endPoint.x - AREA_MARGIN);
        area.right = startPoint.x <= endPoint.x? (endPoint.x + AREA_MARGIN) : (startPoint.x + AREA_MARGIN);
        area.top = startPoint.y <= endPoint.y? (startPoint.y - AREA_MARGIN) : (endPoint.y - AREA_MARGIN);
        area.bottom = startPoint.y <= endPoint.y? (endPoint.y + AREA_MARGIN) : (startPoint.y + AREA_MARGIN);
    }

    public interface OnProcessListener {
        void onProcess(Object result, int with, int height);
        void onWarning();
    }
}
