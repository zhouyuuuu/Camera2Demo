package com.example.administrator.cameraproject.module.camera.config;

import android.hardware.camera2.CameraCharacteristics;
import android.os.Build;
import android.os.Environment;
import android.support.annotation.RequiresApi;

/**
 * 常量类
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
    // 闪光灯关闭
    public static final int FLASH_OFF = 0;
    // 闪光灯自动
    public static final int FLASH_AUTO = 1;
    // 闪光灯常亮
    public static final int FLASH_ALWAYS = 2;
    // 闪光灯打开
    public static final int FLASH_ON = 3;
}
