package com.godot.rtmppushstream.live.widgets;

import android.content.Context;
import android.os.Build;
import android.util.AttributeSet;
import android.view.SurfaceView;

import androidx.annotation.RequiresApi;

/**
 * @Desc 说明
 * @Author Godot
 * @Date 2020-01-23
 * @Version 1.0
 * @Mail 809373383@qq.com
 */
public class AutoFitSurfaceView extends SurfaceView {

    private float aspectRatio = 0f;

    public AutoFitSurfaceView(Context context) {
        super(context);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFitSurfaceView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    public AutoFitSurfaceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
    }

    public void setAspectRatio(int width, int height) {
        if( width > 0 && height > 0 ) {
            aspectRatio = (float) width / (float) height;
            requestLayout();
        } else {
            throw new IllegalArgumentException("width，height must greater than zero");
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        if( aspectRatio > 0 ) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            int height = MeasureSpec.getSize(heightMeasureSpec);
            if (width < height * aspectRatio) {
                setMeasuredDimension(width, (int)(width * 1 / aspectRatio));
            } else {
                setMeasuredDimension((int)(height * aspectRatio), height);
            }
        }
    }
}
