package com.bimal.mag_positioning.pdr;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.SystemClock;
import android.util.Log;

import com.bimal.mag_positioning.InsertData;

import java.io.PrintWriter;

/**
 * Created by JHLee on 2015-09-14.
 *
 */
public class GyroscopeEventListener implements SensorEventListener
{
    private static final String TAG = GyroscopeEventListener.class.getSimpleName();

    // 적분 관련 상수
    private static final float NS2S = 1.0f / 1000000000.0f;

    // 상보필터 상수
    private static final float FILTER_COEFFICIENT = 0.95f;

    // 자이로 측정값 관련 변수
    private final float[] deltaRotationVector = new float[4];
    private float[] gyroscopeOrientation = new float[3];

    // 값 전달 용 객체체
    private InsertData onlineActivity;
    private MagneticfieldEventListener magEvent;
    private AccelerationEventListener accEvent;

    // 데이터 저장용 객체 및 변수
    private PrintWriter printWriter;

    private float[] values;
    private float[] radAngle;
    private float[] N_radAngle;
    private float[] degree;
    private float[] N_degree;

    private static double initAzimuth;
    private float curAzimuth;
    private float N_curAzimuth;
    private float filtered;

    // 변수
    private long startTime;
    private long timestamp;
    private long N_timestamp;

    public static boolean Norm = false;

    // average Filter value
    private static int k;
    private float[] prevAvg;
    private static float alpha;
    private float[] avg;

    // 저장 Datas
    public static float kalAngleZ = 0.0f;
    public static float compZ = 0.0f;
    public static float compYaw = 0.0f;
    public static float compYawDeg = 0.0f;
    public static float compDeg = 0.0f;
    private float kalYaw = 0.0f;


    private float[] rotationMatrix;
    private float[] orientationValues;
    private float[] acc;

    // 칼만 필터 객체
    public static KalmanFilter kalmanYaw = new KalmanFilter();

    public GyroscopeEventListener(InsertData onlineActivity, AccelerationEventListener accEvent, MagneticfieldEventListener magEvent)
    {
        this.onlineActivity = onlineActivity;
        this.magEvent = magEvent;
        this.accEvent = accEvent;

        startTime = SystemClock.uptimeMillis();

        k = 1;
        alpha = 0.0f;
        avg = new float[3];
        prevAvg = new float[3];
        values = new float[3];
        radAngle = new float[3];
        degree = new float[3];
        N_degree = new float[3];
        N_radAngle = new float[3];


        rotationMatrix = new float[16];
        orientationValues = new float[3];

        acc = new float[3];

        timestamp = 0;
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        values = event.values.clone();

        if (onlineActivity.isStart == true && Norm == false) {
            long curTime = SystemClock.uptimeMillis();

            checkNorm(curTime);
        }

        if (Norm== true && magEvent.checkOrient == true){
            calRadAzimuth(values, event.timestamp);
        }


    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Accuracy Change : " + accuracy);
    }

    private void calRadAzimuth(float[] value, long eventTime){
        final float magAzimuth = magEvent.getRadAzimuth();
        acc = accEvent.getFilteredValues();

        if (timestamp != 0) {
            final float dT = (eventTime - timestamp) * NS2S;
            radAngle[0] += value[0] * dT;
            radAngle[1] += value[1] * dT;
            radAngle[2] += value[2] * dT;

            N_radAngle[0] += (value[0] * dT - avg[0]);
            N_radAngle[1] += (value[1] * dT - avg[1]);
            N_radAngle[2] += (value[2] * dT - avg[2]);

           kalAngleZ = kalmanYaw.getAngle(magAzimuth, -value[2], dT);
            complementaryFilter(magAzimuth, value[2], dT);

           kalYaw = (float) Math.toDegrees(kalAngleZ);

           // onlineActivity.setKalmanYaw(kalYaw);
        }

        calDegAzimuth();
        calDegAzimuthWithNorm();
        timestamp = eventTime;
    }

    private void complementaryFilter(float magAzimuth, float value, float dT) {
        compZ = FILTER_COEFFICIENT * (compZ + (-value) * dT) + (1 - FILTER_COEFFICIENT) * magAzimuth;
        compYawDeg = FILTER_COEFFICIENT * compDeg + (1 - FILTER_COEFFICIENT) * magEvent.getAzimuth();

      //  onlineActivity.setCompYawDeg(compYawDeg);
       // onlineActivity.mapView.setCompYawRad(compZ);
    }

    private void calDegAzimuth() {
        degree[0] = (float) Math.toDegrees(radAngle[0]);
        degree[1] = (float) Math.toDegrees(radAngle[1]);
        degree[2] = (float) Math.toDegrees(radAngle[2]);

        curAzimuth = degree[2];
        compDeg = (float) (initAzimuth - curAzimuth);

     //   onlineActivity.mapView.setGyroYaw(compDeg);
        onlineActivity.setGyroYaw(compDeg);

    }


    private void calDegAzimuthWithNorm() {
        N_degree[0] = (float) Math.toDegrees(N_radAngle[0]);
        N_degree[1] = (float) Math.toDegrees(N_radAngle[1]);
        N_degree[2] = (float) Math.toDegrees(N_radAngle[2]);

        N_curAzimuth = N_degree[2];

        //onlineActivity.setNGyroYaw(N_curAzimuth);
    }

    public double getCurrentAzimuth() {
        return curAzimuth;
    }



    public static void setInitAzimuth(float azimuth) {
        initAzimuth = azimuth;

        Log.d(TAG, "" + initAzimuth);
    }

    public float[] getDegree() {
        return degree;
    }

    public float[] getValues() {
        return values;
    }

    private void checkNorm(long curTime){
        if(curTime - onlineActivity.startTime <= 3000){
            averageFilter(values);
        }else if(curTime - onlineActivity.startTime > 3000){
            Norm = true;

            Log.d(TAG, "avg[0] : " + avg[0] + ", avg[1] : " + avg[1]
                    + ", avg[2] : " + avg[2]);
        }
    }

    private void averageFilter(float [] value){
        alpha = ((float)(k - 1) / k);

        avg[0] =  alpha * prevAvg[0] + (1 - alpha) * value[0];
        avg[1] = alpha * prevAvg[1] + (1 - alpha) * value[1];
        avg[2] = alpha * prevAvg[2] + (1 - alpha) * value[2];

        prevAvg[0] = avg[0];
        prevAvg[1] = avg[1];
        prevAvg[2] = avg[2];

        Log.d(TAG, "K : " + k + ", alpha : " + alpha + ", avg[0] : " + avg[0] + ", value[0] : " + value[0] + ", prevAvg[0] : " + prevAvg[0] );

        k = k + 1;
    }




}
