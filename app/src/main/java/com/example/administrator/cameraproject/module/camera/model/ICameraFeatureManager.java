package com.example.administrator.cameraproject.module.camera.model;

import android.app.Activity;
import android.view.TextureView;

/**
 * 相机功能管理者，Model，管理闪光灯、比例、镜头、预览、拍照
 * Edited by Administrator on 2018/4/10.
 */

public interface ICameraFeatureManager {
    /**
     * 开始预览
     *
     * @param textureView textureView
     * @param activity    activity
     */
    void openCameraAndStartPreview(TextureView textureView, Activity activity);

    /**
     * 拍照
     */
    void takePhoto();

    /**
     * 停止预览
     */
    void stopCameraAndPreview();

    /**
     * 重启预览
     */
    void openCameraAndStartPreview();

    /**
     * Presenter执行onDestroy
     */
    void onDestroy();

    /**
     * 改变相机模式（前后置）
     */
    void changeCameraMode();

    /**
     * 改变照片比例
     *
     * @param ratio 比例
     */
    void setCameraRatio(float ratio);

    /**
     * 改变闪关灯模式
     *
     * @param flashMode 模式
     */
    void setFlashMode(int flashMode);

    /**
     * 设置焦点
     *
     * @param x x
     * @param y x
     */
    void setFocusPoint(int x, int y);

    /**
     * 设置缩放
     *
     * @param distance 缩放量
     */
    void setScale(int distance);
}
