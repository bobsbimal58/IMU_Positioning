package com.bimal.mag_positioning;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.Color;
import android.graphics.PointF;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wmcs on 7/21/2017.
 */

public class MapActivity extends AppCompatActivity implements SensorEventListener{


    Sensor magnetometer;
    SensorManager sm;
    public Float a;
    public Float b;
    public Float c;
    public Float d;
    public Float total;
    public Integer id;
    PointF get;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.map_activity);


        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);

        magnetometer = sm.getDefaultSensor(magnetometer.TYPE_MAGNETIC_FIELD);
        if (magnetometer == null) {
            Toast.makeText(this, "Magnetometer not available", Toast.LENGTH_SHORT).show();
            finish();
        }
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        Float z;
        Float a1;
        Float b1;
        Float cc;

        a = event.values[0];
        b = event.values[1];
        c = event.values[2];
        d = (float) Math.sqrt(Math.pow(a, 2) + Math.pow(b, 2) + Math.pow(c, 2));

            SQLiteDatabase db;
            db = openOrCreateDatabase(
                    "Mag_Positioning.db"
                    , SQLiteDatabase.CREATE_IF_NECESSARY
                    , null
            );
            db.setVersion(1);
            db.setLocale(Locale.getDefault());
            db.setLockingEnabled(true);

            Cursor cur = DBHelper.getInstance().getAllData();
            cur.moveToFirst();
            HashMap<PointF, Float> difference = new HashMap<>();

            if (cur.isLast() == false) {
                do {

                    PointF location = new PointF(cur.getInt(2), cur.getInt(3));
                    a1 = Float.valueOf(cur.getString(4));
                    b1 = Float.valueOf(cur.getString(5));
                    cc = Float.valueOf(cur.getString(6));
                    z = Float.valueOf(cur.getString(7));
                    total = Float.valueOf((float) Math.sqrt(((Math.pow((a - a1), 2) + Math.pow((b - b1), 2) + Math.pow((c - cc), 2) + Math.pow((d - z), 2)))));
                    difference.put(location, total);

                } while (cur.moveToNext());

            }

            Map.Entry<PointF, Float> min = Collections.min(difference.entrySet(), new Comparator<Map.Entry<PointF, Float>>() {
                @Override
                public int compare(Map.Entry<PointF, Float> entry1, Map.Entry<PointF, Float> entry2) {
                    return entry1.getValue().compareTo(entry2.getValue());
                }
            });
            get = min.getKey();
        int x = (int) get.x;
        int y = (int) get.y;

        FrameLayout root = (FrameLayout)findViewById(R.id.mapview);
        if (root != null){
            root.removeAllViewsInLayout();
        }
        ImageView img = new ImageView(this);
        img.setBackgroundColor(Color.GREEN);
        FrameLayout.LayoutParams params  = new FrameLayout.LayoutParams(35,35);
        params.leftMargin = x;
        params.topMargin = y;
        root.addView(img, params);


            cur.close();

        }
    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sm.unregisterListener(this);
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }
}
