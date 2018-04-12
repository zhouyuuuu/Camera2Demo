package com.example.administrator.cameraproject.module.camera.model;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.ImageFormat;
import android.graphics.Matrix;
import android.graphics.Rect;
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
 * 相机功能管理者，Model，管理闪光灯、比例、镜头、预览、拍照、对焦、缩放
 * Edited by Administrator on 2018/4/10.
 */

@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
public class CameraFeatureManager implements ImageReader.OnImageAvailableListener, ICameraFeatureManager {

    // 子线程名
    private static final String THREAD_NAME = "cameraThread";
    // 默认后置摄像头
    private int mCameraMode = CameraConfig.CAMERA_REAR_MODE;
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
    private float mCameraRatio = CameraConfig.CAMERA_PHOTO_RATIO_16_9;
    // 焦点占width比
    private float mFocusWidthRatio = CameraConfig.FOCUS_WIDTH_RATIO_DEFAULT;
    // 焦点占height比
    private float mFocusHeightRatio = CameraConfig.FOCUS_HEIGHT_RATIO_DEFAULT;
    // 获取的图像对应的rect
    private Rect mRectOriginal;
    // 拍照最大放大left
    private int mRectMaxLeft;
    // 拍照最大放大right
    private int mRectMaxRight;
    // 拍照最大放大top
    private int mRectMaxTop;
    // 拍照最大放大bottom
    private int mRectMaxBottom;
    // 拍照最小缩小left
    private int mRectMinLeft;
    // 拍照最小缩小right
    private int mRectMinRight;
    // 拍照最小缩小top
    private int mRectMinTop;
    // 拍照最小缩小bottom
    private int mRectMinBottom;
    // 缩放后的rect
    private Rect mRectScale;
    // session是否准备好
    private boolean mSessionConfigured = false;

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
     * 设置缩放，缩放不小于原来Rect的1倍，不大于原来Rect的4倍
     *
     * @param distance 缩放量
     */
    public void setScale(int distance) {
        if (distance == 0) return;
        Rect curRect;
        if (mRectScale != null) {
            curRect = mRectScale;
        } else {
            curRect = mRectOriginal;
        }
        int left = curRect.left + distance;
        int right = curRect.right - distance;
        int top = curRect.top + distance;
        int bottom = curRect.bottom - distance;
        // 如果大于0是放大，放大不能超过各个限定值如left不能大于最大的left，缩小同理
        if (left <= mRectMinLeft || right >= mRectMinRight || top <= mRectMinTop || bottom >= mRectMinBottom
                || left > mRectMaxLeft || right < mRectMaxRight || top > mRectMaxTop || bottom < mRectMaxBottom) {
            return;
        }
        // 建立新的缩放Rect
        mRectScale = new Rect(left, top, right, bottom);
        // 重新打开预览
        try {
            sessionStopPreview();
            sessionPreview();
        } catch (CameraAccessException e) {
            // 出事了就重新打开相机
            openCameraAndStartPreview();
            e.printStackTrace();
        }
    }

    /**
     * 设置焦点在图像中的x比和y比
     *
     * @param x x
     * @param y x
     */
    @Override
    public void setFocusPoint(int x, int y) {
        TextureView textureView = mTextureViewWeakReference.get();
        // 焦点在屏幕中的x相对位置百分比
        mFocusWidthRatio = 1f * x / textureView.getWidth();
        // 焦点在屏幕中的y相对位置百分比
        mFocusHeightRatio = 1f * y / textureView.getHeight();
        // 重新打开预览
        try {
//            sessionStopPreview();
            sessionPreview();
        } catch (CameraAccessException e) {
            openCameraAndStartPreview();
            e.printStackTrace();
        }
    }

