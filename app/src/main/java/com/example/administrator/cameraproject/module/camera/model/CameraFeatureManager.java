package com.example.administrator.cameraproject.module.camera.model;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.TotalCaptureResult;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.Image;
import android.media.ImageReader;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;
import android.widget.RelativeLayout;

import com.example.administrator.cameraproject.module.camera.application.CameraApplication;
import com.example.administrator.cameraproject.module.camera.config.CameraConfig;
import com.example.administrator.cameraproject.module.camera.presenter.ICameraFeaturePresenter;
import com.example.administrator.cameraproject.module.camera.util.StoreUtil;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * 相机功能管理者，Model，管理闪光灯、比例、镜头、预览、拍照
 * Edited by Administrator on 2018/4/10.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraFeatureManager implements ImageReader.OnImageAvailableListener, ICameraFeatureManager {

    // 后置模式
    private static final int CAMERA_REAR_MODE = 101;
    // 前置模式
    private static final int CAMERA_FRONT_MODE = 102;
    // 子线程名
    private static final String THREAD_NAME = "cameraThread";
    // 默认后置摄像头
    private int mCameraMode = CAMERA_REAR_MODE;
    // 默认是后置摄像头
    private String mCameraID = CameraConfig.CAMERA_REAR_ID;
    // 主线程handler
    private Handler mMainHandler;
    // 子线程handler
    private Handler mChildHandler;
    // presenter
    private ICameraFeaturePresenter mCameraFeaturePresenter;
    // 相机设备
    private CameraDevice mCameraDevice;
    // TextureView的渲染
    private Surface mSurface;
    // 图片读取器
    private ImageReader mImageReader;
    // 相机管理
    private CameraManager mCameraManager;
    // Texture弱引用
    private WeakReference<TextureView> mTextureViewWeakReference;
    // activity弱引用
    private WeakReference<Activity> mActivityWeakReference;
    // session
    private CameraCaptureSession mCameraCaptureSession;
    // 预览请求
    private CaptureRequest mPreviewRequest;
    // 相机状态回调
    private CameraStateCallback mCameraStateCallback;
    // 目标尺寸
    private Size mTargetSize;
    // 默认闪光模式为关闭
    private int mFlashMode = CameraConfig.FLASH_OFF;
    // 默认照片比例为16比9
    private float mCameraRatio = 16f / 9;

    public CameraFeatureManager(ICameraFeaturePresenter iCameraFeaturePresenter) {
        this.mMainHandler = new Handler(Looper.getMainLooper());
        mCameraFeaturePresenter = iCameraFeaturePresenter;
        // 运行一条异步handler线程
        HandlerThread mChildThread = new HandlerThread(THREAD_NAME);
        mChildThread.start();
        mChildHandler = new Handler(mChildThread.getLooper());
    }

    /**
     * 通过对比得到与宽高比最接近的尺寸
     * 这边思路是找到比例最接近的相机支持的比例，如果有比例相同的，则比较与textureView的宽之差，相差最小的为最合适的
     *
     * @param cameraRatio 相机宽高比
     * @param preSizeList 需要对比的预览尺寸列表
     * @return 得到与原宽高比例最接近的尺寸
     */
    private static Size getCloselyPreSize(float cameraRatio, int textureViewWidth, Size[] preSizeList) {
        float curRatio, deltaRatio;
        int curWidth, deltaWidth;
        float deltaRatioMin = Float.MAX_VALUE;
        int deltaWidthMin = Integer.MAX_VALUE;
        Size retSize = null;
        for (Size size : preSizeList) {
            curRatio = ((float) size.getWidth()) / size.getHeight();
            curWidth = size.getHeight();
            deltaRatio = Math.abs(cameraRatio - curRatio);
            deltaWidth = Math.abs(textureViewWidth - curWidth);
            if (deltaRatio < deltaRatioMin || (deltaRatio == deltaRatioMin && deltaWidth < deltaWidthMin)) {
                deltaRatioMin = deltaRatio;
                deltaWidthMin = deltaWidth;
                retSize = size;
            }
        }
        return retSize;
    }

    /**
     * 改变闪关灯模式，session要重新关掉预览并重启
     *
     * @param flashMode 模式
     */
    @Override
    public void setFlashMode(int flashMode) {
        if (flashMode == CameraConfig.FLASH_ALWAYS || mFlashMode == CameraConfig.FLASH_ALWAYS) {
            mFlashMode = flashMode;
            sessionStopPreview();
            sessionPreview();
        } else {
            mFlashMode = flashMode;
        }
    }

    /**
     * session停止预览
     */
    private void sessionStopPreview() {
        if (mCameraCaptureSession != null) {
            try {
                mCameraCaptureSession.stopRepeating();
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * session开启预览
     */
    private void sessionPreview() {
        if (mCameraCaptureSession == null) return;
        if (mCameraDevice == null) return;
        if (mSurface == null) return;
        try {
            // 预览请求Builder
            CaptureRequest.Builder requestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 设置图片输出目标
            requestBuilder.addTarget(mSurface);
            // 设置自动对焦模式
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            if (mFlashMode == CameraConfig.FLASH_ALWAYS) {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            }
            // 建立请求
            mPreviewRequest = requestBuilder.build();
            // 开始显示相机预览
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequest, null, null);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 设置照片比例，设置完了要关闭预览再重启
     *
     * @param cameraRatio 比例
     */
    @Override
    public void setCameraRatio(float cameraRatio) {
        this.mCameraRatio = cameraRatio;
        stopCameraAndPreview();
        openCameraAndStartPreview();
    }

    /**
     * 改变相机模式（前后置）
     */
    @Override
    public void changeCameraMode() {
        // 前后置转换
        switch (mCameraMode) {
            case CAMERA_FRONT_MODE:
                mCameraID = CameraConfig.CAMERA_REAR_ID;
                mCameraMode = CAMERA_REAR_MODE;
                break;
            case CAMERA_REAR_MODE:
                mCameraID = CameraConfig.CAMERA_FRONT_ID;
                mCameraMode = CAMERA_FRONT_MODE;
                break;
        }
        // 重新打开相机预览
        stopCameraAndPreview();
        openCameraAndStartPreview();
    }

    /**
     * 停止相机和预览
     */
    @Override
    public void stopCameraAndPreview() {
        // 关掉相机设备
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
        // 关掉Reader
        if (mImageReader != null) {
            mImageReader.close();
        }
        // 关掉session
        if (mCameraCaptureSession != null) {
            mCameraCaptureSession.close();
        }
        // 释放surface
        if (mSurface != null) {
            mSurface.release();
        }
    }

    /**
     * 重启相机并预览
     */
    @Override
    public void openCameraAndStartPreview() {
        if (mTextureViewWeakReference == null || mTextureViewWeakReference.get() == null) return;
        if (mActivityWeakReference == null || mActivityWeakReference.get() == null) return;
        openCameraAndStartPreview(mTextureViewWeakReference.get(), mActivityWeakReference.get());
    }

    /**
     * 开始预览
     *
     * @param textureView textureView
     * @param activity    activity
     */
    @Override
    public void openCameraAndStartPreview(TextureView textureView, Activity activity) {
        if (textureView == null) return;
        if (activity == null) return;
        mTextureViewWeakReference = new WeakReference<>(textureView);
        mActivityWeakReference = new WeakReference<>(activity);
        // 获取摄像头管理
        mCameraManager = (CameraManager) CameraApplication.getApplication().getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(CameraApplication.getApplication(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED
                    && ActivityCompat.checkSelfPermission(CameraApplication.getApplication(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }
            if (mCameraManager == null) return;
            if (mCameraStateCallback == null) {
                mCameraStateCallback = new CameraStateCallback();
            }
            // 打开摄像头
            mCameraManager.openCamera(mCameraID, mCameraStateCallback, mMainHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拍照
     */
    @Override
    public void takePhoto() {
        if (mCameraDevice == null) return;
        if (mImageReader == null) return;
        if (mCameraCaptureSession == null) return;
        if (mPreviewRequest == null) return;
        try {
            // 创建拍照需要的CaptureRequest.Builder
            final CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
            // 将mImageReader的surface作为CaptureRequest.Builder的目标
            captureRequestBuilder.addTarget(mImageReader.getSurface());
            // 自动对焦
            captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 根据mFlashMode决定开不开闪光灯
            switch (mFlashMode) {
                case CameraConfig.FLASH_ALWAYS:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_ALWAYS_FLASH);
                    break;
                case CameraConfig.FLASH_OFF:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_OFF);
                    break;
                case CameraConfig.FLASH_ON:
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
                    break;
            }
            // 拍照请求
            CaptureRequest captureRequest = captureRequestBuilder.build();
            // 停止预览
            mCameraCaptureSession.stopRepeating();
            // 开始拍照
            mCameraCaptureSession.capture(captureRequest, new CameraCaptureSession.CaptureCallback() {
                @Override
                public void onCaptureCompleted(@NonNull CameraCaptureSession session, @NonNull CaptureRequest request, @NonNull TotalCaptureResult result) {
                    super.onCaptureCompleted(session, request, result);
                    try {
                        // 拍照完了重新开启预览
                        mCameraCaptureSession.setRepeatingRequest(mPreviewRequest, null, null);
                    } catch (CameraAccessException e) {
                        e.printStackTrace();
                    }
                }
            }, mChildHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 清除弱引用
     */
    @Override
    public void onDestroy() {
        if (mActivityWeakReference != null) {
            mActivityWeakReference.clear();
            mActivityWeakReference = null;
        }
        if (mTextureViewWeakReference != null) {
            mTextureViewWeakReference.clear();
            mTextureViewWeakReference = null;
        }
    }

    /**
     * 拍照完成回调
     *
     * @param reader imageReader
     */
    @Override
    public void onImageAvailable(final ImageReader reader) {
        mChildHandler.post(new Runnable() {
            @Override
            public void run() {
                // 拿到拍照照片数据
                Image image = reader.acquireNextImage();
                ByteBuffer buffer = image.getPlanes()[0].getBuffer();
                byte[] bytes = new byte[buffer.remaining()];
                buffer.get(bytes);
                Bitmap bitmap = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (bitmap == null) return;
                // 旋转90度矩阵，因为拍出来的照片都是以横向拍摄为准，会导致图片看起来像被旋转了-90度
                Matrix matrix = new Matrix();
                switch (mCameraMode) {
                    case CAMERA_FRONT_MODE:
                        // 逆时针旋转
                        matrix.postRotate(-90);
                        // 水平反转
                        matrix.postScale(-1, 1);
                        break;
                    case CAMERA_REAR_MODE:
                        // 顺时针旋转
                        matrix.postRotate(90);
                        break;
                }
                // 得到旋转后的图片
                Bitmap rotatedBitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
                // 文件名
                final String fileName = System.currentTimeMillis() + CameraConfig.JPEG;
                // 保存图片
                StoreUtil.saveBitmap(rotatedBitmap, fileName);
                // 转移到主线程进行回调
                mMainHandler.post(new Runnable() {
                    @Override
                    public void run() {
                        mCameraFeaturePresenter.onPhotoToken(fileName);
                    }
                });
                // 资源清理
                bitmap.recycle();
                rotatedBitmap.recycle();
                image.close();
            }
        });
    }

    /**
     * 新建一个Reader
     *
     * @return ImageReader
     */
    private ImageReader newReader() {
        if (mTargetSize == null) return null;
        ImageReader imageReader = ImageReader.newInstance(mTargetSize.getWidth(), mTargetSize.getHeight(), ImageFormat.JPEG, 1);
        imageReader.setOnImageAvailableListener(this, mMainHandler);
        return imageReader;
    }

    /**
     * 相机打开完毕回调类
     */
    private class CameraStateCallback extends CameraDevice.StateCallback {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            try {
                TextureView textureView = mTextureViewWeakReference.get();
                if (textureView == null) return;
                mCameraDevice = camera;
                // 控制摄像头属性的对象
                CameraCharacteristics characteristics = mCameraManager.getCameraCharacteristics(mCameraID);
                // 获取摄像头支持的配置属性
                StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
                // 获取摄像头支持的最大尺寸
                assert map != null;
                // 摄像头支持的输出尺寸
                Size[] sizes = map.getOutputSizes(SurfaceTexture.class);
                // 找到最接近于textureView的宽高比例的输出比例
                mTargetSize = getCloselyPreSize(mCameraRatio, textureView.getWidth(), sizes);
                // scale为短的除以长的，这边targetSize的width指的是长的一边，比如1920x1080，width指的是1920，而长的一边在我们的应用中是height，所以现在把targetSize的width当成height，height当成width
                float targetScale = 1F * mTargetSize.getHeight() / mTargetSize.getWidth();
                // 高不变
                int targetHeight = (int) (textureView.getWidth() / targetScale);
                // 宽为高乘以目标比例
                int targetWidth = textureView.getWidth();
                // 重新调整textureView宽高
                RelativeLayout.LayoutParams layoutParams = (RelativeLayout.LayoutParams) textureView.getLayoutParams();
                layoutParams.width = targetWidth;
                layoutParams.height = targetHeight;
                textureView.setLayoutParams(layoutParams);
                // 设置缓冲区大小
                SurfaceTexture texture = textureView.getSurfaceTexture();
                texture.setDefaultBufferSize(mTargetSize.getWidth(), mTargetSize.getHeight());
                // TextureView的Surface
                mSurface = new Surface(texture);
                if (mImageReader != null) {
                    mImageReader.close();
                }
                mImageReader = newReader();
                if (mImageReader == null) return;
                // 创建预览Session
                camera.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), new PreviewSessionStateCallback(), mMainHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            camera.close();
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {

        }
    }

    /**
     * 预览Session准备完毕回调类
     */
    private class PreviewSessionStateCallback extends CameraCaptureSession.StateCallback {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            mCameraCaptureSession = cameraCaptureSession;
            // 开启预览
            sessionPreview();
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    }

}
