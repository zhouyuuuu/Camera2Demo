package com.example.administrator.cameraproject.module.camera.application;

import android.annotation.SuppressLint;
import android.app.Application;
import android.content.Context;

/**
 * Edited by Administrator on 2018/4/10.
 */

public class CameraApplication extends Application {
    @SuppressLint("StaticFieldLeak")
    private static Context mContext;

    /**
     * 获得App的Context
     *
     * @return 上下文
     */
    public static Context getApplication() {
        return mContext;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        mContext = this;
    }
}
