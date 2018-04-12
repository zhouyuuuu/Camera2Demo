package com.example.administrator.cameraproject.module.camera.activity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.v7.app.AppCompatActivity;
import android.widget.ImageView;

import com.example.administrator.cameraproject.R;
import com.example.administrator.cameraproject.module.camera.config.CameraConfig;
import com.example.administrator.cameraproject.module.camera.presenter.IPhotoPresenter;
import com.example.administrator.cameraproject.module.camera.presenter.PhotoPresenter;
import com.example.administrator.cameraproject.module.camera.view.IPhotoView;

/**
 * Edited by Administrator on 2018/4/10.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class PhotoActivity extends AppCompatActivity implements IPhotoView {

    // 显示照片的ImageView
    private ImageView mIvPhoto;
    // presenter
    private IPhotoPresenter mPhotoPresenter;
    // 是否加载过请求了
    private boolean mIsLoaded = false;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.photo_activity);
        initView();
        initData();
    }

    private void initView(){
        mIvPhoto = findViewById(R.id.iv_photo);
    }

    private void initData(){
        mPhotoPresenter = new PhotoPresenter(this);
    }

    /**
     * 这里回调的时候，View的长宽会被计算好
     *
     * @param hasFocus 获得焦点
     */
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        if (mIsLoaded) return;
        if (hasFocus) {
            Intent intent = getIntent();
            if (intent != null) {
                // 发起加载图片请求
                mIsLoaded = true;
                final String fileName = intent.getStringExtra(CameraConfig.INTENT_KEY_BITMAP);
                mPhotoPresenter.loadPhoto(fileName, mIvPhoto);
            }
        }
    }

    /**
     * 图片加载完成回调
     *
     * @param bitmap 图片
     */
    @Override
    public void onBitmapLoaded(Bitmap bitmap) {
        mIvPhoto.setImageBitmap(bitmap);
    }

    /**
     * presenter也要销毁
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        mPhotoPresenter.onDestroy();
    }
}
