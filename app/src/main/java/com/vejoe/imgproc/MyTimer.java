package com.vejoe.imgproc;

import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Color;
import android.os.CountDownTimer;
import android.support.v7.app.AlertDialog;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import com.vejoe.widget.TimePickerView;

import java.util.ArrayList;
import java.util.List;

/**
 * <p>Author: Leej
 * <p>Company: 深圳维周
 * <p>Comment: //TODO
 * <p>Date: 2017/10/13 0013-下午 6:46
 */

public class MyTimer {
    private Context context;
    private CountDownTimer countDownTimer = null;
    private int selectHour = 0;
    private int selectMinute = 0;
    private TextView textView = null;

    public MyTimer(Context context) {
        this.context = context;
    }

    public void displayOnView(TextView textView) {
        this.textView = textView;
    }

    public void start() {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.pick_time_layout, null);

        TimePickerView hourPicker = (TimePickerView) view.findViewById(R.id.hour_pv);
        TimePickerView minutePicker = (TimePickerView) view.findViewById(R.id.minute_pv);
        List<String> hours = new ArrayList<>();
        for (int i = 0; i <= 23; i ++) {
            hours.add(i < 10? "0" + i : String.valueOf(i));
        }
        hourPicker.setData(hours);
        hourPicker.setSelected(0);
        hourPicker.setDataColor(Color.WHITE);
        List<String> minutes = new ArrayList<>();
        for (int i = 0; i <= 59; i ++) {
            minutes.add(i < 10? "0" + i : String.valueOf(i));
        }
        minutePicker.setData(minutes);
        minutePicker.setSelected(0);
        minutePicker.setDataColor(Color.WHITE);

        hourPicker.setOnSelectListener(new TimePickerView.onSelectListener() {
            @Override
            public void onSelect(String text) {
                selectHour = Integer.parseInt(text);
            }
        });

        minutePicker.setOnSelectListener(new TimePickerView.onSelectListener() {
            @Override
            public void onSelect(String text) {
                selectMinute = Integer.parseInt(text);
            }
        });

        builder.setView(view);
        builder.setTitle("选择定时时间");
        builder.setCancelable(true);
        builder.setPositiveButton("确认", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                long millis = (selectHour * 3600 + selectMinute * 60) * 1000;
                if (millis == 0)
                    return;
                stop();
                countDownTimer = new MyCountDownTimer(millis, 1000);
                countDownTimer.start();
            }
        });
        builder.setNegativeButton("取消", null);
        builder.create().show();
    }

    public void stop() {
        if (countDownTimer != null) {
            countDownTimer.cancel();
            countDownTimer = null;
        }
    }

    private class MyCountDownTimer extends CountDownTimer {

        /**
         * @param millisInFuture    The number of millis in the future from the call
         *                          to {@link #start()} until the countdown is done and {@link #onFinish()}
         *                          is called.
         * @param countDownInterval The interval along the way to receive
         *                          {@link #onTick(long)} callbacks.
         */
        public MyCountDownTimer(long millisInFuture, long countDownInterval) {
            super(millisInFuture, countDownInterval);
        }

        @Override
        public void onTick(long millisUntilFinished) {
            if (textView != null) {
                int totalSeconds = (int) (millisUntilFinished / 1000);
                int hour = totalSeconds / 3600;
                int minute = (totalSeconds - hour * 3600) / 60;
                int second = totalSeconds - hour * 3600 - minute * 60;
                textView.setText(String.format("%02d:%02d:%02d", hour, minute, second));
            }
        }

        @Override
        public void onFinish() {
            if (textView != null) {
                textView.setText("");
            }
            AlertDialog.Builder builder = new AlertDialog.Builder(context);
            builder.setTitle("提示");
            builder.setMessage("定时时间到，可以休息一下！");
            builder.setCancelable(true);
            builder.setPositiveButton("确定", null);
            builder.create().show();
        }
    }
}
