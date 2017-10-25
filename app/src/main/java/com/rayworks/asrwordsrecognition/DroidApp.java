package com.rayworks.asrwordsrecognition;

import android.app.Application;

import timber.log.Timber;

/**
 * Created by Sean on 10/19/17.
 */

public class DroidApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();

        Timber.plant(new Timber.DebugTree());
    }
}
