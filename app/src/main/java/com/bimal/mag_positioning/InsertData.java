package com.bimal.mag_positioning;

import android.content.pm.PackageManager;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.graphics.PointF;
import android.hardware.GeomagneticField;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.SystemClock;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bimal.mag_positioning.pdr.AccelerationEventListener;
import com.bimal.mag_positioning.pdr.GyroscopeEventListener;
import com.bimal.mag_positioning.pdr.MagneticfieldEventListener;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Created by wmcs on 7/21/2017.
 */

public class InsertData extends AppCompatActivity implements SensorEventListener,View.OnClickListener {
    Sensor magnetometer;
    SensorManager sm;
    TextView magnetismx;
    TextView magnetismy;
    TextView magnetismz;
    TextView magnetismd;
    public Float xaxis;
    public Float yaxis;
    public Float zaxis;
    public Float xaxis_rot;
    public Float yaxis_rot;
    public Float zaxis_rot;
    public Float xbias;
    public Float ybias;
    public Float zbias;
    public Float average;
    public Float total;
    public Integer id;
    boolean recording = false;
    boolean stoprecord = false;

    public boolean isStart = false;
    public FileWriter file_writer;
    public boolean success = true;
    public boolean isInsert = false;
    public boolean calcdiff = false;
    public boolean islocalize = false;
    PointF get;
    public boolean isErrror = false;
    public double errorvalue;
    public Multimap<PointF, Float> result= HashMultimap.create();

    //Sensor events
    private float [] RotationMatrix = new float[16];
    private float [] mOrientationAngles = new float[3];
    private SensorEvent MagnetData;;
    private SensorEvent RotVectorData;
    private SensorEvent AccelVector;
    public SensorEvent Gravity;
    public  SensorEvent RawMagnetData;
    public SensorEvent GeoRotation;
    SensorEvent MagFast;


    //For tilt and azimuth
    public float [] earthAcc= new float[4];
    private float gravity[] = new float[3];
    private final float beta = (float) 0.8;
    float[] magValues = new float[3];
    float[] values = new float[3];
    Integer degreeDisplay;
    Integer degree;
    int offset;

    //Average filter
    private static int k;
    private float[] prevAvg;
    private static float alpha;
    private float[] avg;

    //For coordinates
    public static final int COL = 5;
    public static final int LOW = 10;
    public boolean ismeasure=false;

    //Array
    long curTimeAcc;
    long lastTimeAcc=System.currentTimeMillis();
    public ArrayList<Float>sensorData= new ArrayList<>();
    public ArrayList<Double> xcordList= new ArrayList<>();
    public ArrayList<Double>ycordList =  new ArrayList<>();
    public ArrayList<Double>probAll= new ArrayList<>();
    public ArrayList<Double>probFinalList= new ArrayList<>();
    float sum_x=0;
    float sum_y=0;
    float probAdd=0;
    double probFinal=0;


    //test for pdr
    private static final String TAG = InsertData.class.getSimpleName();
    private static final int RATE = SensorManager.SENSOR_DELAY_GAME;

    private AccelerationEventListener accEvent;
    private GyroscopeEventListener gyroEvent;
    private MagneticfieldEventListener magEvent;

    int stepCount = 0;
    float stepLength = 0.0f;
    float gyroValue = 0.0f;
    float azimuthValue = 0.0f;
    float imuLocationX = 0.0f;
    float imuLocationY = 0.0f;

    public boolean isRecord = false;
    public long startTime;
    public boolean isStep=false;

