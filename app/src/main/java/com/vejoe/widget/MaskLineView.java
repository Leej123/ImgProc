package com.vejoe.widget;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.PathEffect;
import android.graphics.Point;
import android.support.annotation.ColorInt;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by Administrator on 2017/6/30 0030.
 */

public class MaskLineView extends View {
    private static final float DEFAULT_STROKE_WIDTH = 8f;
    private static final float CIRCLE_RADIUS = 20F;
    private static final int TOUCH_POINT_COLOR = Color.parseColor("#1296db");
    private enum PointState {
        LINE_ONE_START,
        LINE_ONE_END,
        LINE_TWO_START,
        LINE_TWO_END,
        LINE_DONE
    }

    private Paint paint;
    private Paint pointPaint;
    private PathEffect pathEffect;
    private Paint detectLinePaint;

    private Point lineOneStartPoint;
    private Point lineOneEndPoint;
    private Point lineTwoStartPoint;
    private Point lineTwoEndPoint;

    private Point touchPoint;
    private PointState pointState;

    private boolean calibrationLine = false;

    private MaskLineDoneListener maskLineDoneListener;

    private List<Line> detectLines;
    private Object lineLock = new Object();

    public MaskLineView(Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MaskLineView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    public void setColor(@ColorInt int color) {
        paint.setColor(color);
    }

    public void setLineStrokeWidth(float lineStrokeWidth) {
        paint.setStrokeWidth(lineStrokeWidth);
    }

    public void setMaskLineDoneListener(MaskLineDoneListener maskLineDoneListener) {
        this.maskLineDoneListener = maskLineDoneListener;
    }

    public void setLineOne(Point startPoint, Point endPoint) {
        lineOneStartPoint.set(startPoint.x, startPoint.y);
        lineOneEndPoint.set(endPoint.x, endPoint.y);
    }

    public void setLineTwo(Point startPoint, Point endPoint) {
        lineTwoStartPoint.set(startPoint.x, startPoint.y);
        lineTwoEndPoint.set(endPoint.x, endPoint.y);
    }

    public void clearLines() {
        lineOneStartPoint.set(-1, -1);
        lineOneEndPoint.set(-1, -1);
        lineTwoStartPoint.set(-1, -1);
        lineTwoEndPoint.set(-1, -1);
        invalidate();
    }

    public void getLines(@NonNull Point startPoint1, @NonNull Point endPoint1,
                         @NonNull Point startPoint2, @NonNull Point endPoint2) {
        startPoint1.set(lineOneStartPoint.x, lineOneStartPoint.y);
        endPoint1.set(lineOneEndPoint.x, lineOneEndPoint.y);

        startPoint2.set(lineTwoStartPoint.x, lineTwoStartPoint.y);
        endPoint2.set(lineTwoEndPoint.x, lineTwoEndPoint.y);
    }

    public void undo() {
        switch (pointState) {
            case LINE_DONE:
                pointState = PointState.LINE_TWO_END;
                break;
            case LINE_TWO_END:
                pointState = PointState.LINE_TWO_START;
                break;
            case LINE_TWO_START:
                pointState = PointState.LINE_ONE_END;
                break;
            case LINE_ONE_END:
                pointState = PointState.LINE_ONE_START;
                break;
            case LINE_ONE_START:
                touchPoint.set(-1, -1);
                break;
        }

        if (maskLineDoneListener != null) {
            maskLineDoneListener.onMaskLineDone(false);
        }

        invalidate();
    }

    public void setCalibrationLine(boolean calibration) {
        this.calibrationLine = calibration;
        invalidate();
    }

    public void setDetectLines(List<Line> lines) {
        synchronized (lineLock) {
            if (detectLines == null) {
                detectLines = new ArrayList<>();
            }
            detectLines.clear();
            detectLines.addAll(lines);
            postInvalidate();
        }
    }

    public void setDetectLines(int[] points, int width, int height) {
        synchronized (lineLock) {
            if (detectLines == null) {
                detectLines = new ArrayList<>();
            }

            int w = getWidth();
            int h = getHeight();

            float ratioX = (float) w / width;
            float ratioY = (float) h / height;

            float ratio = Math.max(ratioX, ratioY);

            detectLines.clear();

            for (int i = 0; i < points.length; i += 4) {
                Point start = new Point((int) (points[i] * ratio), (int) (points[i + 1] * ratio));
                Point end = new Point((int) (points[i + 2] * ratio), (int) (points[i + 3] * ratio));
                Line line = new Line(start, end);
                detectLines.add(line);
            }

            if (detectLines.size() > 0) {
                postInvalidate();
            }
        }
    }

    public void clearDetectLines() {
        if (detectLines != null) {
            synchronized (lineLock) {
                detectLines.clear();
                postInvalidate();
            }
        }
    }

    private void init() {
        setLayerType(LAYER_TYPE_SOFTWARE, null);
        setBackgroundColor(Color.TRANSPARENT);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        setColor(Color.WHITE);
        setLineStrokeWidth(DEFAULT_STROKE_WIDTH);
        pathEffect = new DashPathEffect(new float[]{8, 16}, 0);

        lineOneStartPoint = new Point(-1, -1);
        lineOneEndPoint = new Point(-1, -1);
        lineTwoStartPoint = new Point(-1, -1);
        lineTwoEndPoint = new Point(-1, -1);
        touchPoint = new Point(-1, -1);

        pointPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        pointPaint.setStyle(Paint.Style.FILL);

        pointState = PointState.LINE_ONE_START;

        detectLinePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        detectLinePaint.setStyle(Paint.Style.STROKE);
        detectLinePaint.setStrokeWidth(DEFAULT_STROKE_WIDTH);
        detectLinePaint.setColor(Color.BLUE);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (detectLines != null) {
            synchronized (lineLock) {
                if (detectLines.size() > 0) {
                    for (Line line : detectLines) {
                        canvas.drawLine(line.startPoint.x, line.startPoint.y, line.endPoint.x, line.endPoint.y, detectLinePaint);
                    }
                }
            }
        }

        if (calibrationLine) {
            if (pointState == PointState.LINE_DONE) {
                canvas.drawLine(lineOneStartPoint.x, lineOneStartPoint.y, lineOneEndPoint.x, lineOneEndPoint.y, paint);
                canvas.drawLine(lineTwoStartPoint.x, lineTwoStartPoint.y, lineTwoEndPoint.x, lineTwoEndPoint.y, paint);
            } else if (pointState == PointState.LINE_TWO_START || pointState == PointState.LINE_TWO_END) {
                canvas.drawLine(lineOneStartPoint.x, lineOneStartPoint.y, lineOneEndPoint.x, lineOneEndPoint.y, paint);

                if (touchPoint.x >= 0 && touchPoint.y >= 0) {
                    pointPaint.setColor(TOUCH_POINT_COLOR);
                    canvas.drawCircle(touchPoint.x, touchPoint.y, CIRCLE_RADIUS, pointPaint);
                }

                boolean end = pointState == PointState.LINE_TWO_END;
                if (end) {
                    pointPaint.setColor(Color.RED);
                    canvas.drawCircle(lineTwoStartPoint.x, lineTwoStartPoint.y, CIRCLE_RADIUS, pointPaint);

                    if (touchPoint.x >= 0 && touchPoint.y >= 0) {
                        paint.setPathEffect(pathEffect);
                        canvas.drawLine(lineTwoStartPoint.x, lineTwoStartPoint.y, touchPoint.x, touchPoint.y, paint);
                        paint.setPathEffect(null);
                    }
                }
            } else {
                if (touchPoint.x >= 0 && touchPoint.y >= 0) {
                    pointPaint.setColor(TOUCH_POINT_COLOR);
                    canvas.drawCircle(touchPoint.x, touchPoint.y, CIRCLE_RADIUS, pointPaint);
                }

                boolean end = pointState == PointState.LINE_ONE_END;
                if (end) {
                    pointPaint.setColor(Color.RED);
                    canvas.drawCircle(lineOneStartPoint.x, lineOneStartPoint.y, CIRCLE_RADIUS, pointPaint);

                    if (touchPoint.x >= 0 && touchPoint.y >= 0) {
                        paint.setPathEffect(pathEffect);
                        canvas.drawLine(lineOneStartPoint.x, lineOneStartPoint.y, touchPoint.x, touchPoint.y, paint);
                        paint.setPathEffect(null);
                    }
                }
            }
        } else {
            canvas.drawLine(lineOneStartPoint.x, lineOneStartPoint.y, lineOneEndPoint.x, lineOneEndPoint.y, paint);
            canvas.drawLine(lineTwoStartPoint.x, lineTwoStartPoint.y, lineTwoEndPoint.x, lineTwoEndPoint.y, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (!calibrationLine) return super.onTouchEvent(event);

        if (event.getAction() != MotionEvent.ACTION_CANCEL && pointState != PointState.LINE_DONE) {
            touchPoint.x = (int) event.getX();
            touchPoint.y = (int) event.getY();

            if (MotionEvent.ACTION_UP == event.getAction()) {
                setLinePoint();
                touchPoint.set(-1, -1);
                if (maskLineDoneListener != null && pointState == PointState.LINE_DONE) {
                    maskLineDoneListener.onMaskLineDone(true);
                }
            }

            invalidate();
            return true;
        }

        return super.onTouchEvent(event);
    }

    private void setLinePoint() {
        switch (pointState) {
            case LINE_ONE_START:
                lineOneStartPoint.x = touchPoint.x;
                lineOneStartPoint.y = touchPoint.y;
                pointState = PointState.LINE_ONE_END;
                break;
            case LINE_ONE_END:
                lineOneEndPoint.x = touchPoint.x;
                lineOneEndPoint.y = touchPoint.y;
                pointState = PointState.LINE_TWO_START;
                break;
            case LINE_TWO_START:
                lineTwoStartPoint.x = touchPoint.x;
                lineTwoStartPoint.y = touchPoint.y;
                pointState = PointState.LINE_TWO_END;
                break;
            case LINE_TWO_END:
                lineTwoEndPoint.x = touchPoint.x;
                lineTwoEndPoint.y = touchPoint.y;
                pointState = PointState.LINE_DONE;
                break;
        }
    }

    private int getInverseColor(int color) {
        int r = 255 - ((color >> 16) & 0x0000FF);
        int g = 255 - ((color >> 8) & 0x0000FF);
        int b = 255 - (color & 0x0000FF);
        return Color.rgb(r, g, b);
    }

    public interface MaskLineDoneListener {
        void onMaskLineDone(boolean done);
    }
}
