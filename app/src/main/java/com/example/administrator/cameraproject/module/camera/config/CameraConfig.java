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
    // 后置模式
    public static final int CAMERA_REAR_MODE = 101;
    // 前置模式
    public static final int CAMERA_FRONT_MODE = 102;
    // 前置摄像头照片修正角度
    public static final int CAMERA_FRONT_PHOTO_REVISE_DEGREE = -90;
    // 前置摄像头照片修正角度
    public static final int CAMERA_REAR_PHOTO_REVISE_DEGREE = 90;
    // 照片比例16比9
    public static final float CAMERA_PHOTO_RATIO_16_9 = 16F / 9;
    // 照片比例4比3
    public static final float CAMERA_PHOTO_RATIO_4_3 = 4F / 3;
    // 照片比例1比1
    public static final float CAMERA_PHOTO_RATIO_1_1 = 1F;
    // 焦点占width比
    public static final float FOCUS_WIDTH_RATIO_DEFAULT = 0.5f;
    // 焦点占height比
    public static final float FOCUS_HEIGHT_RATIO_DEFAULT = 0.5f;
}
