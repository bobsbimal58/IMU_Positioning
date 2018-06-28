package com.bimal.mag_positioning.pdr;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.SystemClock;
import android.util.Log;

import com.bimal.mag_positioning.InsertData;

/**
 * Created by JHLee on 2015-09-14.
 */
public class MagneticfieldEventListener implements SensorEventListener {
    private static final String TAG = MagneticfieldEventListener.class.getSimpleName();

    // 자료 전달 관련 객체
    private InsertData onlineActivity;
    private AccelerationEventListener accEvent;
    private GyroscopeEventListener gyroEvent;

    // 저장 측정치 변수
    private float[] values;
    private float[] rotationMatrix;
    private float[] orientationValues;
    private float[] initDegree;
    private float[] oriDegree;
    private float initAzimuth;
    private static float azimuth;

    private float initRadAzimuth;
    private static float RadAzimuth;

    // 변수
    private long startTime;
    private boolean isInit;
    float[] acc;


    public static boolean checkOrient = false;

    public MagneticfieldEventListener(InsertData onlineActivity,
                                      AccelerationEventListener accEvent, GyroscopeEventListener gyroEvent) {

        this.onlineActivity = onlineActivity;
        this.accEvent = accEvent;
        this.gyroEvent = gyroEvent;

        startTime = SystemClock.uptimeMillis();

        values = new float[3];
        rotationMatrix = new float[16];
        orientationValues = new float[3];
        initDegree = new float[3];
        oriDegree = new float[3];
        acc = new float[3];

        isInit = false;
    }



    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Accurach Changed : " + accuracy);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        values = event.values.clone();

        if(gyroEvent.Norm == true && checkOrient == false){
            setOrientation();
        }

        if (checkOrient == true)
            calcAzimuth();
    }

    private void calcAzimuth() {
        if(!isInit){
            acc = accEvent.getFilteredValues();
            isInit = true;
        }


        SensorManager.getRotationMatrix(rotationMatrix, null, acc, values);
        SensorManager.getOrientation(rotationMatrix, orientationValues);

        RadAzimuth = orientationValues[0];
        azimuth = (float) Math.toDegrees(orientationValues[0]);

        //onlineActivity.mapView.setAzimuth(azimuth);
        onlineActivity.getAzimuth(azimuth);

    }



    public void setOrientation() {
        float[] acc = accEvent.getFilteredValues();

        SensorManager.getRotationMatrix(rotationMatrix, null, acc, values);
        SensorManager.getOrientation(rotationMatrix, orientationValues);

        setInitAzimuth();

        checkOrient = true;
    }

    public float getInitAzimuth() {
        return initAzimuth;
    }

    public static float getAzimuth()




    {
        return azimuth;
    }

    public static float getRadAzimuth(){
        return RadAzimuth;
    }

    public float[] getValues() {
        return values;
    }

    private void setInitAzimuth() {
        initRadAzimuth = orientationValues[0];
        initAzimuth = (float) Math.toDegrees(orientationValues[0]);
        RadAzimuth = orientationValues[0];

        Log.e(TAG, "" + initAzimuth);
        gyroEvent.setInitAzimuth(initAzimuth);
        gyroEvent.kalmanYaw.setAngle(initRadAzimuth);
        gyroEvent.kalAngleZ = initRadAzimuth;
        gyroEvent.compZ = initRadAzimuth;
        gyroEvent.compDeg = initAzimuth;
    }

}
