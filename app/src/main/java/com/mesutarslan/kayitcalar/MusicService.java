package com.mesutarslan.kayitcalar;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
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

    private Runnable restartRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null) {
                mediaPlayer.seekTo(0);
                mediaPlayer.start();
                handler.postDelayed(this, 60000);
            }
        }
    };

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent.getAction() != null && intent.getAction().equals("STOP_ACTION")) {
            stopSelf();
            return START_NOT_STICKY;
        }

        if (mediaPlayer != null) {
            mediaPlayer.release();
        }

        Uri uri = intent.getData();
        mediaPlayer = MediaPlayer.create(this, uri);
        mediaPlayer.start();

        // 60 saniye sonra başa sarma zamanlayıcısını başlat
        handler.removeCallbacks(restartRunnable);
        handler.postDelayed(restartRunnable, 60000);

        createNotificationChannel();

        Intent stopIntent = new Intent(this, MusicService.class);
        stopIntent.setAction("STOP_ACTION");
        PendingIntent stopPendingIntent = PendingIntent.getService(this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE);

        Notification notification = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Müzik Çalıyor")
                .setContentText("1 dakikada bir tekrarlanıyor")
                .setSmallIcon(R.mipmap.ic_launcher)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Durdur", stopPendingIntent)
                .build();

        startForeground(1, notification);
        return START_STICKY;
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(CHANNEL_ID, "Music Service Channel", NotificationManager.IMPORTANCE_DEFAULT);
            getSystemService(NotificationManager.class).createNotificationChannel(serviceChannel);
        }
    }

    @Override
    public void onDestroy() {
        handler.removeCallbacks(restartRunnable);
        if (mediaPlayer != null) {
            mediaPlayer.stop();
            mediaPlayer.release();
        }
        stopForeground(true);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}