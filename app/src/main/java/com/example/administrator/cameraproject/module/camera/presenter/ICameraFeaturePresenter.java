package com.example.administrator.cameraproject.module.camera.presenter;

import android.app.Activity;
import android.view.TextureView;

/**
 * Edited by Administrator on 2018/4/10.
 */

public interface ICameraFeaturePresenter {
    /**
     * 开始预览
     *
     * @param textureView textureView
     * @param activity    activity
     */
    void startPreview(TextureView textureView, Activity activity);

    /**
     * View执行onDestroy
     */
    void onDestroy();

    /**
     * View执行onStop
     */
    void onStop();

    /**
     * View执行onRestart
     */
    void onRestart();

    /**
     * 拍照
     */
    void takePhoto();

    /**
     * 拍照完成回调
     *
     * @param fileName 图片文件名
     */
    void onPhotoToken(String fileName);
}
