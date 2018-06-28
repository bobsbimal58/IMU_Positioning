package com.bimal.mag_positioning;


import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by wmcs on 5/4/2017.
 */

public class DBHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "Mag_Positioning.db";
    private static final int DB_VERSION = 1;

    //Table for measurement
    private static final String ID = "id";
    private static final String MAP_ID = "Mapid";
    private static final String COLXAXIS = "X_AXIS";
    private static final String TABLENAME = "Fingerprint";
    private static final String COLYAXIS = "Y_AXIS";
    private static final String COLZAXIS = "Z_AXIS";
    private static final String MAPX = "X_CORD";
    private static final String MAPY = "Y_CORD";
    private static final String DIFF = "Diff";

    private static final String FINAL = "FINAL";
    private static final String SD = "SD";
    private static final String XROT = "XROT";
    private static final String YROT = "YROT";
    private static final String ZROT = "ZROT";
    private static final String DEGREE = "DEGREE";

    Integer rDeg;
    private static final String KEY_FINGERPRINT_ID = "id";
    private static final String KEY_MAP_NAME = "map_name";
    private static final String KEY_POSITION_X = "position_x";
    private static final String KEY_POSITION_Y = "position_y";

    private static DBHelper mInstance;

    public DBHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public static DBHelper getInstance() {
        if (mInstance == null) {
            synchronized (DBHelper.class) {
                if (mInstance == null) {
                    mInstance = new DBHelper(BaseApp.getApp());
                }
            }
        }

        return mInstance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLENAME + "("
                + ID + " INTEGER PRIMARY KEY,"
                + MAP_ID + " INTEGER ," +
                MAPX + " INTEGER, " +
                MAPY + " INTEGER, " +
                COLXAXIS + " REAL, " +
                COLYAXIS + " REAL, " +
                COLZAXIS + " REAL, " +
                FINAL + " REAL, " +
                SD + " REAL, " +
                XROT + " REAL, " +
                YROT + " REAL, " +
                ZROT + " REAL, " +
                DEGREE + " INTEGER )";
        db.execSQL(createTable);

    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLENAME);
        onCreate(db);
    }

    public void insert(int a, float b, float c, float x, float y, float z, float d, float e, float g, float h, float i, int f) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues contentvalues = new ContentValues();
        contentvalues.put("Mapid", a);
        contentvalues.put("X_CORD", b);
        contentvalues.put("Y_CORD", c);
        contentvalues.put("X_AXIS", x);
        contentvalues.put("Y_AXIS", y);
        contentvalues.put("Z_AXIS", z);
        contentvalues.put("FINAL", d);
        contentvalues.put("SD", e);
        contentvalues.put("XROT", g);
        contentvalues.put("YROT", h);
        contentvalues.put("ZROT", i);
        contentvalues.put("Degree", f);
        db.insert("Fingerprint", null, contentvalues);
        db.close();
    }

    public void deleteAllFingerprints() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLENAME, null, null); // delete all fingerprints
        db.close();
    }

    public Cursor getAllData() {
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cur = db.rawQuery("select * from " + TABLENAME, null);
        return cur;
    }
    public void deleteDataMag(){
        SQLiteDatabase db = this.getWritableDatabase();
        db.execSQL("DELETE FROM " + TABLENAME);
        db.close();
    }

    public  void sendDegree(Integer degreeDisplay) {
        rDeg= degreeDisplay;
    }
    public Cursor selectDegree(){
        int limit= 60;
        float upperArea= rDeg-limit;
        if(upperArea<0){
            upperArea=360+upperArea;
        }
        float lowerArea= rDeg+limit;
        if(lowerArea>360){
            lowerArea=lowerArea-360;
        }
        SQLiteDatabase db = this.getWritableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM Fingerprint WHERE DEGREE BETWEEN '" +upperArea+"' AND '"+lowerArea+"' ",null );
        return cur;
    }
}