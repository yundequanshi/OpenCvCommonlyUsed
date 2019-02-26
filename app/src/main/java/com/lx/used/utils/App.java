package com.lx.used.utils;

import android.app.Application;
import org.opencv.android.OpenCVLoader;

public class App extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        OpenCVLoader.initDebug();
    }
}
