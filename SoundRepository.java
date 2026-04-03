package com.example.soundmeter;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;
import java.util.List;

public class SoundRepository {

    DBHelper dbHelper;

    public SoundRepository(Context context) {
        dbHelper = new DBHelper(context);
    }

    public void insert(float value) {

        SQLiteDatabase db = dbHelper.getWritableDatabase();

        ContentValues cv = new ContentValues();
        cv.put("time", System.currentTimeMillis());
        cv.put("value", value);

        db.insert("sound_data", null, cv);
    }

    public List<SoundData> getByDay(long startDay, long endDay) {

        List<SoundData> list = new ArrayList<>();

        SQLiteDatabase db = dbHelper.getReadableDatabase();

        Cursor cursor = db.rawQuery(
                "SELECT * FROM sound_data WHERE time BETWEEN ? AND ?",
                new String[]{
                        String.valueOf(startDay),
                        String.valueOf(endDay)
                }
        );

        if (cursor.moveToFirst()) {
            do {
                SoundData data = new SoundData(
                        cursor.getFloat(cursor.getColumnIndexOrThrow("value")),
                        cursor.getLong(cursor.getColumnIndexOrThrow("time"))
                );

                list.add(data);

            } while (cursor.moveToNext());
        }

        cursor.close();

        return list;
    }
}
