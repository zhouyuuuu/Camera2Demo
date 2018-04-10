package com.example.administrator.cameraproject.module.camera.view;

/**
 * Edited by Administrator on 2018/4/10.
 */

public interface ICameraFeatureView {
    /**
     * 拍照完成回调
     * @param fileName 文件名
     */
    void onPhotoToken(String fileName);
}
