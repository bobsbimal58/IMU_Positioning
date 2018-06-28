package com.bimal.mag_positioning.pdr;

import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.os.SystemClock;
import android.util.Log;

import com.bimal.mag_positioning.InsertData;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

/**
 * Created by JHLee on 2015-09-14.
 */
public class AccelerationEventListener implements SensorEventListener {
    private static final String TAG = AccelerationEventListener.class.getSimpleName();

    // Record 관련
    private static final String CSV_HEADER = "A_X Axis, A_Y Axis, A_Z Axis, F_X Axis, F_Y Axis, F_Z Axis, Acceleration, F_Acceleration, A_Time";
    private static final char CSV_DELIM = ',';

    private PrintWriter printWriter;

    // Record 데이터 변수
    private float[] filteredValues;


    // 걸음 검출 및 보폭 결정 상수
    private static final float maxPeakThreshold = 10.7f;
    private static final float minPeakThreshold = 9.0f;
    private static final float K = 0.35f;



    // 시작 시간
    private long startTime;

    // 걸음 데이터 전송용 메인 객체
    private InsertData onlineActivity;
    private long detectedTime;              // 걸음 검출 시간

   // 센서 측정 변수
    private float[] values;

    // 걸음 검출 및 보폭 관련 변수
    private float stepSize;
    private int step;
    private boolean stepFlag;
    private boolean minFlag;

    // 최대 및 최소 가속도 (1걸음)
    private float maxSumAcc;
    private float minSumAcc;



    public AccelerationEventListener(InsertData onlineActivity){
        this.onlineActivity = onlineActivity;

        startTime = SystemClock.uptimeMillis();

        // 사용 변수(객체) 초기화
        values = new float[3];
        filteredValues = new float[3];

        step = 0;
        stepSize = 0.0f;
        stepFlag = false;
        minFlag = false;
        maxSumAcc = 0.0f;
        minSumAcc = 0.0f;
        detectedTime = 0L;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        Log.d(TAG, "Accuracy Changed : " + accuracy);
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        values = event.values.clone();

        filteredValues = lowPassFilter(values);

        // 합산 가속도 계산
        double sumOfSquares = (values[0] * values[0]) + (values[1] * values[1])
                + (values[2] * values[2]);
        double acceleration = Math.sqrt(sumOfSquares);

        double f_sumOfSquares = (filteredValues[0] * filteredValues[0])
                + (filteredValues[1] * filteredValues[1])
                + (filteredValues[2] * filteredValues[2]);
        double f_acceleration = Math.sqrt(f_sumOfSquares);


        if (onlineActivity.isRecord)
        {
            detectedTime = (event.timestamp / 1000000) - startTime;
            writeSensorEvent(acceleration, f_acceleration, detectedTime);
        }
        if (onlineActivity.isStart)
            stepDetection(f_acceleration);
    }

    private float[] lowPassFilter(float[] values) {
        float[] value = new float[3];
        final float alpha = 0.8f;

        value[0] = alpha * filteredValues[0] + (1 - alpha) * values[0];
        value[1] = alpha * filteredValues[1] + (1 - alpha) * values[1];
        value[2] = alpha * filteredValues[2] + (1 - alpha) * values[2];

        return value;
    }

    private void stepDetection(double acceleration) {
        if (acceleration > maxPeakThreshold && stepFlag == false) {
            stepFlag = true;
            maxSumAcc = (float) acceleration;

        } else if (acceleration > maxPeakThreshold && stepFlag == true) {
            if (maxSumAcc < acceleration) {
                maxSumAcc = (float) acceleration;
            }

        }else if (acceleration < minPeakThreshold && stepFlag == true) {
            minFlag = true;
            if(minSumAcc > acceleration){
                minSumAcc = (float) acceleration;
            }

        }
        // 걸음이 검출 된 경우
        if(acceleration < maxPeakThreshold && acceleration > minPeakThreshold){
            if(stepFlag == true && minFlag == true){
                step++;

                setStepSize();

                // 걸음 정보 전달
                onlineActivity.setStepLength(stepSize);
                onlineActivity.getStepCount(step);
               // onlineActivity.mapView.setStepCount(step);
               // onlineActivity.mapView.setDetectedTime(detectedTime);
               // onlineActivity.mapView.setStepLength(stepSize);



                stepFlag = false;
                minFlag = false;
            }
        }
    }



    public float[] getValues() {
        return this.values;
    }

    public float[] getFilteredValues() {
        return this.filteredValues;
    }

    // 보폭 결정 메소드
    private void setStepSize() {
        stepSize = (float) (K * Math.sqrt(Math.sqrt(maxSumAcc - minSumAcc)));
    }

    public void setPrintWriter(File dataFile) {
        try {
            printWriter = new PrintWriter(new BufferedWriter(new FileWriter(
                    dataFile)));

            printWriter.println(CSV_HEADER);

        } catch (IOException e) {
            Log.e(TAG, "Could not open CSV file(s)", e);
        }
    }
    public void recordStop() {
        if (printWriter != null) {
            printWriter.close();
        }

        if (printWriter.checkError()) {
            Log.e(TAG, "Error closing writer");
        }
    }

    public long getDetectedTime(){
        return detectedTime;
    }


    // Acceleration Data .csv 저장
    private void writeSensorEvent(double acceleration, double f_acceleration,
                                  long eventTime) {
        if (printWriter != null) {
            StringBuffer sb = new StringBuffer();
            sb.append(values[0])
                    .append(CSV_DELIM).append(values[1]).append(CSV_DELIM)
                    .append(values[2]).append(CSV_DELIM)
                    .append(filteredValues[0]).append(CSV_DELIM)
                    .append(filteredValues[1]).append(CSV_DELIM)
                    .append(filteredValues[2]).append(CSV_DELIM)
                    .append(acceleration).append(CSV_DELIM)
                    .append(f_acceleration).append(CSV_DELIM)
                    .append(eventTime);

            printWriter.println(sb.toString());

            if (printWriter.checkError())
                Log.e(TAG, "Error writing sensor event data");
        }
    }
}
