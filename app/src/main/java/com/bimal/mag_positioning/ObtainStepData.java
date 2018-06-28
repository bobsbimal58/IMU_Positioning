package com.bimal.mag_positioning;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.icu.text.DecimalFormat;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.TextView;

import java.util.ArrayList;

/**
 * Created by Bimal Bhattarai on 9/28/2017.
 */

@RequiresApi(api = Build.VERSION_CODES.N)
public class ObtainStepData  extends AppCompatActivity implements SensorEventListener {
    SensorManager Sm;
    Context context;
    Sensor Accelerometer, Magnetometer;
    TextView textView_countStep, textView_stepLength,
            textView_degree, textView_coordinate;
    int maLength = 5, stepState = 0, stepCount = 0;
    long lastUpdate = 0, zeroTime = 0, accNum = 0, lastTimeAcc, curTimeAcc, lastTimeMag, curTimeMag;
    boolean isObtainAccView = false, isObtainDegreeView = false;
    float[] accValues = new float[3];
    float[] magValues = new float[3];
    float[] values = new float[3];
    float[] R = new float[9];
    float[] I = new float[9];
    float accModule = 0, maResult = 0;
    float maxVal = 0f, minVal = 0f, stepLength = 0f;
    static int stepObtainDelaySec = 0;
    static float accThreshold = 0.65f, co_k_wein = 35f, alpha = 0.25f;
    int degreeDisplay, sensorCounter;
    float offset, degree;
    DecimalFormat decimalF = new DecimalFormat("#.00");
    final static int[] initPoint = {137, 642};
    static float[] curCoordsOfStep = {137, 642};
    static ArrayList<CoordPoint> points = new ArrayList<CoordPoint>();


    public ObtainStepData(Context context,TextView textView_countStep, TextView textView_stepLength, TextView textView_degree, TextView textView_coordinate) {
        this.context = context;
        this.textView_countStep = textView_countStep;
        this.textView_stepLength = textView_stepLength;
        this.textView_degree = textView_degree;
        this.textView_coordinate = textView_coordinate;
        loadSystemService();
    }
    public void loadSystemService() {
		/*
		 * load sensor system service.
		 */
        Sm = (SensorManager)context.getSystemService(Context.SENSOR_SERVICE);
        Accelerometer = Sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        Magnetometer = Sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);
    }

    public void obtainStep() {
		/*
		 * start listening sensor of Accelerometer and Magnetometer.
		 */
        lastTimeAcc = System.currentTimeMillis();
        lastTimeMag = System.currentTimeMillis();
        Sm.registerListener(this, Accelerometer,SensorManager.SENSOR_DELAY_GAME);
        Sm.registerListener(this, Magnetometer,SensorManager.SENSOR_DELAY_GAME);
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
            recordTrajectory(curCoordsOfStep.clone());
            maxVal = minVal = stepState = 0;
        }
        if (isObtainAccView) stepViewShow();
    }
    private void stepViewShow() {
		/*
		 * show some step information.
		 */
        textView_countStep.setText("Step Count : " + stepCount);
        textView_stepLength.setText("Step Length : " + decimalF.format(stepLength) + " cm");
        textView_coordinate.setText("Coordinate : " + "X: " + decimalF.format(curCoordsOfStep[0]) + " Y: "
                + decimalF.format(curCoordsOfStep[1]));
    }

    private void getStepLengthAndCoordinate() {
		/*
		 * compute step length and coordinate of pedestrian.
		 */
        stepLength = (float)(co_k_wein * Math.pow(maxVal - minVal,1.0/4));
        double delta_x = Math.cos(Math.toRadians(degreeDisplay)) * stepLength;
        double delta_y = Math.sin(Math.toRadians(degreeDisplay)) * stepLength;
        curCoordsOfStep[0] += delta_x;
        curCoordsOfStep[1] += delta_y;
    }

    private void recordTrajectory(float[] clone) {
		/*
		 * add the coordinate points of pedestrian.
		 */
        points.add(new CoordPoint(clone[0], clone[1]));
    }

    public static void initPoints() {
		/*
		 * add the initial position coordinate of pedestrian.
		 */
        points.add(new CoordPoint(initPoint[0], initPoint[1]));
    }

    private void getAzimuthDegree(float[] MagClone) {
		/*
		 * get the azimuth degree of the pedestrian.
		 */
        magValues = lowPassFilter(MagClone, magValues);
        if (accValues == null || magValues == null) return;
        boolean sucess = SensorManager.getRotationMatrix(R, I, accValues, magValues);
        if (sucess) {
            SensorManager.getOrientation(R, values);
            degree = (int)(Math.toDegrees(values[0]) + 360) % 360; // translate into (0, 360).
            degree = ((int)(degree + 2)) / 5 * 5; // the value of degree is multiples of 5.
            if (offset == 0) {
                degreeDisplay = (int) degree;
            } else {
                degreeDisplay = roomDirection(degree, offset); // user-defined room direction.
            }
            if (isObtainDegreeView) stepDegreeViewShow();
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

    public void stopStep() {
		/*
		 * stop listening for sensor and recording step information.
		 */
        setObtainAccView(false);
        setObtainDegreeView(false);
        Sm.unregisterListener(this);
    }
    public void correctStep() {
		/*
		 * initialize and correct the step parameters.
		 */
        offset = degree - 270;
        curCoordsOfStep[0] = 137;
        curCoordsOfStep[1] = 642;
        stepCount = 0;
        accNum = 0;
        stepLength = 0;
    }
    public void stepViewGone() {
		/*
		 * set the view to gone.
		 */
        textView_countStep.setVisibility(View.GONE);
        textView_stepLength.setVisibility(View.GONE);
        textView_degree.setVisibility(View.GONE);
        textView_coordinate.setVisibility(View.GONE);
    }
    public void setObtainAccView(boolean isObtainAccView) {
		/*
		 * whether obtain acceleration view or not.
		 */
        this.isObtainAccView = isObtainAccView;
    }
    public void setObtainDegreeView(boolean isObtainDegreeView) {
		/*
		 * whether obtain the azimuth degree or not.
		 */
        this.isObtainDegreeView = isObtainDegreeView;
    }
    public static void setCurCoordsOfStep(float[] coords) {
		/*
		 * set the current coordinate of the pedestrian.
		 */
        curCoordsOfStep = coords.clone();
    }
    public static float getAccThreshold() {
		/*
		 * get the acceleration threshold of the pedestrian.
		 */
        return accThreshold;
    }

    public static int getStepObtainDelaySec() {
		/*
		 * get the delay seconds of obtaining step information.
		 */
        return stepObtainDelaySec;
    }

    public static float getCo_k_wein() {
		/*
		 * get the coefficient K of weinberg model.
		 */
        return co_k_wein;
    }

    public static float[] getCurCoordsOfStep() {
		/*
		 * get the current coordinate of pedestrian step.
		 */
        return curCoordsOfStep;
    }

    public static ArrayList<CoordPoint> getPoints() {
		/*
		 * get coordinate point.
		 */
        return points;
    }

    public static void clearPoints() {
		/*
		 * clear coordinate point.
		 */
        points.clear();
    }
    @Override
    public void onAccuracyChanged(Sensor sensor, int i) {

    }
}
