package com.example.administrator.cameraproject.module.camera.widget;

import android.content.Context;
import android.util.AttributeSet;
import android.view.TextureView;

/**
 * 只是为了重写performClick，否则在setOnTouchListener时会报黄，虽然报黄不会出错
 * Edited by Administrator on 2018/4/12.
 */

public class CustomTextureView extends TextureView {

    public CustomTextureView(Context context) {
        super(context);
    }

    public CustomTextureView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public CustomTextureView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    public boolean performClick() {
        return super.performClick();
    }
}
