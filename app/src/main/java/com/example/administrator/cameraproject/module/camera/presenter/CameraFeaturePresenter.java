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
 * 相机功能业务层
 * Edited by Administrator on 2018/4/10.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraFeaturePresenter implements ICameraFeaturePresenter {

    private ICameraFeatureManager mCameraFeatureManager;
    private WeakReference<ICameraFeatureView> mCameraFeatureViewWeakReference;

    public CameraFeaturePresenter(ICameraFeatureView iCameraFeatureView) {
        this.mCameraFeatureManager = new CameraFeatureManager(this);
        mCameraFeatureViewWeakReference = new WeakReference<>(iCameraFeatureView);
    }

    /**
     * 开始预览
     *
     * @param textureView textureView
     * @param activity    activity
     */
    @Override
    public void startPreview(TextureView textureView, Activity activity) {
        mCameraFeatureManager.openCameraAndStartPreview(textureView, activity);
    }

    /**
     * 清除对View的引用
     */
    @Override
    public void onDestroy() {
        mCameraFeatureViewWeakReference.clear();
        mCameraFeatureViewWeakReference = null;
        mCameraFeatureManager.onDestroy();
    }

    /**
     * 停止预览，会关闭摄像头
     */
    @Override
    public void onStop() {
        mCameraFeatureManager.stopCameraAndPreview();
    }

    /**
     * 重启预览
     */
    @Override
    public void onRestart() {
        mCameraFeatureManager.openCameraAndStartPreview();
    }

    /**
     * 拍照
     */
    @Override
    public void takePhoto() {
        mCameraFeatureManager.takePhoto();
    }

    /**
     * 拍照回调
     *
     * @param fileName 图片文件名
     */
    @Override
    public void onPhotoToken(String fileName) {
        ICameraFeatureView iCameraFeatureView = mCameraFeatureViewWeakReference.get();
        if (iCameraFeatureView == null) return;
        iCameraFeatureView.onPhotoToken(fileName);
    }

    /**
     * 改变相机模式（前后置）
     */
    @Override
    public void changeCamera() {
        mCameraFeatureManager.changeCameraMode();
    }

    /**
     * 改变照片比例
     *
     * @param ratio 比例
     */
    @Override
    public void setCameraRatio(float ratio) {
        mCameraFeatureManager.setCameraRatio(ratio);
    }

    /**
     * 改变闪关灯模式
     *
     * @param flashMode 模式
     */
    @Override
    public void setFlashMode(int flashMode) {
        mCameraFeatureManager.setFlashMode(flashMode);
    }

    /**
     * 设置焦点
     *
     * @param x x
     * @param y x
     */
    @Override
    public void setFocusPoint(int x, int y) {
        mCameraFeatureManager.setFocusPoint(x, y);
    }

    /**
     * 设置缩放
     *
     * @param distance 缩放量
     */
    @Override
    public void setScale(int distance) {
        mCameraFeatureManager.setScale(distance);
    }
}
