package com.example.administrator.cameraproject.module.camera.presenter;

import android.graphics.Bitmap;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.widget.ImageView;

import com.example.administrator.cameraproject.module.camera.model.IPhotoModel;
import com.example.administrator.cameraproject.module.camera.model.PhotoLoader;
import com.example.administrator.cameraproject.module.camera.view.IPhotoView;

import java.lang.ref.WeakReference;

/**
 * 照片业务层
 * Edited by Administrator on 2018/4/10.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PhotoPresenter implements IPhotoPresenter {

    // View层弱引用
    private WeakReference<IPhotoView> mPhotoViewWeakReference;
    // model
    private IPhotoModel mPhotoModel;

    public PhotoPresenter(IPhotoView iPhotoView) {
        this.mPhotoModel = new PhotoLoader(this);
        this.mPhotoViewWeakReference = new WeakReference<>(iPhotoView);
    }

    /**
     * 加载图片
     *
     * @param fileName  文件名
     * @param imageView imageView
     */
    @Override
    public void loadPhoto(String fileName, ImageView imageView) {
        mPhotoModel.loadBitmap(fileName, imageView);
    }

    /**
     * 图片加载完成
     *
     * @param bitmap 图片
     */
    @Override
    public void photoLoaded(Bitmap bitmap) {
        IPhotoView iPhotoView = mPhotoViewWeakReference.get();
        if (iPhotoView == null) return;
        iPhotoView.onBitmapLoaded(bitmap);
    }

    /**
     * 清楚弱引用,model层销毁
     */
    @Override
    public void onDestroy() {
        if (mPhotoViewWeakReference != null) {
            mPhotoViewWeakReference.clear();
            mPhotoViewWeakReference = null;
        }
        mPhotoModel.onDestroy();
    }
}
