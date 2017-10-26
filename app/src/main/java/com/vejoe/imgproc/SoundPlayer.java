package com.vejoe.imgproc;

import android.content.Context;
import android.media.MediaPlayer;
import android.media.RingtoneManager;
import android.net.Uri;

import java.io.IOException;

/**
 * Created by Administrator on 2017/6/16 0016.
 */

public class SoundPlayer implements MediaPlayer.OnCompletionListener {
    private MediaPlayer mediaPlayer;
    private boolean isCompleted = false;
    private boolean uesSytemRingtone = false;

    public void init(Context context) {
        release();
        mediaPlayer = MediaPlayer.create(context, R.raw.warning);
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setLooping(false);
        uesSytemRingtone = false;
    }

    public void initWithSystemDefaultRingtone(Context context) {
        release();
        mediaPlayer = MediaPlayer.create(context, getSystemDefaultRingtoneUri(context));
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setLooping(false);
        uesSytemRingtone = true;
    }

    public boolean isUesSytemRingtone() {
        return uesSytemRingtone;
    }

    private Uri getSystemDefaultRingtoneUri(Context context) {
        return RingtoneManager.getActualDefaultRingtoneUri(context, RingtoneManager.TYPE_RINGTONE);
    }

    public void play() {
        if (mediaPlayer != null) {
            if (!isCompleted) {
                mediaPlayer.start();
            } else {
                isCompleted = false;
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
            }
        }
    }

    public void pause() {
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            mediaPlayer.pause();
        }
    }

    public void resume() {
        if (mediaPlayer != null) {
            if (!isCompleted && mediaPlayer.isPlaying())
                mediaPlayer.start();
        }
    }

    public void release() {
        if (mediaPlayer != null) {
            if (mediaPlayer.isPlaying()) mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        uesSytemRingtone = false;
    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        isCompleted = true;
    }
}
