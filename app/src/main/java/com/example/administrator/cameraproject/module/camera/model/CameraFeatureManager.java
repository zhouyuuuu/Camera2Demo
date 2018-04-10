package com.example.administrator.cameraproject.module.camera.model;

import android.Manifest;
import android.app.Activity;
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
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.util.Size;
import android.view.Surface;
import android.view.TextureView;

import com.example.administrator.cameraproject.module.camera.application.CameraApplication;
import com.example.administrator.cameraproject.module.camera.config.CameraConfig;
import com.example.administrator.cameraproject.module.camera.presenter.ICameraFeaturePresenter;
import com.example.administrator.cameraproject.module.camera.util.StoreUtil;

import java.lang.ref.WeakReference;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * Edited by Administrator on 2018/4/10.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraFeatureManager implements ImageReader.OnImageAvailableListener, ICameraFeatureManager {

    // 子线程名
    private static final String THREAD_NAME = "cameraThread";
    // 默认是后置摄像头
    private static String mCameraID = CameraConfig.CAMERA_REAR_ID;
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

    public CameraFeatureManager(ICameraFeaturePresenter iCameraFeaturePresenter) {
        this.mMainHandler = new Handler(Looper.getMainLooper());
        mCameraFeaturePresenter = iCameraFeaturePresenter;
        // 运行一条异步handler线程
        HandlerThread mChildThread = new HandlerThread(THREAD_NAME);
        mChildThread.start();
        mChildHandler = new Handler(mChildThread.getLooper());
    }

    /**
     * 通过对比得到与宽高比最接近的尺寸（如果有相同尺寸，优先选择）
     *
     * @param textureViewWidth  需要被进行对比的原宽
     * @param textureViewHeight 需要被进行对比的原高
     * @param preSizeList       需要对比的预览尺寸列表
     * @return 得到与原宽高比例最接近的尺寸
     */
    private static Size getCloselyPreSize(int textureViewWidth, int textureViewHeight,
                                          Size[] preSizeList) {
        for (Size size : preSizeList) {
            // 由于是竖屏，宽和高是反过来的
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

    /**
     * 开始预览
     *
     * @param textureView textureView
     * @param activity    activity
     */
    @Override
    public void startPreview(TextureView textureView, Activity activity) {
        mTextureViewWeakReference = new WeakReference<>(textureView);
        mActivityWeakReference = new WeakReference<>(activity);
        // 获取摄像头管理
        mCameraManager = (CameraManager) CameraApplication.getApplication().getSystemService(Context.CAMERA_SERVICE);
        try {
            if (ActivityCompat.checkSelfPermission(CameraApplication.getApplication(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(CameraApplication.getApplication(), Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(activity, new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return;
            }
            // 打开摄像头
            if (mCameraManager != null) {
                mCameraManager.openCamera(mCameraID, new CameraStateCallback(), mMainHandler);
            }
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 拍照
     */
    @Override
    public void takePhoto() {
        try {
            // 关闭之前使用过的reader
            if (mImageReader != null) {
                mImageReader.close();
            }
            // 得到新的reader，一个reader只可以用一次
            mImageReader = newReader();
            if (mImageReader == null) return;
            // 拍照请求，异步回调
            mCameraDevice.createCaptureSession(Arrays.asList(mSurface, mImageReader.getSurface()), new CaptureSessionStateCallback(), mChildHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    /**
     * 新建一个Reader
     *
     * @return ImageReader
     */
    private ImageReader newReader() {
        TextureView textureView = mTextureViewWeakReference.get();
        if (textureView == null) return null;
        ImageReader imageReader = ImageReader.newInstance(textureView.getWidth(), textureView.getHeight(), ImageFormat.JPEG, 1);
        imageReader.setOnImageAvailableListener(this, mMainHandler);
        return imageReader;
    }

    /**
     * 停止预览
     */
    @Override
    public void stopPreview() {
        // 关掉相机设备
        if (mCameraDevice != null) {
            mCameraDevice.close();
        }
        // 关掉Reader
        if (mImageReader != null) {
            mImageReader.close();
        }
    }

    /**
     * 重启预览
     */
    @Override
    public void restartPreview() {
        if (mTextureViewWeakReference == null || mTextureViewWeakReference.get() == null) return;
        if (mActivityWeakReference == null || mActivityWeakReference.get() == null) return;
        startPreview(mTextureViewWeakReference.get(), mActivityWeakReference.get());
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
                matrix.postRotate(90);
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
                reader.close();
            }
        });
    }

    private class PreviewSessionStateCallback extends CameraCaptureSession.StateCallback {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession cameraCaptureSession) {
            // 当摄像头已经准备好时，开始显示预览
            try {
                // 预览请求Builder
                CaptureRequest.Builder requestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
                // 设置图片输出目标
                requestBuilder.addTarget(mSurface);
                // 设置自动对焦模式
                requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 设置自动曝光模式
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                // 建立请求
                CaptureRequest previewRequest = requestBuilder.build();
                // 开始显示相机预览
                cameraCaptureSession.setRepeatingRequest(previewRequest, null, null);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    }

    private class CaptureSessionStateCallback extends CameraCaptureSession.StateCallback {

        @Override
        public void onConfigured(@NonNull CameraCaptureSession session) {
            // 当摄像头已经准备好时，开始拍照
            try {
                // 创建拍照需要的CaptureRequest.Builder
                final CaptureRequest.Builder captureRequestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_STILL_CAPTURE);
                // 将mImageReader的surface作为CaptureRequest.Builder的目标
                captureRequestBuilder.addTarget(mImageReader.getSurface());
                if (null == mCameraDevice) return;
                // 自动对焦
                captureRequestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
                // 自动闪光灯
                captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON_AUTO_FLASH);
                // 拍照请求
                CaptureRequest captureRequest = captureRequestBuilder.build();
                // 开始拍照
                session.capture(captureRequest, null, mChildHandler);
            } catch (CameraAccessException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    }

    private class CameraStateCallback extends CameraDevice.StateCallback {

        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            // 摄像头打开了
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
                Size targetSize = getCloselyPreSize(textureView.getWidth(), textureView.getHeight(), sizes);
                // 原来textureView的Rect
                RectF preRect = new RectF(0, 0, textureView.getWidth(), textureView.getHeight());
                // scale为短的除以长的，这边targetSize的width指的是长的一边，比如1920x1080，width指的是1920，而长的一边在我们的应用中是height，所以现在把targetSize的width当成height，height当成width
                float targetScale = 1F * targetSize.getHeight() / targetSize.getWidth();
                // 高不变
                int targetHeight = textureView.getHeight();
                // 宽为高乘以目标比例
                int targetWidth = (int) (textureView.getHeight() * targetScale);
                // 目标Rect
                RectF targetRect = new RectF(0, 0, targetWidth, targetHeight);
                // 变换矩阵
                Matrix matrix = new Matrix();
                matrix.setRectToRect(preRect, targetRect, Matrix.ScaleToFit.FILL);
                // 宽高比例变换
                textureView.setTransform(matrix);
                // 设置缓冲区大小
                SurfaceTexture texture = textureView.getSurfaceTexture();
                texture.setDefaultBufferSize(targetSize.getWidth(), targetSize.getHeight());
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
}
