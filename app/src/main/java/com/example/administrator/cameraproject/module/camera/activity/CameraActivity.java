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

    // 相机比例
    private float[] scales;
    // 闪光灯模式
    private int[] modes;
    // 闪光灯图标
    private int[] flashIconRes;
    // 当前相机比例
    private int mCurFlashModePos = 0;
    // 当前闪光灯模式
    private int mCurScalesPos = 0;
    // 预览显示View
    private TextureView mTextureView;
    // 拍照按钮
    private ImageView mIvTakePhoto;
    // presenter
    private CameraFeaturePresenter mCameraFeaturePresenter;
    // 前后置转换按钮
    private ImageView mIvChangeCamera;
    // 相机比例转换按钮
    private ImageView mIvCameraScale;
    // 闪光灯模式转换按钮
    private ImageView mIvFlashMode;

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
        mIvChangeCamera = findViewById(R.id.iv_change_camera);
        mIvCameraScale = findViewById(R.id.iv_change_scale);
        mIvFlashMode = findViewById(R.id.iv_change_flash_mode);
    }

    private void initData() {
        mIvChangeCamera.setOnClickListener(this);
        mIvTakePhoto.setOnClickListener(this);
        mIvCameraScale.setOnClickListener(this);
        mIvFlashMode.setOnClickListener(this);
        mTextureView.setOnClickListener(this);
        mTextureView.setSurfaceTextureListener(this);
        mCameraFeaturePresenter = new CameraFeaturePresenter(this);
        // 16比9、4比3、1比1三种尺寸比例
        scales = new float[]{16f / 9, 4f / 3, 1f};
        // 关闭、打开、常亮三种闪光模式
        modes = new int[]{CameraConfig.FLASH_OFF, CameraConfig.FLASH_ON, CameraConfig.FLASH_ALWAYS};
        // 三种图标对应关闭、打开、常亮
        flashIconRes = new int[]{R.mipmap.ic_flash_off, R.mipmap.ic_flash_on, R.mipmap.ic_flash_torch};
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.iv_take_picture:
                // 拍照
                mCameraFeaturePresenter.takePhoto();
                break;
            case R.id.iv_change_camera:
                // 设置前后置镜头
                mCameraFeaturePresenter.changeCamera();
                break;
            case R.id.iv_change_scale:
                // 设置相机比例
                mCameraFeaturePresenter.setCameraRatio(scales[++mCurScalesPos % scales.length]);
                break;
            case R.id.iv_change_flash_mode:
                // 设置闪光模式
                mCameraFeaturePresenter.setFlashMode(modes[++mCurFlashModePos % modes.length]);
                // 设置图标
                mIvFlashMode.setImageResource(flashIconRes[mCurFlashModePos % flashIconRes.length]);
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

    /**
     * presenter进行相关引用清除
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mCameraFeaturePresenter.onDestroy();
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
