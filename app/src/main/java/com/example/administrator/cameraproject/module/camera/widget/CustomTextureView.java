package com.example.administrator.cameraproject.module.camera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.TextureView;

/**
 * 重写了触摸事件，双指操作触发onScaleListener，单击触发onFocusListener
 * Edited by Administrator on 2018/4/12.
 */

public class CustomTextureView extends TextureView {

    // 双点触控第一个点坐标x
    private float mPreX;
    // 双点触控第一个点坐标y
    private float mPreY;
    // 上一次双点触控的两点间距
    private float mPreDistance;
    // 双点模式标志
    private boolean mIsDoublePoint = false;
    // 点击监听器
    private OnFocusListener mOnFocusListener = null;
    // 缩放监听器
    private OnScaleListener mOnScaleListener = null;

    public CustomTextureView(Context context) {
        super(context);
    }

    public CustomTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 为了不报黄重写了该方法
     */
    @Override
    public boolean performClick() {
        return super.performClick();
    }

    /**
     * 触摸事件
     *
     * @param event 事件
     * @return 是否处理
     */
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction() & MotionEvent.ACTION_MASK) {
            case MotionEvent.ACTION_DOWN:
                // 记录DOWN的XY
                mPreX = event.getX();
                mPreY = event.getY();
                break;
            case MotionEvent.ACTION_UP:
                // 如果和DOWN的XY相同，则认为是Click，触发对焦监听
                if (mPreX == event.getX() && mPreY == event.getY() && mOnFocusListener != null) {
                    mOnFocusListener.onFocus((int) event.getX(), (int) event.getY());
                }
                // 触发View的Click监听，在这里调用只是为了不报黄
                performClick();
                break;
            case MotionEvent.ACTION_POINTER_DOWN:
                // 双击模式为true
                mIsDoublePoint = true;
                // 重新获取两个点的XY
                mPreX = event.getX(0);
                mPreY = event.getY(0);
                float mAfterX = event.getX(1);
                float mAfterY = event.getY(1);
                // 计算两个点的距离
                mPreDistance = (mPreX - mAfterX) * (mPreX - mAfterX) + (mPreY - mAfterY) * (mPreY - mAfterY);
                mPreDistance = (float) Math.sqrt(mPreDistance);
                break;
            case MotionEvent.ACTION_POINTER_UP:
                // 双击模式为false
                mIsDoublePoint = false;
                break;
            case MotionEvent.ACTION_MOVE:
                // 如果不是双点模式就退出
                if (!mIsDoublePoint) {
                    break;
                }
                if (event.getPointerCount() < 2) {
                    break;
                }
                // 计算两点间距
                float disX = Math.abs(event.getX(0) - event.getX(1));
                float disY = Math.abs(event.getY(0) - event.getY(1));
                float curDistance = (float) Math.sqrt(disX * disX + disY * disY);
                // 计算本次两点距离与上一次两点距离之差，该值为镜头放大缩小的值
                int distance = (int) (curDistance - mPreDistance);
                // 变化距离不为0则触发缩放监听
                if (distance != 0 && mOnScaleListener != null) {
                    mOnScaleListener.onScale(distance);
                }
                // 当前距离成为上一次距离
                mPreDistance = curDistance;
                break;
        }
        return true;
    }

    /**
     * 设置对焦监听器
     */
    public void setOnFocusListener(OnFocusListener onFocusListener) {
        mOnFocusListener = onFocusListener;
    }

    /**
     * 设置缩放监听器
     */
    public void setOnScaleListener(OnScaleListener onScaleListener) {
        mOnScaleListener = onScaleListener;
    }

    /**
     * 对焦监听器
     */
    public interface OnFocusListener {
        void onFocus(int x, int y);
    }

    /**
     * 缩放监听器
     */
    public interface OnScaleListener {
        void onScale(int distance);
    }
}
