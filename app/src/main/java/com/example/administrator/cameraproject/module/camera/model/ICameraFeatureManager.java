package com.example.administrator.cameraproject.module.camera.model;

import android.app.Activity;
import android.view.TextureView;

/**
 * Edited by Administrator on 2018/4/10.
 */

public interface ICameraFeatureManager {
    /**
     * 开始预览
     *
     * @param textureView textureView
     * @param activity    activity
     */
    void startPreview(TextureView textureView, Activity activity);

    /**
     * 拍照
     */
    void takePhoto();

    /**
     * 停止预览
     */
    void stopPreview();

    /**
     * 重启预览
     */
    void restartPreview();

    /**
     * Presenter执行onDestroy
     */
    void onDestroy();
}
