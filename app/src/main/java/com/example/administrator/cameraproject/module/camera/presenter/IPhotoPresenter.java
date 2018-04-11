package com.example.administrator.cameraproject.module.camera.presenter;

import android.graphics.Bitmap;
import android.widget.ImageView;

/**
 * 照片业务层
 * Edited by Administrator on 2018/4/10.
 */

public interface IPhotoPresenter {
    /**
     * 加载图片
     *
     * @param fileName  文件名
     * @param imageView imageView
     */
    void loadPhoto(String fileName, ImageView imageView);

    /**
     * 图片加载完成
     *
     * @param bitmap 图片
     */
    void photoLoaded(Bitmap bitmap);

    /**
     * View在onDestroy
     */
    void onDestroy();
}