    /**
     * 改变闪关灯模式，session要重新关掉预览并重启
     *
     * @param flashMode 模式
     */
    @Override
    public void setFlashMode(int flashMode) {
        // 设置闪光模式，如果从常亮模式切换到别的模式或者别的模式切换到常亮，要重新打开预览
        if (flashMode == CameraConfig.FLASH_ALWAYS || mFlashMode == CameraConfig.FLASH_ALWAYS) {
            mFlashMode = flashMode;
            // 重新打开预览
            try {
                sessionStopPreview();
                sessionPreview();
            } catch (CameraAccessException e) {
                openCameraAndStartPreview();
                e.printStackTrace();
            }
        } else {
            mFlashMode = flashMode;
        }
    }

    /**
     * session停止预览
     */
    private void sessionStopPreview() throws android.hardware.camera2.CameraAccessException {
        if (!mSessionConfigured) return;
        if (mCameraCaptureSession != null) {
            try {
                // 关闭预览
                mCameraCaptureSession.stopRepeating();
            } catch (CameraAccessException e) {
                openCameraAndStartPreview();
                e.printStackTrace();
            }
        }
    }

    /**
     * session开启预览
     */
    private void sessionPreview() throws android.hardware.camera2.CameraAccessException {
        if (!mSessionConfigured) openCameraAndStartPreview();
        try {
            // 预览请求Builder
            CaptureRequest.Builder requestBuilder = mCameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            // 设置图片输出目标
            requestBuilder.addTarget(mSurface);
            // 设置自动对焦模式
            requestBuilder.set(CaptureRequest.CONTROL_AF_MODE, CaptureRequest.CONTROL_AF_MODE_CONTINUOUS_PICTURE);
            // 对焦区域
            android.hardware.camera2.params.MeteringRectangle[] meteringRectangles = new android.hardware.camera2.params.MeteringRectangle[1];
            // 焦点坐标
            int x, y;
            // 是否目前有缩放
            if (mRectScale == null) {
                // 焦点区域左边，rect是横屏的，rect的right对应了竖屏的activity中view的height，因此是right乘以高度比
                x = (int) (mRectOriginal.right * mFocusHeightRatio);
                // 防止-5后小于0
                if (x < 0) x = 0;
                // 焦点区域上边，这边（1-宽度比）是因为竖屏模式下，view的坐标原点在屏幕左上角，rect的坐标点在屏幕右上角
                y = (int) (mRectOriginal.bottom * (1f - mFocusWidthRatio));
                // 防止-5后小于0
                if (y < 0) y = 0;
            } else {
                // 设置缩放
                requestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mRectScale);
                // 通过宽度乘以比例得到焦点在RectScale上的坐标x，然后加上目前看不到的原Rect左边部分距离，就得到焦点在原Rect上的坐标x
                x = (int) ((mRectScale.right - mRectScale.left) * mFocusHeightRatio) + mRectScale.left;
                // 防止-5后小于0
                if (x < 0) x = 0;
                // 焦点区域上边，计算原理同x
                y = (int) ((mRectScale.bottom - mRectScale.top) * (1f - mFocusWidthRatio)) + mRectScale.top;
                // 防止-5后小于0
                if (y < 0) y = 0;
            }
            // 焦点区域宽度
            int width = CameraConfig.FOCUS_AREA_WIDTH;
            // 焦点区域高度
            int height = CameraConfig.FOCUS_AREA_HEIGHT;
            // 新建对焦区域对象
            meteringRectangles[0] = new android.hardware.camera2.params.MeteringRectangle(x, y, width, height, 1);
            // 设置对焦区域
            requestBuilder.set(CaptureRequest.CONTROL_AF_REGIONS, meteringRectangles);
            // 设置测光区域，测光区域与对焦区域要一致
            requestBuilder.set(CaptureRequest.CONTROL_AE_REGIONS, meteringRectangles);
            // 如果闪光模式为ALWAYS,则预览的时候打开闪光灯
            if (mFlashMode == CameraConfig.FLASH_ALWAYS) {
                requestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                requestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
            }
            // 建立请求
            mPreviewRequest = requestBuilder.build();
            // 开始显示相机预览
            mCameraCaptureSession.setRepeatingRequest(mPreviewRequest, null, mMainHandler);
        } catch (CameraAccessException e) {
            openCameraAndStartPreview();
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
        // 设置照片比例
        this.mCameraRatio = cameraRatio;
        // 关掉相机和预览
        stopCameraAndPreview();
        // 启动相机和预览
        openCameraAndStartPreview();
    }

