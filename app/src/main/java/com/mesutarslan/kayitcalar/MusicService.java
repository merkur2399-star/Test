package com.mesutarslan.kayitcalar;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

import androidx.core.app.NotificationCompat;

public class MusicService extends Service {
    private MediaPlayer mediaPlayer;
    private static final String CHANNEL_ID = "MusicServiceChannel";
    private Handler handler = new Handler(Looper.getMainLooper());
    private long targetRestartTime = 0;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) return START_STICKY;

        String action = intent.getAction();

        if ("STOP_ACTION".equals(action)) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if ("SET_VOLUME".equals(action)) {
            int progress = intent.getIntExtra("VOLUME_LEVEL", 50);
            setSystemMediaVolume(progress);
            return START_STICKY;
        }

        // --- Dışarıdan gelen URI kontrolü ---
        Uri uri = intent.getData();
        if (uri != null) {
            if (mediaPlayer != null) {
                mediaPlayer.release();
            }

            mediaPlayer = MediaPlayer.create(this, uri);

            if (mediaPlayer != null) {
                mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        targetRestartTime = System.currentTimeMillis() + 60000;
                        MusicService.this.checkAndRestart();
                    }
                });
                mediaPlayer.start();
            }
        }
        // ------------------------------------

        createNotificationChannel();

        Intent stopIntent = new Intent(this, MusicService.class);
        stopIntent.setAction("STOP_ACTION");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Müzik Çalıyor")
                .setContentText("1 dakika kuralı aktif.")
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Durdur", stopPendingIntent)
                .build();

        startForeground(1, notification);
        return START_STICKY;
    }

    private void setSystemMediaVolume(int progress) {
        AudioManager audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null) {
            int maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            int volumeToSet = (int) ((progress / 100.0) * maxVolume);
            audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volumeToSet, 0);
        }
    }

    private void checkAndRestart() {
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                long currentTime = System.currentTimeMillis();
                if (currentTime >= targetRestartTime) {
                    if (mediaPlayer != null) {
                        mediaPlayer.seekTo(0);
                        mediaPlayer.start();
                    }
                } else {
                    checkAndRestart();
                }
            }
        }, 1000);
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Music Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
            getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacksAndMessages(null);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
            mediaPlayer = null;
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}