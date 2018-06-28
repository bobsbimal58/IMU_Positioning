package com.bimal.mag_positioning;

import android.app.Application;

/**
 * Created by wmcs on 6/23/2017.
 */

public class BaseApp extends Application {
    private static BaseApp mInstance;

    @Override
    public void onCreate() {
        super.onCreate();
        mInstance = this;
    }

    public static Application getApp() {
        return mInstance;
    }
}
