package com.example.administrator.cameraproject.module.camera.model;

import android.widget.ImageView;

/**
 * 照片Model层
 * Edited by Administrator on 2018/4/10.
 */

public interface IPhotoModel {
    /**
     * 加载图片
     *
     * @param fileName  文件名
     * @param imageView imageView
     */
    void loadBitmap(String fileName, ImageView imageView);
}
