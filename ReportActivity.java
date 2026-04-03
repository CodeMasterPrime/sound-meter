package com.example.soundmeter;

import android.graphics.Color;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.widget.*;
import android.os.Environment;
import android.content.Intent;
import android.net.Uri;

import androidx.appcompat.app.AppCompatActivity;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.*;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.Date;
import java.util.Locale;

public class ReportActivity extends AppCompatActivity {

    LineChart chart;

    SoundRepository repo;

    Button btn1min, btn1hour, btn4hour, btnPdf;

    ImageButton btnPrevDay, btnNextDay;

    TextView txtMin, txtMax, txtAvg, txtDate;

    TextView slot1, slot2, slot3, slot4, slot5, slot6;

    TextView slotName1, slotName2, slotName3, slotName4, slotName5, slotName6;

    List<SoundData> filtered = new ArrayList<>();

    long selectedDay = System.currentTimeMillis();

    long currentRange = 60000;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.report_layout);


        chart = findViewById(R.id.lineChart);

        btn1min = findViewById(R.id.btn1min);
        btn1hour = findViewById(R.id.btn1hour);
        btn4hour = findViewById(R.id.btn4hour);
        btnPdf = findViewById(R.id.btnPdf);

        btnPrevDay = findViewById(R.id.btnPrevDay);
        btnNextDay = findViewById(R.id.btnNextDay);

        btnPdf.setOnClickListener(v -> exportPdf());

        txtDate = findViewById(R.id.txtDate);

        txtMin = findViewById(R.id.txtMinReport);
        txtMax = findViewById(R.id.txtMaxReport);
        txtAvg = findViewById(R.id.txtAvgReport);

        slot1 = findViewById(R.id.slot1);
        slot2 = findViewById(R.id.slot2);
        slot3 = findViewById(R.id.slot3);
        slot4 = findViewById(R.id.slot4);
        slot5 = findViewById(R.id.slot5);
        slot6 = findViewById(R.id.slot6);

        slotName1 = findViewById(R.id.slotName1);
        slotName2 = findViewById(R.id.slotName2);
        slotName3 = findViewById(R.id.slotName3);
        slotName4 = findViewById(R.id.slotName4);
        slotName5 = findViewById(R.id.slotName5);
        slotName6 = findViewById(R.id.slotName6);

        repo = new SoundRepository(this);

        btn1min.setOnClickListener(v -> {
            currentRange = 60000;
            load();
        });

        btn1hour.setOnClickListener(v -> {
            currentRange = 3600000;
            load();
        });

        btn4hour.setOnClickListener(v -> {
            currentRange = 14400000;
            load();
        });


        btnPrevDay.setOnClickListener(v -> {
            selectedDay -= 86400000;
            updateDateText();
            load();
        });

        btnNextDay.setOnClickListener(v -> {
            selectedDay += 86400000;
            updateDateText();
            load();
        });

        findViewById(R.id.btnDetail1)
                .setOnClickListener(v -> exportSlotPdf(0,4));

        findViewById(R.id.btnDetail2)
                .setOnClickListener(v -> exportSlotPdf(4,8));

        findViewById(R.id.btnDetail3)
                .setOnClickListener(v -> exportSlotPdf(8,12));

        findViewById(R.id.btnDetail4)
                .setOnClickListener(v -> exportSlotPdf(12,16));

        findViewById(R.id.btnDetail5)
                .setOnClickListener(v -> exportSlotPdf(16,20));

        findViewById(R.id.btnDetail6)
                .setOnClickListener(v -> exportSlotPdf(20,24));

        btnPdf.setOnClickListener(v -> exportPdf());

        updateDateText();

        load();

    }

    void setLevelName(TextView tv, float db) {

        String text = "";
        int color = Color.GRAY;

        if (db <= 81) {
            text = "0-81 dB ปลอดภัย";
            color = Color.rgb(120,255,0);
        }
        else if (db <= 84) {
            text = "82-84 dB รับได้";
            color = Color.rgb(0,255,0);
        }
        else if (db <= 87) {
            text = "85-87 dB พอรับไหว";
            color = Color.rgb(255,255,0);
        }
        else if (db <= 90) {
            text = "88-90 dB ไม่ควรรับนาน";
            color = Color.rgb(255,200,0);
        }
        else if (db <= 93) {
            text = "91-93 dB ไม่ควรรับเสียง";
            color = Color.rgb(255,120,0);
        }
        else if (db <= 96) {
            text = "94-96 dB รับได้น้อยกว่า 1 ชม.";
            color = Color.rgb(255,0,0);
        }
        else if (db <= 99) {
            text = "97-99 dB รับได้น้อยกว่า 30 นาที";
            color = Color.rgb(255,0,150);
        }
        else if (db <= 102) {
            text = "100-102 dB ควรออกจากสถานที่";
            color = Color.rgb(255,0,255);
        }
        else {
            text = "103+ dB รับไม่ได้";
            color = Color.rgb(180,0,255);;
        }

        tv.setText(text);
        tv.setTextColor(color);
    }

    void updateDateText() {

        SimpleDateFormat f =
                new SimpleDateFormat("dd/MM/yyyy", Locale.US);

        txtDate.setText(
                f.format(selectedDay)
        );
    }


    void load() {

        long startDay = getStartOfDay(selectedDay);
        long endDay = startDay + 86400000;

        List<SoundData> list = repo.getByDay(startDay, endDay);

        filtered.clear();

        long todayStart = getStartOfDay(System.currentTimeMillis());

        if (startDay == todayStart) {

            long now = System.currentTimeMillis();

            for (SoundData d : list) {
                if (now - d.time <= currentRange) {
                    filtered.add(d);
                }
            }

        }

        else {

            filtered.addAll(list); 
        }

        draw();
        calc();
        updateSlot();
    }


    long getStartOfDay(long time) {

        Calendar c = Calendar.getInstance();

        c.setTimeInMillis(time);

        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        return c.getTimeInMillis();
    }


    void draw() {

        List<Entry> e = new ArrayList<>();

        int i = 0;

        for (SoundData d : filtered) {

            e.add(new Entry(i++, d.value));

        }

        LineDataSet set =
                new LineDataSet(e, "dB");

        set.setDrawCircles(false);
        set.setLineWidth(2f);
        set.setDrawValues(false);

        chart.setData(new LineData(set));

        chart.invalidate();
    }


    void calc() {

        if (filtered.size() == 0) return;

        float min = 999;
        float max = 0;
        float sum = 0;

        for (SoundData d : filtered) {

            if (d.value < min) min = d.value;

            if (d.value > max) max = d.value;

            sum += d.value;
        }

        float avg = sum / filtered.size();

        txtMin.setText("MIN\n" + (int) min);
        txtMax.setText("MAX\n" + (int) max);
        txtAvg.setText("AVG\n" + (int) avg);
    }


    // SLOT UPDATE
    void updateSlot() {

        slot1.setText("00:00-04:00");
        slot2.setText("04:00-08:00");
        slot3.setText("08:00-12:00");
        slot4.setText("12:00-16:00");
        slot5.setText("16:00-20:00");
        slot6.setText("20:00-24:00");

        updateSlotRange(slot1, slotName1, 0, 4);
        updateSlotRange(slot2, slotName2, 4, 8);
        updateSlotRange(slot3, slotName3, 8, 12);
        updateSlotRange(slot4, slotName4, 12, 16);
        updateSlotRange(slot5, slotName5, 16, 20);
        updateSlotRange(slot6, slotName6, 20, 24);
    }


    void updateSlotRange(
            TextView tv,
            TextView nameTv,
            int startHour,
            int endHour
    ) {

        long startDay = getStartOfDay(selectedDay);
        long endDay = startDay + 86400000;

        long start = startDay + startHour * 3600000L;
        long end = startDay + endHour * 3600000L;

        float min = 999;
        float max = 0;
        float sum = 0;
        int count = 0;

        List<SoundData> list = repo.getByDay(startDay, endDay);

        for (SoundData d : list) {

            if (d.time >= start && d.time < end) {

                if (d.value < min) min = d.value;
                if (d.value > max) max = d.value;

                sum += d.value;
                count++;
            }
        }

        if (count == 0) return;

        float avg = sum / count;

        String line1 =
                String.format(
                        "%02d:00-%02d:00",
                        startHour,
                        endHour
                );

        String minStr = String.valueOf((int) min);
        String maxStr = String.valueOf((int) max);
        String avgStr = String.valueOf((int) avg);

        String line2 =
                "Min " + minStr +
                        " Max " + maxStr +
                        " Avg " + avgStr;

        String text = line1 + "\n" + line2;

        SpannableString span = new SpannableString(text);


// ===== Min =====

        int minStart = text.indexOf(minStr);
        int minEnd = minStart + minStr.length();

        if (minStart >= 0) {

            int color;

            if (min == 0) {
                color = Color.WHITE;
            } else if (min > 94) {
                color = Color.RED;
            } else {
                color = Color.GREEN;
            }

            span.setSpan(
                    new ForegroundColorSpan(color),
                    minStart,
                    minEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }


// ===== Max =====

        int maxStart = text.indexOf(maxStr);
        int maxEnd = maxStart + maxStr.length();

        if (maxStart >= 0) {

            int color =
                    max > 94
                            ? Color.RED
                            : Color.GREEN;

            span.setSpan(
                    new ForegroundColorSpan(color),
                    maxStart,
                    maxEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }


// ===== Avg =====

        int avgStart = text.lastIndexOf(avgStr);
        int avgEnd = avgStart + avgStr.length();

        if (avgStart >= 0) {

            int color =
                    avg > 94
                            ? Color.RED
                            : Color.GREEN;

            span.setSpan(
                    new ForegroundColorSpan(color),
                    avgStart,
                    avgEnd,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
            );
        }


        tv.setText(span);
        tv.setTextColor(Color.WHITE);

        setLevelName(nameTv, avg);
    }


    // PDF
    void exportPdf() {

        try {

            long startDay = getStartOfDay(selectedDay);
            long endDay = startDay + 86400000;

            List<SoundData> list =
                    repo.getByDay(startDay, endDay);

            if (list.isEmpty()) {

                Toast.makeText(
                        this,
                        "No data",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            PdfDocument pdf = new PdfDocument();

            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo
                            .Builder(1000, 1600, 1)
                            .create();

            PdfDocument.Page page =
                    pdf.startPage(info);

            Canvas c = page.getCanvas();

            Paint text = new Paint();
            text.setTextSize(26);

            int y = 60;

            // ===== TITLE =====

            c.drawText(
                    "Sound Meter-Daily Report",
                    260,
                    y,
                    text
            );

            y += 60;

            // ===== DATE =====

            SimpleDateFormat f =
                    new SimpleDateFormat(
                            "dd MMMM yyyy",
                            Locale.US
                    );

            text.setTextSize(22);

            c.drawText(
                    "Date: " + f.format(selectedDay),
                    40,
                    y,
                    text
            );

            y += 40;

            // ===== STAT =====

            float min = Float.MAX_VALUE;
            float max = Float.MIN_VALUE;
            float sum = 0f;
            int totalCount = 0;

            for (SoundData d : list) {

                float v = d.value;

                if (v < min) min = v;
                if (v > max) max = v;

                sum += v;
                totalCount++;
            }

            float avg = 0f;

            if (totalCount > 0) {
                avg = sum / totalCount;
            }

            c.drawText("Statistics", 40, y, text);
            y += 40;

            c.drawText(
                    "MIN: " +
                            String.format(Locale.US, "%.2f", min)
                            + " dB",
                    80,
                    y,
                    text
            );
            y += 30;

            c.drawText(
                    "MAX: " +
                            String.format(Locale.US, "%.2f", max)
                            + " dB",
                    80,
                    y,
                    text
            );
            y += 30;

            c.drawText(
                    "AVG: " +
                            String.format(Locale.US, "%.2f", avg)
                            + " dB",
                    80,
                    y,
                    text
            );

            y += 50;

            // ===== SLOT =====

            c.drawText(
                    "Time Frame Breakdown (4hr slots)",
                    40,
                    y,
                    text
            );

            y += 40;

            for (int i = 0; i < 6; i++) {

                int s = i * 4;
                int e = s + 4;

                long start = startDay + s * 3600000L;
                long end = startDay + e * 3600000L;

                float sMin = 0;
                float sMax = 0;
                float sSum = 0;
                int count = 0;

                for (SoundData d : list) {

                    if (d.time >= start &&
                            d.time < end) {

                        if (d.value < sMin) sMin = d.value;
                        if (d.value > sMax) sMax = d.value;

                        sSum += d.value;
                        count++;
                    }
                }

                float sAvg =
                        count == 0 ? 0 : sSum / count;

                String level = getLevelName(sAvg);
                int levelColor = getLevelColor(sAvg);

                c.drawText(
                        String.format(
                                Locale.US,
                                "%02d.00-%02d.00",
                                s,
                                e
                        ),
                        40,
                        y,
                        text
                );

                y += 30;

                text.setColor(Color.BLACK);

                c.drawText(
                        "Min "
                                + String.format(Locale.US, "%.1f", sMin)
                                + " Max "
                                + String.format(Locale.US, "%.1f", sMax)
                                + " Avg "
                                + String.format(Locale.US, "%.0f", sAvg),
                        60,
                        y,
                        text
                );

                y += 30;

                text.setColor(levelColor);

                c.drawText(
                        level,
                        80,
                        y,
                        text
                );

                y += 50;

                text.setColor(Color.BLACK);
            }

            // ===== CHART =====

            c.drawText("Chart", 40, y, text);
            y += 20;

            int chartLeft = 120;
            int chartTop = y + 20;
            int chartRight = 900;
            int chartBottom = chartTop + 350;

            float maxDb = 120f;

            Paint border = new Paint();
            border.setColor(Color.GRAY);
            border.setStyle(Paint.Style.STROKE);

            c.drawRect(
                    chartLeft,
                    chartTop,
                    chartRight,
                    chartBottom,
                    border
            );

            Paint grid = new Paint();
            grid.setColor(Color.LTGRAY);

            Paint axis = new Paint();
            axis.setTextSize(18);

            for (int db = 0; db <= 120; db += 20) {

                float yPos =
                        chartBottom -
                                (db / maxDb)
                                        * (chartBottom - chartTop);

                c.drawLine(
                        chartLeft,
                        yPos,
                        chartRight,
                        yPos,
                        grid
                );

                c.drawText(
                        String.valueOf(db),
                        chartLeft - 50,
                        yPos,
                        axis
                );
            }

            Paint line = new Paint();
            line.setColor(Color.RED);
            line.setStrokeWidth(4);

            if (list.size() > 1) {

                float stepX =
                        (chartRight - chartLeft)
                                / (float) list.size();

                for (int i = 1; i < list.size(); i++) {

                    float x1 =
                            chartLeft + stepX * (i - 1);

                    float x2 =
                            chartLeft + stepX * i;

                    float y1 =
                            chartBottom -
                                    (list.get(i - 1).value / maxDb)
                                            * (chartBottom - chartTop);

                    float y2 =
                            chartBottom -
                                    (list.get(i).value / maxDb)
                                            * (chartBottom - chartTop);

                    c.drawLine(
                            x1,
                            y1,
                            x2,
                            y2,
                            line
                    );
                }
            }

            pdf.finishPage(page);

            SimpleDateFormat fileDate =
                    new SimpleDateFormat("yyyyMMdd", Locale.US);

            String fileName =
                    "daily_report_" +
                            fileDate.format(selectedDay) +
                            ".pdf";

            File file = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
            );

            FileOutputStream out =
                    new FileOutputStream(file);

            pdf.writeTo(out);

            out.close();
            pdf.close();

            try {
                Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "com.example.soundmeter.provider",
                        file
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(
                    this,
                    "Saved:\n" +
                            file.getAbsolutePath(),
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }

    void exportSlotPdf(int startHour, int endHour) {

        try {

            long startDay = getStartOfDay(selectedDay);
            long endDay = startDay + 86400000;

            List<SoundData> list =
                    repo.getByDay(startDay, endDay);

            if (list.isEmpty()) {

                Toast.makeText(
                        this,
                        "ไม่มีข้อมูล",
                        Toast.LENGTH_SHORT
                ).show();

                return;
            }

            PdfDocument pdf = new PdfDocument();

            PdfDocument.PageInfo info =
                    new PdfDocument.PageInfo
                            .Builder(1000, 1400, 1)
                            .create();

            PdfDocument.Page page =
                    pdf.startPage(info);

            Canvas c = page.getCanvas();

            Paint textPaint = new Paint();
            textPaint.setTextSize(28);

            int y = 60;

            // ===== TITLE =====

            c.drawText(
                    "Sound Meter - frameReport",
                    250,
                    y,
                    textPaint
            );

            y += 60;

            // ===== DATE =====

            textPaint.setTextSize(22);

            SimpleDateFormat f =
                    new SimpleDateFormat(
                            "dd MMMM yyyy",
                            Locale.US
                    );

            c.drawText(
                    "Date: " + f.format(selectedDay),
                    40,
                    y,
                    textPaint
            );

            y += 40;

            c.drawText(
                    startHour + ":00 - " +
                            endHour + ":00",
                    40,
                    y,
                    textPaint
            );

            y += 60;

            // ===== STAT =====

            float min = 999;
            float max = 0;
            float sum = 0;

            for (SoundData d : list) {

                if (d.value < min) min = d.value;
                if (d.value > max) max = d.value;

                sum += d.value;
            }

            float avg = sum / list.size();

            c.drawText("Statistics", 40, y, textPaint);
            y += 40;

            c.drawText(
                    "MIN: " +
                            String.format(Locale.US, "%.2f", min) +
                            " dB",
                    80,
                    y,
                    textPaint
            );
            y += 30;

            c.drawText(
                    "MAX: " +
                            String.format(Locale.US, "%.2f", max) +
                            " dB",
                    80,
                    y,
                    textPaint
            );
            y += 30;

            c.drawText(
                    "AVG: " +
                            String.format(Locale.US, "%.2f", avg) +
                            " dB",
                    80,
                    y,
                    textPaint
            );
            y += 60;

            // ===== CHART =====

            c.drawText("Chart", 40, y, textPaint);
            y += 20;

            int chartLeft = 120;
            int chartTop = y + 20;
            int chartRight = 900;
            int chartBottom = chartTop + 400;

            float maxDb = 120f;

// ===== border =====

            Paint border = new Paint();
            border.setColor(Color.GRAY);
            border.setStyle(Paint.Style.STROKE);
            border.setStrokeWidth(2);

            c.drawRect(
                    chartLeft,
                    chartTop,
                    chartRight,
                    chartBottom,
                    border
            );

// ===== grid =====

            Paint grid = new Paint();
            grid.setColor(Color.LTGRAY);
            grid.setStrokeWidth(1);

            Paint axisText = new Paint();
            axisText.setTextSize(20);

            int stepDb = 20;

            for (int db = 0; db <= 120; db += stepDb) {

                float yPos =
                        chartBottom -
                                (db / maxDb)
                                        * (chartBottom - chartTop);

                c.drawLine(
                        chartLeft,
                        yPos,
                        chartRight,
                        yPos,
                        grid
                );

                c.drawText(
                        String.valueOf(db),
                        chartLeft - 60,
                        yPos + 5,
                        axisText
                );
            }
// ===== time axis =====

            Paint timeText = new Paint();
            timeText.setTextSize(18);

            SimpleDateFormat tf =
                    new SimpleDateFormat(
                            "HH:mm:ss",
                            Locale.US
                    );

            if (list.size() > 1) {

                int stepLabel = list.size() / 6;

                if (stepLabel < 1) stepLabel = 1;

                for (int i = 0; i < list.size(); i += stepLabel) {

                    float x =
                            chartLeft +
                                    (chartRight - chartLeft)
                                            * i
                                            / (float) list.size();

                    String t =
                            tf.format(
                                    new Date(
                                            list.get(i).time
                                    )
                            );

                    c.drawLine(
                            x,
                            chartTop,
                            x,
                            chartBottom,
                            grid
                    );

                    c.drawText(
                            t,
                            x - 30,
                            chartBottom + 25,
                            timeText
                    );
                }
            }

// ===== line =====

            Paint line = new Paint();
            line.setColor(Color.RED);
            line.setStrokeWidth(4);
            line.setStyle(Paint.Style.STROKE);

            Paint fill = new Paint();
            fill.setColor(Color.argb(80, 255, 0, 0));
            fill.setStrokeWidth(2);

            if (list.size() > 1) {

                float stepX =
                        (chartRight - chartLeft)
                                / (float) list.size();

                for (int i = 1; i < list.size(); i++) {

                    float x1 =
                            chartLeft + stepX * (i - 1);

                    float x2 =
                            chartLeft + stepX * i;

                    float y1 =
                            chartBottom
                                    - (list.get(i - 1).value / maxDb)
                                    * (chartBottom - chartTop);

                    float y2 =
                            chartBottom
                                    - (list.get(i).value / maxDb)
                                    * (chartBottom - chartTop);

                    c.drawLine(
                            x1,
                            chartBottom,
                            x1,
                            y1,
                            fill
                    );

                    c.drawLine(
                            x1,
                            y1,
                            x2,
                            y2,
                            line
                    );
                }
            }

            pdf.finishPage(page);

            SimpleDateFormat fileDate =
                    new SimpleDateFormat("yyyyMMdd", Locale.US);

            String fileName =
                    "slot_" +
                            fileDate.format(selectedDay) +
                            "_" +
                            String.format("%02d00-%02d00", startHour, endHour) +
                            ".pdf";

            File file = new File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    fileName
            );

            FileOutputStream out =
                    new FileOutputStream(file);

            pdf.writeTo(out);

            out.close();
            pdf.close();

            try {
                Uri uri = androidx.core.content.FileProvider.getUriForFile(
                        this,
                        "com.example.soundmeter.provider",
                        file
                );

                Intent intent = new Intent(Intent.ACTION_VIEW);
                intent.setDataAndType(uri, "application/pdf");
                intent.setFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

                startActivity(intent);

            } catch (Exception e) {
                Toast.makeText(this, "No PDF viewer found", Toast.LENGTH_SHORT).show();
            }

            Toast.makeText(
                    this,
                    "Saved:\n" +
                            file.getAbsolutePath(),
                    Toast.LENGTH_LONG
            ).show();

        } catch (Exception e) {

            e.printStackTrace();
        }
    }
    String getLevelName(float db) {

        if (db <= 81) return "0-81dB ปลอดภัย";
        else if (db <= 84) return "82-84dB รับได้";
        else if (db <= 87) return "85-87dB พอรับไหว";
        else if (db <= 90) return "88-90dB ไม่ควรรับนาน";
        else if (db <= 93) return "91-93dB ไม่ควรรับเสียง";
        else if (db <= 96) return "94-96dB รับได้น้อยกว่า 1ชม.";
        else if (db <= 99) return "97-99dB รับได้น้อยกว่า 30นาที";
        else if (db <= 102) return "100-102dB ควรออกจากสถานที่";
        else return "103+ รับไม่ได้";
    }
    int getLevelColor(float db) {

        if (db <= 81) return Color.rgb(120,255,0);
        else if (db <= 84) return Color.rgb(0,255,0);
        else if (db <= 87) return Color.rgb(255,255,0);
        else if (db <= 90) return Color.rgb(255,200,0);
        else if (db <= 93) return Color.rgb(255,120,0);
        else if (db <= 96) return Color.rgb(255,0,0);
        else if (db <= 99) return Color.rgb(255,0,150);
        else if (db <= 102) return Color.rgb(255,0,255);
        else return Color.rgb(180,0,255);
    }
}
