package com.mesutarslan.kayitcalar;

import android.Manifest;
import android.content.Intent;
import android.media.MediaPlayer;
import android.media.MediaRecorder;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import java.io.File;

public class MainActivity extends AppCompatActivity {
    private MediaPlayer mediaPlayer;
    private MediaRecorder mediaRecorder;
    private SurfaceView surfaceView;
    private Button btnSelect, btnRecord;
    private SeekBar videoSeekBar;
    private TextView tvTime;
    private Uri pendingUri;
    private Handler handler = new Handler();
    private boolean isRecording = false;
    private String filePath;
    private int resumePosition = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 200);
        }

        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                handleExternalFile(data);
            }
        }




        surfaceView = findViewById(R.id.surfaceView);
        btnSelect = findViewById(R.id.btn_select);
        videoSeekBar = findViewById(R.id.videoSeekBar);
        btnRecord = findViewById(R.id.btn_record);
        tvTime = findViewById(R.id.tv_time);



        SeekBar volumeSeekBar = findViewById(R.id.volumeSeekBar);

        volumeSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    // Servise ses seviyesini bildir
                    Intent intent = new Intent(MainActivity.this, MusicService.class);
                    intent.setAction("SET_VOLUME");
                    intent.putExtra("VOLUME_LEVEL", progress);
                    startService(intent);
                }
            }
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });





        videoSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser && mediaPlayer != null) {
                    mediaPlayer.seekTo(progress);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Sürükleme başladığında videoyu duraklat (daha kararlı çalışması için)
                if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                    mediaPlayer.pause();
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Sürükleme bittiğinde videoyu oynatmaya devam et
                if (mediaPlayer != null) {
                    mediaPlayer.start();
                }
            }
        });

        btnRecord.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isRecording) {
                    File downloadsDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
                    filePath = downloadsDir.getAbsolutePath() + "/kayit_" + System.currentTimeMillis() + ".aac";
                    mediaRecorder = new MediaRecorder();
                    mediaRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
                    mediaRecorder.setOutputFormat(MediaRecorder.OutputFormat.MPEG_4);
                    mediaRecorder.setAudioEncoder(MediaRecorder.AudioEncoder.AAC);
                    mediaRecorder.setOutputFile(filePath);
                    try {
                        mediaRecorder.prepare();
                        mediaRecorder.start();
                        isRecording = true;
                        btnRecord.setText("Kayit yapiliyor...");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else {
                    try {
                        mediaRecorder.stop();
                        mediaRecorder.release();
                        mediaRecorder = null;
                        isRecording = false;
                        btnRecord.setText("Kayit Bitti: " + filePath);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        });


        surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override public void surfaceCreated(SurfaceHolder holder) {
                // Uygulama geri açıldığında mediaPlayer'ı yeni yüzeye bağla
                if (mediaPlayer != null) {
                    mediaPlayer.setDisplay(holder);
                }
                if (pendingUri != null) { playVideo(pendingUri); pendingUri = null; }
            }

            @Override public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {}

            @Override public void surfaceDestroyed(SurfaceHolder holder) {
                // release() işlemini burada yaparsan uygulama aşağı inince video tamamen silinir.
                // Bu yüzden burayı boş bırak veya mediaPlayer'ı burada release etme.
                // Release işlemini sadece onDestroy içinde yapmalısın.
            }
        });






        btnSelect.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("*/*");
                intent.putExtra(Intent.EXTRA_MIME_TYPES, new String[]{"audio/*", "video/*"});
                MainActivity.this.startActivityForResult(Intent.createChooser(intent, "Dosya Sec"), 100);
            }
        });
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mediaPlayer != null && mediaPlayer.isPlaying()) {
            resumePosition = mediaPlayer.getCurrentPosition();
            mediaPlayer.pause();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mediaPlayer != null && resumePosition > 0) {
            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }



    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {
            if (mediaPlayer != null && mediaPlayer.isPlaying()) {
                int cur = mediaPlayer.getCurrentPosition();
                int tot = mediaPlayer.getDuration();
                videoSeekBar.setProgress(cur);
                tvTime.setText(String.format("%02d:%02d / %02d:%02d", (cur/1000)/60, (cur/1000)%60, (tot/1000)/60, (tot/1000)%60));
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            String mime = getContentResolver().getType(uri);
            String path = uri.toString();
            String ext = path.contains(".") ? path.substring(path.lastIndexOf(".") + 1).toLowerCase() : "";

            if (ext.equals("wma")) {
                Toast.makeText(this, "WMA formatı desteklenmiyor!", Toast.LENGTH_SHORT).show();
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=wma+mp4+donustur")));
            } else if (mime != null && mime.startsWith("video/")) {
                pendingUri = uri;
                if (surfaceView.getHolder().getSurface() != null && surfaceView.getHolder().getSurface().isValid()) playVideo(uri);
            } else if (mime != null && mime.startsWith("audio/")) {
                Intent s = new Intent(this, MusicService.class);
                s.setData(uri);
                startService(s);
            } else {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com/search?q=" + Uri.encode(ext + " mp4 donustur"))));
            }
        }
    }


    private void handleExternalFile(Uri uri) {
        // 1. Dosya yolu üzerinden uzantı kontrolü (WMA gibi sorunlu dosyalar için)
        String path = uri.toString().toLowerCase();
        if (path.endsWith(".wma")) {
            Toast.makeText(this, "Bu format (WMA) desteklenmiyor.", Toast.LENGTH_LONG).show();
            return;
        }

        // 2. MIME tipine göre standart işlem
        String mime = getContentResolver().getType(uri);

        if (mime != null && mime.startsWith("video/")) {
            pendingUri = uri;
            if (surfaceView != null && surfaceView.getHolder().getSurface() != null && surfaceView.getHolder().getSurface().isValid()) {
                playVideo(uri);
            }
        } else if (mime != null && mime.startsWith("audio/")) {
            Intent s = new Intent(this, MusicService.class);
            s.setData(uri);
            startService(s);
        } else {
            // 3. Hiçbiri değilse uyarı ver
            Toast.makeText(this, "Bu dosya formatı desteklenmiyor.", Toast.LENGTH_SHORT).show();
        }
    }






    private void playVideo(Uri uri) {
        try {
            if (mediaPlayer != null) mediaPlayer.release();
            mediaPlayer = new MediaPlayer();
            mediaPlayer.setDataSource(this, uri);
            mediaPlayer.setDisplay(surfaceView.getHolder());

            mediaPlayer.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    mp.seekTo(0);
                    videoSeekBar.setProgress(0);
                    mp.pause();
                }
            });

            mediaPlayer.prepareAsync();
            mediaPlayer.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mp) {
                    mp.start();
                    videoSeekBar.setMax(mp.getDuration());
                    handler.postDelayed(updateRunnable, 1000);
                }
            });
        } catch (Exception e) { e.printStackTrace(); }
    }
}