package com.example.administrator.cameraproject.module.camera.view;

import android.graphics.Bitmap;

/**
 * Edited by Administrator on 2018/4/10.
 */

public interface IPhotoView {
    /**
     * 图片加载完成回调
     *
     * @param bitmap 图片
     */
    void onBitmapLoaded(Bitmap bitmap);
}
