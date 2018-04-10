package com.example.administrator.cameraproject.module.camera.presenter;

import android.app.Activity;
import android.os.Build;
import android.support.annotation.RequiresApi;
import android.view.TextureView;

import com.example.administrator.cameraproject.module.camera.model.CameraFeatureManager;
import com.example.administrator.cameraproject.module.camera.model.ICameraFeatureManager;
import com.example.administrator.cameraproject.module.camera.view.ICameraFeatureView;

import java.lang.ref.WeakReference;

/**
 * Edited by Administrator on 2018/4/10.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraFeaturePresenter implements ICameraFeaturePresenter {

    private ICameraFeatureManager cameraFeatureManager;
    private WeakReference<ICameraFeatureView> cameraFeatureViewWeakReference;

    public CameraFeaturePresenter(ICameraFeatureView iCameraFeatureView) {
        this.cameraFeatureManager = new CameraFeatureManager(this);
        cameraFeatureViewWeakReference = new WeakReference<>(iCameraFeatureView);
    }

    /**
     * 开始预览
     *
     * @param textureView textureView
     * @param activity    activity
     */
    @Override
    public void startPreview(TextureView textureView, Activity activity) {
        cameraFeatureManager.startPreview(textureView, activity);
    }

    /**
     * 清除对View的引用
     */
    @Override
    public void onDestroy() {
        cameraFeatureViewWeakReference.clear();
        cameraFeatureViewWeakReference = null;
        cameraFeatureManager.onDestroy();
    }

    /**
     * 停止预览，会关闭摄像头
     */
    @Override
    public void onStop() {
        cameraFeatureManager.stopPreview();
    }

    /**
     * 重启预览
     */
    @Override
    public void onRestart() {
        cameraFeatureManager.restartPreview();
    }

    /**
     * 拍照
     */
    @Override
    public void takePhoto() {
        cameraFeatureManager.takePhoto();
    }

    /**
     * 拍照回调
     *
     * @param fileName 图片文件名
     */
    @Override
    public void onPhotoToken(String fileName) {
        ICameraFeatureView iCameraFeatureView = cameraFeatureViewWeakReference.get();
        if (iCameraFeatureView == null) return;
        iCameraFeatureView.onPhotoToken(fileName);
    }
}