 private static final int PERMISSION_REQUEST_CODE=1;
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.insert_data);

        TextView localize=(TextView)findViewById(R.id.localization);
        if (Build.VERSION.SDK_INT >= 23)
        {
            if (checkPermission())
            {
                // Code for above or equal 23 API Oriented Device
                // Your Permission granted already .Do next code
            } else {
                requestPermission(); // Code for permission
            }
        }
        else
        {

            // Code for Below 23 API Oriented Device
            // Do next code
        }
        sm = (SensorManager) getSystemService(SENSOR_SERVICE);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), SensorManager.SENSOR_DELAY_NORMAL);
        sm.registerListener(this, sm.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR), SensorManager.SENSOR_DELAY_NORMAL);
        magnetometer = sm.getDefaultSensor(magnetometer.TYPE_MAGNETIC_FIELD);
        if (magnetometer == null) {
            Toast.makeText(this, "Magnetometer not available", Toast.LENGTH_SHORT).show();
            finish();
        }


        //pdr
        accEvent = new AccelerationEventListener(this);
        gyroEvent = new GyroscopeEventListener(this, accEvent, magEvent);
        magEvent = new MagneticfieldEventListener(this, accEvent, gyroEvent);

        sm.registerListener(accEvent, sm.getDefaultSensor(Sensor.TYPE_ACCELEROMETER), RATE);
        sm.registerListener(gyroEvent, sm.getDefaultSensor(Sensor.TYPE_GYROSCOPE), RATE);
        sm.registerListener(magEvent, sm.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD), RATE);

        if (isStart == false) {
            isStart = true;

            startTime = SystemClock.uptimeMillis();
        }

        magnetismx = (TextView) findViewById(R.id.magnetismx);
        magnetismy = (TextView) findViewById(R.id.magnetismy);
        magnetismz = (TextView) findViewById(R.id.magnetismz);
        magnetismd = (TextView) findViewById(R.id.magnetismd);

        magnetometer = sm.getDefaultSensor(magnetometer.TYPE_MAGNETIC_FIELD);
        if (magnetometer == null) {
            Toast.makeText(this, "Magnetometer not available", Toast.LENGTH_SHORT).show();
            finish();
        }
        if (isStart == false) {
            isStart = true;

            startTime = SystemClock.uptimeMillis();
        }
    }

    private boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(InsertData.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }
    private void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(InsertData.this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(InsertData.this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            ActivityCompat.requestPermissions(InsertData.this, new String[]{android.Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERMISSION_REQUEST_CODE);
        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case PERMISSION_REQUEST_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.e("value", "Permission Granted, Now you can use local drive .");
                } else {
                    Log.e("value", "Permission Denied, You cannot use local drive .");
                }
                break;
        }
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.deleteDatabase:
                DBHelper.getInstance().deleteDataMag();
                break;

            case R.id.btnRecord:
                recording = true;
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                int second = calendar.get(Calendar.SECOND);

                File folder = new File(Environment.getExternalStorageDirectory() + "/bimal" + hour + "-" + minute + "-" + second);
                if (!folder.exists()) {
                    success = folder.mkdir();
                }
                // Do something on success
                String csv = folder.getAbsolutePath() + "/Accelerometer.csv";
                try {
                    file_writer = new FileWriter(csv, true);

                    if (isStart == false) {
                        String s = "X-Axis, Y-Axis, Z-Axis, ERROR \n";
                        file_writer.append(s);
                        isStart = true;
                        Toast.makeText(getBaseContext(), "Data Recording Started", Toast.LENGTH_SHORT).show();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.btnStop:
                stoprecord = true;
                try {
                    file_writer.close();
                    Toast.makeText(getBaseContext(), "Data Recording Stopped", Toast.LENGTH_SHORT).show();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                break;

            case R.id.showDiff:
                calcdiff = true;
                Toast.makeText(getApplicationContext(), "finding difference", Toast.LENGTH_SHORT).show();
                break;

            case R.id.loadData:
                LoadData();
                break;

            case R.id.insertData:
                insertData();
                break;

            case R.id.localaizeData:
            islocalize=true;
                break;
            case R.id.errorData:
                isErrror = true;
                break;
            case R.id.measure:
                ismeasure=true;
        }
    }



    public static void BackupDatabase() throws IOException {
        boolean success = true;
        File file = null;
        file = new File(Environment.getExternalStorageDirectory() + "/bimal");

        if (file.exists()) {
            success = true;
        } else {
            success = file.mkdir();
        }

        if (success) {
            String inFileName = "/data/data/com.bimal.mag_positioning/databases/Mag_Positioning.db";
            File dbFile = new File(inFileName);
            FileInputStream fis = new FileInputStream(dbFile);

            String outFileName = Environment.getExternalStorageDirectory() + "/bimal/Mag_Positioning.db";

            // Open the empty db as the output stream
            OutputStream output = new FileOutputStream(outFileName);

            // Transfer bytes from the inputfile to the outputfile
            byte[] buffer = new byte[1024];
            int length;
            while ((length = fis.read(buffer)) > 0) {
                output.write(buffer, 0, length);
            }

            output.flush();
            output.close();
            fis.close();
        }
    }

    public void LoadData() {
        /*
        TextView view = (TextView) findViewById(R.id.load);
        SQLiteDatabase db;
        db = openOrCreateDatabase(
                "Mag_Positioning.db"
                , SQLiteDatabase.CREATE_IF_NECESSARY
                , null
        );
        db.setVersion(1);
        db.setLocale(Locale.getDefault());
        db.setLockingEnabled(true);
        Cursor cur = db.query("Fingerprint", null, null, null, null, null, null);
        cur.moveToFirst();
        while (cur.isAfterLast() == false) {
            view.append("\n" + cur.getString(2) + "," + cur.getString(3) + "," + cur.getString(4) + "," +
                    cur.getString(7) + "," + "\n");
            cur.moveToNext();
        }
        cur.close();
    }
*/
        Cursor res = DBHelper.getInstance().getAllData();
        if (res.getCount() == 0) {
            showMessage("Error", "Nothing found");
            return;
        }
        StringBuffer buffer = new StringBuffer();
        while (res.moveToNext()) {
            buffer.append("Id :" + res.getInt(0) + "\n");
            buffer.append("MapId :" + res.getInt(1) + "\n");
            buffer.append("X :" + res.getFloat(2) + "\n");
            buffer.append("Y :" + res.getFloat(3) + "\n");
            buffer.append("X_Axis :" + res.getString(4) + "\n");
            buffer.append("Y_Axis :" + res.getFloat(5) + "\n");
            buffer.append("Z_Axis :" + res.getFloat(6) + "\n");
            buffer.append("Average :" + res.getString(7) + "\n");
            buffer.append("SD :" + res.getFloat(8) + "\n");
            buffer.append("XROT :" + res.getFloat(9) + "\n");
            buffer.append("YROT :" + res.getFloat(10) + "\n");
            buffer.append("ZROT :" + res.getFloat(11) + "\n");
            buffer.append("DEGREE :" + res.getFloat(12) + "\n\n");
        }
        showMessage("Data", buffer.toString());
    }
    public void showMessage(String title, String Message) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setCancelable(true);
        builder.setTitle(title);
        builder.setMessage(Message);
        builder.show();
    }

    public void writeToCsv(String x, String y, String z, String q, String t, String r, String v, String u) throws IOException {
        if (isStart == true) {
            String s = x + "," + y + "," + z + "," + q + "," + t + "," + r + ","+ v +","+ u + "\n";
            file_writer.append(s);
        }
    }

    // public  void CalcDiff(){
    //   float total;
    //   String z;
    //   TextView diff= (TextView) findViewById(R.id.calcDiff);
    //   SQLiteDatabase db;
    //  db= openOrCreateDatabase(
    //          "Mag_Positioning.db"
    //          , SQLiteDatabase.CREATE_IF_NECESSARY
    //          , null
    //  );
    //   db.setVersion(1);
    //  db.setLocale(Locale.getDefault());
    //  db.setLockingEnabled(true);
    //  Cursor cur = db.query("Fingerprint", null, null, null, null, null, null);
    // cur.moveToFirst();
    //  while (cur.isAfterLast() == false) {
    //     z= cur.getString(6);
    //     diff.setText(d + "\n");
    //     cur.moveToNext();
    //  }
    //  }


    @Override
    public void onSensorChanged(SensorEvent event) {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.CUPCAKE) {
            if(event.accuracy==sm. SENSOR_STATUS_UNRELIABLE){
                Toast.makeText(getApplicationContext(),"Please calibrate the device", Toast.LENGTH_SHORT).show();
            }
            switch (event.sensor.getType()) {
             /*  case Sensor.TYPE_GEOMAGNETIC_ROTATION_VECTOR:
                    GeoRotation=event;
                    break;

                case Sensor.TYPE_MAGNETIC_FIELD_UNCALIBRATED:

                    curTimeAcc = System.currentTimeMillis();
                    if (curTimeAcc - lastTimeAcc > 500) {
                        RawMagnetData = event;

                    }
                    break;
*/
                case Sensor.TYPE_MAGNETIC_FIELD:
                     MagFast = event;
                    curTimeAcc = System.currentTimeMillis();
                    if (curTimeAcc - lastTimeAcc > 500) {
                        MagnetData = event;
                        lastTimeAcc=curTimeAcc;
                           calculate();

                    }
                    break;
                /*
                    case Sensor.TYPE_ACCELEROMETER:
                        curTimeAcc = System.currentTimeMillis();
                        if(curTimeAcc - lastTimeAcc >40 ){
                            AccelVector=event;

                        }
                        break;

                case Sensor.TYPE_GRAVITY:
                    Gravity = event;
                    break;

*/
                case Sensor.TYPE_ROTATION_VECTOR:
                    RotVectorData = event;
                    sm.getRotationMatrixFromVector(RotationMatrix, RotVectorData.values);
                    break;

            }


        }
        /*
        float[] inv = new float[16];
        float[] geomagneticValuesAdjusted = new float[4];
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER){
            gravity[0] = beta * gravity[0] + (1 - beta) * Gravity.values[0];
            gravity[1] = beta * gravity[1] + (1 - beta) * Gravity.values[1];
            gravity[2] = beta * gravity[2] + (1 - beta) * Gravity.values[2];

        /*
            gravity[0]= AccelVector.values[0];
            gravity[1]= AccelVector.values[1];
            gravity[2]= AccelVector.values[2];
           */
        /*
        }else if(event.sensor.getType() == Sensor.TYPE_MAGNETIC_FIELD){
            //SensorManager.getRotationMatrix(RotationMatrix, null, gravity, MagnetData.values);
            geomagneticValuesAdjusted[0]=MagnetData.values[0];
            geomagneticValuesAdjusted[1]=MagnetData.values[1];
            geomagneticValuesAdjusted[2]=MagnetData.values[2];
            geomagneticValuesAdjusted[3]=0;
            android.opengl.Matrix.invertM(inv, 0,RotationMatrix,0);
            android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0, geomagneticValuesAdjusted,0 );
        }

        magnetismx.setText(String.valueOf(earthAcc[0]));
        magnetismy.setText(Float.toString(earthAcc[1]));
        magnetismz.setText(Float.toString(earthAcc[2]));
*/
        //sm.getRotationMatrix(RotationMatrix, null, AccelVector.values, MagnetData.values);
        // sm.getRotationMatrix(RotationMatrix, null, AccelVector.values, MagnetData.values
        //float[] inv = new float[16];

        //android.opengl.Matrix.invertM(inv, 0, RotationMatrix,0);
        //android.opengl.Matrix.multiplyMV(earthAcc, 0, inv, 0 , MagnetData.values ,0);

        //sm.getRotationMatrix(RotationMatrix, null, AccelVector.values, MagnetData.values);






        }


    private void calculate() {


       /* k = 1;
        alpha = 0.0f;
        avg = new float[3];
        prevAvg = new float[3];
        */

        //probability test
        Double probability;
        Float probDiff;
        Float SD;
        double xcordProb;
        double ycordProb;


        Long tsLong = System.currentTimeMillis() / 1000;
        String ts = tsLong.toString();
        float[] res = new float[3];

        //Database values
        Float xaxis_database;
        Float yaxis_database;
        Float zaxis_database;
        Float average_database;
        String cordTime = "";

        //for calibrated
        xaxis = MagnetData.values[0];
        yaxis = MagnetData.values[1];
        zaxis = MagnetData.values[2];

    /*    // initial magnetic sensor value
        xaxis = RawMagnetData.values[0];
        yaxis = RawMagnetData.values[1];
        zaxis = RawMagnetData.values[2];

        xbias = RawMagnetData.values[3];

        ybias = RawMagnetData.values[4];
        zbias = RawMagnetData.values[5];
*/
        //Applying rotation matrix
        xaxis_rot = RotationMatrix[0] * MagnetData.values[0] + RotationMatrix[1] * MagnetData.values[1] + RotationMatrix[2] * MagnetData.values[2];
        yaxis_rot = RotationMatrix[4] * MagnetData.values[0] + RotationMatrix[5] * MagnetData.values[1] + RotationMatrix[6] * MagnetData.values[2];
        zaxis_rot = RotationMatrix[8] * MagnetData.values[0] + RotationMatrix[9] * MagnetData.values[1] + RotationMatrix[10] * MagnetData.values[2];


        //Average filtering
      /* alpha = ((float)(k - 1) / k);
        avg[0] =  alpha * prevAvg[0] + (1 - alpha) * a;
        avg[1] = alpha * prevAvg[1] + (1 - alpha) * b;
        avg[2] = alpha * prevAvg[2] + (1 - alpha) * c;

        prevAvg[0] = avg[0];
        prevAvg[1] = avg[1];
        prevAvg[2] = avg[2];
        */

        average = (float) Math.sqrt((Math.pow(xaxis, 2) + Math.pow(yaxis, 2) + Math.pow(zaxis, 2)));


        // for finding azimuth angle
        magValues = lowPassFilter(MagFast.values, magValues);
        if (magValues == null) return;
        SensorManager.getOrientation(RotationMatrix, values);
        degree = (int) (Math.toDegrees(values[0]) + 360) % 360; // translate into (0, 360).
        degree = ((degree + 2)) / 5 * 5; // the value of degree is multiples of 5.
        offset = 350;
        degreeDisplay = roomDirection(degree, offset); // user-defined room direction.
        // Toast.makeText(getApplicationContext(), "degree"+ degree, Toast.LENGTH_SHORT).show();

        if (sensorData.size() < 70) {
            sensorData.add(average);
        }
        //send to database for limiting
        DBHelper.getInstance().sendDegree(degreeDisplay);

        //Dispaly the values
        magnetismx.setText(String.valueOf(xaxis));
        magnetismy.setText(Float.toString(yaxis));
        magnetismz.setText(Float.toString(zaxis));

        //magnetismd.setText(String.valueOf(calculateStandarddeviation(sensorData)));
        magnetismd.setText(String.valueOf(calculateStandarddeviation(sensorData)));
        if (isStep) {
            Toast.makeText(getApplicationContext(), "Data insertion started", Toast.LENGTH_SHORT).show();
            DBHelper.getInstance().insert(1, imuLocationX, imuLocationY, xaxis, yaxis, zaxis, average, calculateStandarddeviation(sensorData), xaxis_rot, yaxis_rot, zaxis_rot, degreeDisplay);
        }
        isStep = false;
        if (calcdiff) {
            TextView diff = (TextView) findViewById(R.id.calcDiff);


            Cursor cur = DBHelper.getInstance().selectDegree();

            cur.moveToFirst();
            HashMap<PointF, Float> difference = new HashMap<>();
            Multimap<PointF, Float> result = HashMultimap.create();
            Multimap<PointF, Double> probResult = HashMultimap.create();
            //This hashmap is for creating array of pointf in one location id

            //HashMap<Integer,List<PointF>>find=new HashMap<>();
            //List<PointF> []tmp = new List[4];
            // for(int i = 0; i < 4; i++){
            // tmp[i] = new ArrayList<>();
            // }


            // Integer mapid;


            if (cur.isLast() == false) {

                while (cur.moveToNext()) {
                    //mapid=cur.getInt(1);
                    PointF location = new PointF(cur.getInt(2), cur.getInt(3));
                    xaxis_database = Float.valueOf(cur.getString(4));
                    yaxis_database = Float.valueOf(cur.getString(5));
                    zaxis_database = Float.valueOf(cur.getString(6));
                    average_database = Float.valueOf(cur.getString(7));
                   // SD = Float.valueOf(cur.getString(8));
                    //probDiff = average - average_database;

                   //probability = (1 / (Math.sqrt(2 * Math.PI * 2 * SD * SD))) * Math.exp(-((probDiff) * (probDiff)) / (2 * SD * SD));
                    // total = (Math.abs(average - average_database) + Math.abs(xaxis - xaxis_database) + Math.abs(yaxis - yaxis_database) + Math.abs(zaxis - zaxis_database)) /
                    //     (Math.abs(average + average_database) + Math.abs(xaxis + xaxis_database) + Math.abs(yaxis + yaxis_database) + Math.abs(zaxis + zaxis_database));

                    // total = ((Math.abs(average - average_database) + Math.abs(xaxis_rot - Float.valueOf(cur.getString(9))) + Math.abs(yaxis_rot - Float.valueOf(cur.getString(10))) + Math.abs(zaxis_rot - Float.valueOf(cur.getString(11) )))/
                    //         ((Math.abs(average - average_database) + Math.abs(xaxis_rot - Float.valueOf(cur.getString(9))) + Math.abs(yaxis_rot - Float.valueOf(cur.getString(10))) + Math.abs(zaxis_rot - Float.valueOf(cur.getString(11) )))));
                    total = Float.valueOf((float) Math.sqrt((Math.pow((xaxis - xaxis_database), 2) + Math.pow((yaxis - yaxis_database), 2) +
                            Math.pow((zaxis - zaxis_database), 2) + Math.pow((average - average_database), 2))));
                   // probAll.add(probability);
                    result.put(location, total);
                    // probResult.put(location, probability);
                    //array.add(location);

                    //
                    //tmp[mapid - 1].add(location);


                    //find.put(mapid, array);


                }
                /*if (!probAll.isEmpty()) {
                    for (int i = 0; i < probAll.size(); i++) {
                        probAdd += probAll.get(i);
                    }
                }
                if (!probAll.isEmpty()) {
                    for (int i = 0; i < probAll.size(); i++) {
                        probFinal = probAll.get(i) / probAdd;
                        probFinalList.add(probFinal);
                    }
                }

                cur.moveToFirst();
                int k = 0;
                if (cur.isLast() == false) {

                    while (cur.moveToNext()) {
                        xcordProb = cur.getFloat(2) * probFinalList.get(k);
                        xcordList.add(xcordProb);
                        ycordProb = cur.getFloat(3) * probFinalList.get(k);
                        ycordList.add(ycordProb);
                        k++;
                    }

                }

                if (!xcordList.isEmpty())
                    for (int i = 0; i < xcordList.size(); i++) {
                        sum_x += xcordList.get(i);
                    }

                if (!ycordList.isEmpty()) {
                    for (int i = 0; i < ycordList.size(); i++) {
                        sum_y += ycordList.get(i);
                    }

                }
*/
                diff.setText(String.valueOf(result));

                //find.put(i + 1, tmp[i]);
                // }
/*
                xcordList.clear();
                ycordList.clear();
                probAll.clear();
                probFinalList.clear();
                sum_x = 0;
                sum_y = 0;
                probAdd = 0;

*/
                if (islocalize) {

                    TextView localize = (TextView) findViewById(R.id.localization);
                    Map.Entry<PointF, Float> min = Collections.min(result.entries(), new Comparator<Map.Entry<PointF, Float>>() {
                        @Override
                        public int compare(Map.Entry<PointF, Float> entry1, Map.Entry<PointF, Float> entry2) {
                            return entry1.getValue().compareTo(entry2.getValue());
                        }
                    });
                    get = min.getKey();
                    localize.setText(String.valueOf(get) + ts);
                    cordTime = String.valueOf(get) + ts;
                    int xloc = (int) get.x;
                    int yloc = (int) get.y;
                    if (isErrror) {
                        TextView E1 = (EditText) findViewById(R.id.xCord);
                        TextView E2 = (EditText) findViewById(R.id.yCord);
                        TextView errortext = (TextView) findViewById(R.id.errorText);

                        String tmp2 = E1.getText().toString();
                        String tmp3 = E2.getText().toString();
                        if (tmp2.equals("") || tmp2.equals(""))
                            Toast.makeText(getApplicationContext(), "Please insert  Reference Co-Ordinates", Toast.LENGTH_SHORT).show();
                        int test1 = Integer.parseInt(String.valueOf(tmp2));
                        int test2 = Integer.parseInt(String.valueOf(tmp3));

                        //errorvalue=Math.sqrt((xloc-test1)*(xloc-test1) + (yloc-test2)*(yloc-test2));
                        errortext.setText(String.valueOf(errorvalue));
                    }


                    //code for comparing string in a hashmap and iterating
                    //Iterator<Entry<Integer,List<PointF>>> iter = find.entrySet().iterator();

                    // while(iter.hasNext()){
                    //    Map.Entry<Integer,List<PointF>> entry = iter.next();
                    //   if(String.valueOf(entry.getValue()).equals(get)){

                    //    Integer done=entry.getKey();
                    //     localize.setText(String.valueOf(done));
                    // }
                    // localize.setText(String.valueOf(entry.getKey())+"\n");
                    // Toast.makeText(getApplicationContext(),"done",Toast.LENGTH_SHORT).show();
                    // }

                    //code for iterating the hashmap
                    //Set<Map.Entry<Integer,List<PointF>>>set=find.entrySet();

                    // for(Map.Entry<Integer,List<PointF>> me:set){
                    //  localize.setText(String.valueOf(me.getValue()));
                    // }

                }


            }


            if (!recording) {
                return;
            }
            if (stoprecord) {
                return;
            }

            try {

                writeToCsv(Float.toString(xaxis), Float.toString(yaxis), Float.toString(zaxis), Float.toString(xaxis_rot), Float.toString(yaxis_rot), Float.toString(zaxis_rot), String.valueOf(errorvalue), ts);
            } catch (IOException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();

            }
        }
    }
    private int roomDirection(Integer myDegree, Integer myOffset) {
		/*
		 * define room direction as 270 degree.
		 */
        int tmp = (int)(myDegree - myOffset);
        if(tmp < 0) tmp += 360;
        else if(tmp >= 360) tmp -= 360;
        return tmp;
    }

    private float[] lowPassFilter(float[] input, float[] output) {
        /*
		 * low pass filter algorithm implement.
		 */
        if (output == null) return input;
        for (int i = 0; i < input.length; i++) {
            output[i] = output[i] + alpha * (input[i] - output[i]);
        }
        return output;
    }
    private void insertData() {
        TextView x = (EditText) findViewById(R.id.editText1);
        TextView y = (EditText) findViewById(R.id.editText2);
        TextView e = (EditText) findViewById(R.id.mapid);

        String tmp1 = e.getText().toString();
        String tmp2 = x.getText().toString();
        String tmp3 = y.getText().toString();

        if(tmp1.equals("") || tmp2.equals("") || tmp3.equals(""))
            Toast.makeText(getApplicationContext(), "Please insert Co-Ordinates First", Toast.LENGTH_SHORT).show();

        if(!tmp1.equals("") && !tmp2.equals("") && !tmp3.equals("")){
            int z1= Integer.parseInt(tmp1);
            int x1 = Integer.parseInt(tmp2);
            int y1 = Integer.parseInt(tmp3);

            Toast.makeText(getApplicationContext(),"Data insertion started", Toast.LENGTH_SHORT).show();
            DBHelper.getInstance().insert(z1, x1, y1, xaxis, yaxis, zaxis, average,calculateStandarddeviation(sensorData),xaxis_rot,yaxis_rot,zaxis_rot, (int) azimuthValue);

        }
    }

    private float calculateMean(ArrayList<Float>sensorData){
        int sum = 0;
        float mean;
        if (!sensorData.isEmpty())
            for (int i=10 ; i<sensorData.size(); i++) {
                sum += sensorData.get(i);
            }
           mean = sum / sensorData.size();
           return  mean;
    }

    private float calculateStandarddeviation(List<Float> sensorData) {
        int sum = 0;
        if (!sensorData.isEmpty())
            for (float data : sensorData) {
                sum += data;
            }
        double mean = sum / sensorData.size();
        double temp=0;
        for(int i=0 ; i<sensorData.size(); i++){
            float val= sensorData.get(i);
            double squrDiffToMean = Math.pow(val-mean,2);
            temp+=squrDiffToMean;

        }
        double meanofDiffs=  temp /  (sensorData.size());

       return (float) Math.sqrt(meanofDiffs);
       //magnetismd.setText(String.valueOf(sensorData.get(1)));

    }

    public String calculateCoordinates(List<Double>xcordList, List<Double>ycordList){
        if (!xcordList.isEmpty())
            for (Double data : xcordList) {
                sum_x += data;
            }
            if(!ycordList.isEmpty()){
                for (Double data : ycordList) {
                    sum_y += data;
                }
            }
        return sum_x+"\n"+sum_y;

    }

    @Override
    protected void onPause() {
        super.onPause();
        sm.unregisterListener(gyroEvent);
        sm.unregisterListener(accEvent);
        sm.unregisterListener(this);
        sm.unregisterListener(magEvent);
        sensorData.clear();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        sm.unregisterListener(gyroEvent);
        sm.unregisterListener(accEvent);
        sm.unregisterListener(this);
        sm.unregisterListener(magEvent);
        sensorData.clear();
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    // for pdr

    public void getStepCount(int step) {
        stepCount = step;
        isStep=true;
        float i = 0.0f;
        if(stepCount<=2){
            i=azimuthValue;
        }else if (stepCount>2){
            i=gyroValue;
        }

            imuLocationX = (float) (imuLocationX + (stepLength * -Math.cos(Math.toRadians(-i))));
            imuLocationY = (float) (imuLocationY + (stepLength * -Math.sin(Math.toRadians(-i))));
            magnetismx.setText(imuLocationX + "\n"+ imuLocationY+"\n"+ stepCount);

        }


    public void setStepLength(float stepSize) {
        stepLength = (float) (stepSize/0.45);

    }

    public void getAzimuth(float azimuth) {
        azimuthValue = azimuth;
        Log.e("azim" , "a" + azimuthValue);

    }



    public void setGyroYaw(float compDeg) {
        gyroValue = compDeg;
    }
}

