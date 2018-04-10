package com.example.administrator.cameraproject.module.camera.activity;

import android.content.Intent;
import android.graphics.SurfaceTexture;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import com.example.administrator.cameraproject.R;
import com.example.administrator.cameraproject.module.camera.config.CameraConfig;
import com.example.administrator.cameraproject.module.camera.presenter.CameraFeaturePresenter;
import com.example.administrator.cameraproject.module.camera.view.ICameraFeatureView;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends AppCompatActivity implements View.OnClickListener, TextureView.SurfaceTextureListener, ICameraFeatureView {

    private TextureView mTextureView;
    // 拍照按钮
    private ImageView mIvTakePhoto;
    // presenter
    private CameraFeaturePresenter mCameraFeaturePresenter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camara_activity);
        initView();
        initData();
    }

    private void initView() {
        mTextureView = findViewById(R.id.textureview_camera);
        mIvTakePhoto = findViewById(R.id.iv_take_picture);
    }

    private void initData() {
        mTextureView.setOnClickListener(this);
        mIvTakePhoto.setOnClickListener(this);
        mTextureView.setSurfaceTextureListener(this);
        mCameraFeaturePresenter = new CameraFeaturePresenter(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_take_picture:
                mCameraFeaturePresenter.takePhoto();
                break;
        }
    }

    /**
     * TextureView 初始化完成才可以配置相机，在这里开启预览
     */
    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        mCameraFeaturePresenter.startPreview(mTextureView, this);
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {

    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        return false;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {

    }

    /**
     * presenter进行相关引用清除
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraFeaturePresenter.onDestroy();
    }

    /**
     * 照片回调
     *
     * @param fileName 文件名
     */
    @Override
    public void onPhotoToken(String fileName) {
        Intent intent = new Intent(CameraActivity.this, PhotoActivity.class);
        intent.putExtra(CameraConfig.INTENT_KEY_BITMAP, fileName);
        startActivity(intent);
    }

    @Override
    protected void onStop() {
        super.onStop();
        mCameraFeaturePresenter.onStop();
    }

    @Override
    protected void onRestart() {
        super.onRestart();
        mCameraFeaturePresenter.onRestart();
    }
}
