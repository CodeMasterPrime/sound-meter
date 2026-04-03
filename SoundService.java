package com.example.soundmeter;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.Service;

import android.content.Intent;
import android.content.pm.PackageManager;

import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaRecorder;

import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.content.Context;
import android.app.PendingIntent;

import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

public class SoundService extends Service {

    public static double dbValue = 0;

    AudioRecord recorder;
    Handler handler;

    boolean isRecording = false;

    int sampleRate = 44100;
    int bufferSize;

    long lastWarningTime = 0;

    SoundRepository repository;
    long lastSaveTime = 0;

    @Override
    public void onCreate() {
        super.onCreate();

        handler = new Handler(Looper.getMainLooper());

        bufferSize = AudioRecord.getMinBufferSize(
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT
        );

        bufferSize = bufferSize / 2;
        if (bufferSize < 2048) bufferSize = 2048;

        repository = new SoundRepository(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        startForeground(1, createNotification());

        startRecorder();

        return START_STICKY;
    }

    private void startRecorder() {

        if (ActivityCompat.checkSelfPermission(
                this,
                android.Manifest.permission.RECORD_AUDIO
        ) != PackageManager.PERMISSION_GRANTED) {

            stopSelf();
            return;
        }

        try {

            recorder = new AudioRecord(
                    MediaRecorder.AudioSource.MIC,
                    sampleRate,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                    bufferSize
            );

            if (recorder.getState() != AudioRecord.STATE_INITIALIZED) {
                stopSelf();
                return;
            }

            recorder.startRecording();

            isRecording = true;

            handler.post(updateRunnable);

        } catch (Exception e) {
            e.printStackTrace();
            stopSelf();
        }
    }

    private Runnable updateRunnable = new Runnable() {
        @Override
        public void run() {

            if (!isRecording || recorder == null) return;

            short[] buffer = new short[bufferSize];

            int read = recorder.read(buffer, 0, bufferSize);

            if (read > 0) {

                double sum = 0;

                for (int i = 0; i < read; i++) {
                    sum += buffer[i] * buffer[i];
                }

                double rms = Math.sqrt(sum / read);

                double ref = 32768.0;
                double db = 20.0 * Math.log10(rms / ref);

                db += 85;

                if (db < 0) db = 0;
                if (db > 120) db = 120;

                dbValue = db;

                long now = System.currentTimeMillis();

                if (now - lastSaveTime > 1000) {
                    repository.insert((float) dbValue);
                    lastSaveTime = now;
                }

                if (dbValue >= 88) {
                    showWarningNotification(dbValue);
                }
            }

            handler.postDelayed(this, 100);
        }
    };

    private void showWarningNotification(double db) {

        long now = System.currentTimeMillis();

        if (now - lastWarningTime < 3000) return;

        lastWarningTime = now;

        NotificationManager nm =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        String channelId = "sound_warning";

        if (android.os.Build.VERSION.SDK_INT >= 26) {

            NotificationChannel channel =
                    new NotificationChannel(
                            channelId,
                            "Sound Warning",
                            NotificationManager.IMPORTANCE_HIGH
                    );

            nm.createNotificationChannel(channel);
        }

        Intent i = new Intent(this, MainActivity.class);

        PendingIntent pi =
                PendingIntent.getActivity(
                        this,
                        0,
                        i,
                        PendingIntent.FLAG_IMMUTABLE
                );

        Notification n =
                new NotificationCompat.Builder(this, channelId)
                        .setContentTitle("⚠ เสียงดังเกิน 88 dB")
                        .setContentText("กรุณาออกจากพื้นที่นี้")
                        .setSmallIcon(R.mipmap.ic_launcher)
                        .setContentIntent(pi)
                        .setPriority(NotificationCompat.PRIORITY_HIGH)
                        .build();

        nm.notify((int) now, n);

        Vibrator v =
                (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (v != null) {

            if (android.os.Build.VERSION.SDK_INT >= 26) {

                v.vibrate(
                        VibrationEffect.createOneShot(
                                500,
                                VibrationEffect.DEFAULT_AMPLITUDE
                        )
                );

            } else {

                v.vibrate(500);
            }
        }
    }

    private Notification createNotification() {

        String chId = "sound_channel";

        NotificationChannel ch =
                new NotificationChannel(
                        chId,
                        "SoundMeter",
                        NotificationManager.IMPORTANCE_LOW
                );

        NotificationManager nm =
                getSystemService(NotificationManager.class);

        if (nm != null) {
            nm.createNotificationChannel(ch);
        }

        return new NotificationCompat.Builder(this, chId)
                .setContentTitle("SoundMeter running")
                .setContentText("Measuring sound...")
                .setSmallIcon(R.mipmap.ic_launcher)
                .build();
    }

    @Override
    public void onDestroy() {

        isRecording = false;

        handler.removeCallbacks(updateRunnable);

        if (recorder != null) {

            try {
                recorder.stop();
            } catch (Exception e) {}

            recorder.release();
            recorder = null;
        }

        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
