package com.bimal.mag_positioning;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.text.DecimalFormat;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;

/**
 * Created by Bimal Bhattarai on 9/29/2017.
 */

public class stepCounter extends AppCompatActivity implements SensorEventListener {

    Sensor magnetometer;
    Sensor Accelerometer;
    SensorManager sm;
    SensorEvent MagnetData,AccelVector;
    TextView textView_countStep, textView_stepLength,
            textView_degree, textView_coordinate;
    long lastUpdate = 0, zeroTime = 0, accNum = 0, lastTimeAcc, curTimeAcc, lastTimeMag, curTimeMag;
    int maLength = 5, stepState = 0, stepCount = 0;
    float[] accValues = new float[3];
    float[] magValues = new float[3];
    float[] values = new float[3];
    float[] Rot = new float[9];
    float[] I = new float[9];
    float accModule = 0, maResult = 0;
    float maxVal = 0f, minVal = 0f, stepLength = 0f;
    static int stepObtainDelaySec = 0;
    static float accThreshold = 0.65f, co_k_wein = 45f, alpha = 0.25f;
    int degreeDisplay, sensorCounter;
    float offset, degree;
    final static int[] initPoint = {0, 0};
    static float[] curCoordsOfStep = {0, 0};
    boolean isRecordAcc = false, isObtainAccView = false, isObtainDegreeView = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.accelerometer_main);

        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), SensorManager.SENSOR_DELAY_GAME);

        textView_countStep         = (TextView) findViewById(R.id.textView_countStep);
        textView_stepLength        = (TextView) findViewById(R.id.textView_stepLength);
        textView_degree        = (TextView) findViewById(R.id.textView_degree);
        textView_coordinate        = (TextView) findViewById(R.id.textView_coordinate);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            curTimeAcc = System.currentTimeMillis();
            if (curTimeAcc - lastTimeAcc > 40) {
                getStepAccInfo(event.values.clone());
                lastTimeAcc = curTimeAcc;
            }
        }
        if (event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD) {
            curTimeMag = System.currentTimeMillis();
            if (curTimeMag - lastTimeMag > 40) {
                getAzimuthDegree(event.values.clone());
                lastTimeMag = curTimeMag;
            }
        }


    }
    private void getStepAccInfo(float[] accClone) {
		/*
		 * step information computing algorithm.
		 */
        accValues = accClone;
        accModule = (float) (Math.sqrt(Math.pow(accValues[0], 2) + Math.pow(accValues[1], 2) + Math.pow(accValues[2], 2)) - 9.794);
        maResult = MovingAverage.movingAverage(accModule, maLength);
        if (stepState == 0 && maResult > accThreshold) {
            stepState = 1;
        }
        if (stepState == 1 && maResult > maxVal) { //find peak
            maxVal = maResult;
        }
        if (stepState == 1 && maResult <= 0) {
            stepState = 2;
        }
        if (stepState == 2 && maResult < minVal) { //find bottom
            minVal = maResult;
        }
        if (stepState == 2 && maResult >= 0) {
            stepCount ++;
            getStepLengthAndCoordinate();
            maxVal = minVal = stepState = 0;
        }
        stepViewShow();
    }
    private void stepViewShow() {
		/*
		 * show some step information.
		 */
        textView_countStep.setText("Step Count : " + stepCount);
        textView_stepLength.setText("Step Length : " + (stepLength) + " cm");
        textView_coordinate.setText("Coordinate : " + "X: " + (curCoordsOfStep[0]) + " Y: "
                + (curCoordsOfStep[1]));
    }

    private void getStepLengthAndCoordinate() {
		/*
		 * compute step length and coordinate of pedestrian.
		 */
        stepLength = (float)(co_k_wein * Math.pow(maxVal - minVal,1.0/4));
        double delta_x = Math.cos(Math.toRadians(-degreeDisplay)) * stepLength;
        double delta_y = Math.sin(Math.toRadians(-degreeDisplay)) * stepLength;
        curCoordsOfStep[0] += delta_x;
        curCoordsOfStep[1] += delta_y;
    }


    private void getAzimuthDegree(float[] MagClone) {
		/*
		 * get the azimuth degree of the pedestrian.
		 */
        magValues = lowPassFilter(MagClone, magValues);
        if (accValues == null || magValues == null) return;
        boolean sucess = SensorManager.getRotationMatrix(Rot, I, accValues, magValues);
        if (sucess) {
            SensorManager.getOrientation(Rot, values);
            degree = (int)(Math.toDegrees(values[0]) + 360) % 360; // translate into (0, 360).
            degree = ((int)(degree + 2)) / 5 * 5; // the value of degree is multiples of 5.
            if (offset == 0) {
                degreeDisplay = (int) degree;
            } else {
                degreeDisplay = roomDirection(degree, offset); // user-defined room direction.
            }
           stepDegreeViewShow();
        }
    }
    private void stepDegreeViewShow() {
		/*
		 * show the azimuth degree.
		 */
        textView_degree.setText(" Angle : " + degreeDisplay + " degree");
    }
    private int roomDirection(float myDegree, float myOffset) {
		/*
		 * define room direction as 270 degree.
		 */
        int tmp = (int)(myDegree - myOffset);
        if(tmp < 0) tmp += 360;
        else if(tmp >= 360) tmp -= 360;
        return tmp;
    }

    protected float[] lowPassFilter(float[] input, float[] output) {
		/*
		 * low pass filter algorithm implement.
		 */
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
