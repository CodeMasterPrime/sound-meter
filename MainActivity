package com.example.soundmeter;

import android.Manifest;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Vibrator;
import android.os.VibrationEffect;
import android.content.Context;
import android.view.View;
import android.widget.*;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

public class MainActivity extends AppCompatActivity {

    Button startBtn, btnReport;

    TextView txtDb, txtMin, txtMax, txtAvg, startText;

    ProgressBar progress;
    ImageView arrow;
    View indicator;

    boolean isRecording = false;

    ObjectAnimator blinkAnim;

    Handler uiHandler = new Handler();

    double minDb = 999;
    double maxDb = 0;
    double sumDb = 0;
    int count = 0;

    boolean warning = false;
    SoundRepository repo;
    long lastSaveTime = 0;

    Runnable uiRunnable = new Runnable() {
        @Override
        public void run() {

            double db = SoundService.dbValue;

            long now = System.currentTimeMillis();

            if (now - lastSaveTime >= 1000) {
                repo.insert((float) db);
                lastSaveTime = now;
            }

            txtDb.setText((int) db + " dB");

            float maxWidth =
                    progress.getWidth() - arrow.getWidth();

            float pos =
                    (float) (db / 120f) * maxWidth;

            if (pos < 0) pos = 0;
            if (pos > maxWidth) pos = maxWidth;

            arrow.setTranslationX(pos);


            if (db > 0) {

                if (db >= 88) {

                    if (!warning) {

                        warning = true;

                        vibratePhone();

                        String msg = "";

                        if (db >= 88 && db <= 93) {

                            msg = "ไม่ควรรับเสียงนาน";

                        } else if (db >= 94 && db <= 99) {

                            msg = "ควรย้ายออกจากสถานที่";

                        } else if (db >= 100) {

                            msg = "โปรดออกจากสถานที่ทันที";
                        }

                        Toast.makeText(
                                MainActivity.this,
                                msg,
                                Toast.LENGTH_LONG
                        ).show();
                    }

                } else {

                    warning = false;
                }


                if (db < minDb) minDb = db;
                if (db > maxDb) maxDb = db;

                sumDb += db;
                count++;

                double avg = sumDb / count;

                txtMin.setText("MIN\n" + (int) minDb);
                txtMax.setText("MAX\n" + (int) maxDb);
                txtAvg.setText("AVG\n" + (int) avg);
            }

            uiHandler.postDelayed(this, 100);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        startBtn = findViewById(R.id.startBtn);
        btnReport = findViewById(R.id.viewReportBtn);
        repo = new SoundRepository(this);

        txtDb = findViewById(R.id.txtDb);
        txtMin = findViewById(R.id.txtMin);
        txtMax = findViewById(R.id.txtMax);
        txtAvg = findViewById(R.id.txtAvg);

        startText = findViewById(R.id.startText);

        progress = findViewById(R.id.progress);
        arrow = findViewById(R.id.dbArrow);
        indicator = findViewById(R.id.recordingIndicator);


        // ===== START BUTTON =====

        startBtn.setOnClickListener(v -> {

            if (!isRecording) {

                checkPermissionAndStart();

            } else {

                stopServiceSound();
            }
        });


        // ===== REPORT BUTTON =====

        btnReport.setOnClickListener(v -> {

            Intent i =
                    new Intent(
                            MainActivity.this,
                            ReportActivity.class);

            startActivity(i);

        });

    }

    // PERMISSION
    private void checkPermissionAndStart() {

        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.RECORD_AUDIO)
                != PackageManager.PERMISSION_GRANTED) {

            ActivityCompat.requestPermissions(
                    this,
                    new String[]{
                            Manifest.permission.RECORD_AUDIO
                    },
                    1);

        } else {

            startServiceSound();
        }
    }

    // START SERVICE
    private void startServiceSound() {

        Intent i =
                new Intent(this, SoundService.class);

        if (Build.VERSION.SDK_INT >= 26) {

            startForegroundService(i);

        } else {

            startService(i);
        }

        isRecording = true;

        startBtn.setText("STOP");

        startText.setText("Recording...");

        indicator.setVisibility(View.VISIBLE);

        startBlink();

        minDb = 999;
        maxDb = 0;
        sumDb = 0;
        count = 0;

        uiHandler.post(uiRunnable);
    }

    // STOP SERVICE
    private void stopServiceSound() {

        Intent i =
                new Intent(this, SoundService.class);

        stopService(i);

        isRecording = false;

        startBtn.setText("START");

        startText.setText("Click to allow microphone");

        indicator.setVisibility(View.GONE);

        stopBlink();

        uiHandler.removeCallbacks(uiRunnable);
    }

    // BLINK
    private void startBlink() {

        blinkAnim =
                ObjectAnimator.ofFloat(
                        indicator,
                        "alpha",
                        1f,
                        0f);

        blinkAnim.setDuration(500);
        blinkAnim.setRepeatMode(ObjectAnimator.REVERSE);
        blinkAnim.setRepeatCount(
                ObjectAnimator.INFINITE);

        blinkAnim.start();
    }


    private void stopBlink() {

        if (blinkAnim != null) {
            blinkAnim.cancel();
        }
    }

    // VIBRATE
    private void vibratePhone() {

        Vibrator vibrator =
                (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);

        if (vibrator == null) return;

        if (Build.VERSION.SDK_INT >= 26) {

            vibrator.vibrate(
                    VibrationEffect.createOneShot(
                            500,
                            VibrationEffect.DEFAULT_AMPLITUDE
                    )
            );

        } else {

            vibrator.vibrate(500);
        }
    }

}
