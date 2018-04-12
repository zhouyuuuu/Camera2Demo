package com.example.administrator.cameraproject.module.camera.view;

/**
 * 相机功能视图层
 * Edited by Administrator on 2018/4/10.
 */

public interface ICameraView {
    /**
     * 拍照完成回调
     * @param fileName 文件名
     */
    void onPhotoToken(String fileName);
}