    /**
     * 改变相机模式（前后置）
     */
    @Override
    public void changeCameraMode() {
        // 前后置转换
        switch (mCameraMode) {
            case CameraConfig.CAMERA_FRONT_MODE:
                mCameraID = CameraConfig.CAMERA_REAR_ID;
                mCameraMode = CameraConfig.CAMERA_REAR_MODE;
                break;
            case CameraConfig.CAMERA_REAR_MODE:
                mCameraID = CameraConfig.CAMERA_FRONT_ID;
                mCameraMode = CameraConfig.CAMERA_FRONT_MODE;
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
        mSessionConfigured = false;
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
            openCameraAndStartPreview();
            e.printStackTrace();
        }
    }

    /**
     * 拍照
     */
    @Override
    public void takePhoto() {
        if (!mSessionConfigured) {
            openCameraAndStartPreview();
            return;
        }
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
                    captureRequestBuilder.set(CaptureRequest.CONTROL_AE_MODE, CaptureRequest.CONTROL_AE_MODE_ON);
                    captureRequestBuilder.set(CaptureRequest.FLASH_MODE, CaptureRequest.FLASH_MODE_TORCH);
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
            // 设置缩放
            if (mRectScale != null) {
                captureRequestBuilder.set(CaptureRequest.SCALER_CROP_REGION, mRectScale);
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
                        mCameraCaptureSession.setRepeatingRequest(mPreviewRequest, null, mMainHandler);
                    } catch (CameraAccessException e) {
                        openCameraAndStartPreview();
                        e.printStackTrace();
                    }
                }
            }, mMainHandler);
        } catch (CameraAccessException e) {
            openCameraAndStartPreview();
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
                    case CameraConfig.CAMERA_FRONT_MODE:
                        // 逆时针旋转
                        matrix.postRotate(CameraConfig.CAMERA_FRONT_PHOTO_REVISE_DEGREE);
                        // 水平反转
                        matrix.postScale(-1, 1);
                        break;
                    case CameraConfig.CAMERA_REAR_MODE:
                        // 顺时针旋转
                        matrix.postRotate(CameraConfig.CAMERA_REAR_PHOTO_REVISE_DEGREE);
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
                // 拍摄图像的Rect，设置焦点区域时根据该Rect的长宽来设置，并设置缩放的最大最小值
                mRectOriginal = characteristics.get(CameraCharacteristics.SENSOR_INFO_ACTIVE_ARRAY_SIZE);
                if (mRectOriginal != null) {
                    mRectMaxLeft = (int) (mRectOriginal.right * CameraConfig.CAMERA_SCALE_LEFT_PERCENT);
                    mRectMaxRight = (int) (mRectOriginal.right * CameraConfig.CAMERA_SCALE_RIGHT_PERCENT);
                    mRectMaxTop = (int) (mRectOriginal.bottom * CameraConfig.CAMERA_SCALE_TOP_PERCENT);
                    mRectMaxBottom = (int) (mRectOriginal.bottom * CameraConfig.CAMERA_SCALE_BOTTOM_PERCENT);
                    mRectMinLeft = mRectOriginal.left;
                    mRectMinRight = mRectOriginal.right;
                    mRectMinTop = mRectOriginal.top;
                    mRectMinBottom = mRectOriginal.bottom;
                }
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
                openCameraAndStartPreview();
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
            mSessionConfigured = true;
            // 开启预览
            try {
                sessionPreview();
            } catch (CameraAccessException e) {
                openCameraAndStartPreview();
                e.printStackTrace();
            }
        }

        @Override
        public void onConfigureFailed(@NonNull CameraCaptureSession session) {

        }
    }

}
