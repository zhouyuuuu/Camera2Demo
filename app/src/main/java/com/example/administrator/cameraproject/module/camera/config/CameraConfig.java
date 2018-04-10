package com.example.administrator.cameraproject.module.camera.config;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;

/**
 * Edited by Administrator on 2018/4/10.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraConfig {
    // INTENT的照片文件名的key
    public static final String INTENT_KEY_BITMAP = "bitmap";
    // 照片存储文件夹
    public static final String PHOTO_PATH = Environment.getExternalStorageDirectory() + "/CameraProjectPhoto/";
    // JPEG后缀
    public static final String JPEG = ".jpg";
    // 后置摄像头ID
    public static final String CAMERA_REAR_ID = "" + CameraCharacteristics.LENS_FACING_FRONT;
    // 前置摄像头ID
    public static final String CAMERA_FRONT_ID = "" + CameraCharacteristics.LENS_FACING_BACK;
}
