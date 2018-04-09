package com.example.administrator.cameraproject;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.RectF;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.widget.ImageView;

import java.nio.ByteBuffer;
import java.util.Arrays;

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraActivity extends AppCompatActivity implements View.OnClickListener {

    private static String mCameraID = "" + CameraCharacteristics.LENS_FACING_FRONT;
    private TextureView mTextureView;
    private ImageView mIvShow;
    private Handler mainHandler;
    private ImageReader mImageReader;
    private CameraManager mCameraManager;
    private CameraStateCallback mCameraStateCallback;
    private SurfaceTexture mTexture;
    private Surface mSurface;
    private CaptureRequest.Builder mRequestBuilder;
    private SessionStateCallback mSessionStateCallback;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.camara_activity);
        mIvShow = findViewById(R.id.iv_show);
        mTextureView = findViewById(R.id.textureview_camera);
        mainHandler = new Handler(getMainLooper());
        mCameraStateCallback = new CameraStateCallback();
        mSessionStateCallback = new SessionStateCallback();
        mTextureView.setOnClickListener(this);
        mTextureView.setSurfaceTextureListener(new TextureView.SurfaceTextureListener() {
            @Override
            public void onSurfaceTextureAvailable(final SurfaceTexture surface, final int width, final int height) {
                mTexture = mTextureView.getSurfaceTexture();
                mSurface = new Surface(mTexture);
                //后摄像头
                mImageReader = ImageReader.newInstance(mTextureView.getWidth(), mTextureView.getHeight(), ImageFormat.JPEG, 1);
                mImageReader.setOnImageAvailableListener(new ImageReader.OnImageAvailableListener() { //可以在这里处理拍照得到的临时照片 例如，写入本地
                    @Override
                    public void onImageAvailable(ImageReader reader) {
                        // 拿到拍照照片数据
                        Image image = reader.acquireNextImage();
                        ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                        byte[] bytes = new byte[buffer.remaining()];
                        buffer.get(bytes);//由缓冲区存入字节数组
                        final Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                        if (bitmap != null) {
                            mIvShow.setImageBitmap(bitmap);
                        }
                    }
                }, mainHandler);
                //获取摄像头管理
                mCameraManager = (CameraManager) getSystemService(Context.CAMERA_SERVICE);
                try {
                    if (ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(CameraActivity.this, Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                        ActivityCompat.requestPermissions(CameraActivity.this, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                        return;
                    }
                    //打开摄像头
                    if (mCameraManager != null) {
                        mCameraManager.openCamera(mCameraID, mCameraStateCallback, mainHandler);
                    }
                } catch (CameraAccessException e) {
                    e.printStackTrace();
                }
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
        });
    }

    @Override
    public void onClick(View v) {

    }

    /**
     * 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择）
     *
     * @param textureViewWidth  需要被进行对比的原宽
     * @param textureViewHeight 需要被进行对比的原高
     * @param preSizeList       需要对比的预览尺寸列表
     * @return 得到与原宽高比例最接近的尺寸
     */
    protected Size getCloselyPreSize(int textureViewWidth, int textureViewHeight,
                                     Size[] preSizeList) {
        for (Size size : preSizeList) {
            if ((size.getWidth() == textureViewHeight) && (size.getHeight() == textureViewWidth)) {
                return size;
            }
        }
        // 得到与传入的宽高比最接近的size
        float reqRatio = ((float) textureViewHeight) / textureViewWidth;
        float curRatio, deltaRatio;
        float deltaRatioMin = Float.MAX_VALUE;
        Size retSize = null;
        for (Size size : preSizeList) {
            curRatio = ((float) size.getWidth()) / size.getHeight();
            deltaRatio = Math.abs(reqRatio - curRatio);
            if (deltaRatio < deltaRatioMin) {
                deltaRatioMin = deltaRatio;
                retSize = size;
            }
        }
        return retSize;
    }

    private class CameraStateCallback extends CameraDevice.StateCallback {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            try {
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraID);
                // 获取摄像头支持的配置属性
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                // 获取摄像头支持的最大尺寸
                assert map != null;
                // 摄像头支持的输出尺寸
                Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
                // 找到最接近于textureView的宽高比例的输出比例
                Size targetSize = getCloselyPreSize(mTextureView.getWidth(), mTextureView.getHeight(), sizes);
                // 原来textureView的Rect
                RectF preRect = new RectF(0, 0, mTextureView.getWidth(), mTextureView.getHeight());
                // scale为短的除以长的，这边targetSize的width指的是长的一边，比如1920x1080，width指的是1920，而长的一边在我们的应用中是height，所以现在把targetSize的width当成height，height当成width
                float targetScale = 1F * targetSize.getHeight() / targetSize.getWidth();
                // 高不变
                int targetHeight = mTextureView.getHeight();
                // 宽为高乘以目标比例
                int targetWidth = (int) (mTextureView.getHeight() * targetScale);
                // 目标Rect
                RectF targetRect = new RectF(0, 0, targetWidth, targetHeight);
                // 变换矩阵
                Matrix matrix = new Matrix();
                matrix.setRectToRect(preRect, targetRect, Matrix.ScaleToFit.FILL);
                // 宽高比例变换
                mTextureView.setTransform(matrix);
                // 设置缓冲区大小
                mTexture.setDefaultBufferSize(targetSize.getWidth(), targetSize.getHeight());
                mRequestBuilder = camera.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                mRequestBuilder.addTarget(mSurface);
                camera.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), mSessionStateCallback, mainHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {

        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    }

    private class SessionStateCallback extends CameraCaptureSession.StateCallback {


        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            // 当摄像头已经准备好时，开始显示预览
            try {
                // 设置自动对焦模式
                mRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE,
                        CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 设置自动曝光模式
                mRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE,
                        CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                // 开始显示相机预览
                CaptureRequest previewRequest = mRequestBuilder.build();
                // 设置预览时连续捕获图像数据
                cameraCaptureSession.setRepeatingRequest(previewRequest,
                        null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    }


}
