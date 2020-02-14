package com.godot.rtmppushstream.live.channel.utils;

import android.app.Activity;
import android.os.Build;
import android.view.SurfaceHolder;

/**
 * @Desc 说明
 * @Author Godot
 * @Date 2020-02-04
 * @Version 1.0
 * @Mail 809373383@qq.com
 */
public abstract class BaseCameraHelper implements SurfaceHolder.Callback {
    protected SurfaceHolder surfaceHolder;
    protected Activity activity;
    protected int width;
    protected int height;
    protected OnSizeChangeListener onSizeChangeListener;
    protected OnPreviewFrameCallback onPreviewFrameCallback;

    public static BaseCameraHelper getCameraHelper(Activity activity, int width, int height) {
        if( true ) {
            return new CameraHelper(activity, width, height);
        }
        if(Build.VERSION_CODES.LOLLIPOP <= Build.VERSION.SDK_INT ) {
            return new Camera2Helper(activity, width, height);
        } else {
            return new CameraHelper(activity, width, height);
        }
    }
    public BaseCameraHelper(Activity activity, int width, int height) {
        this.activity = activity;
        this.width = width;
        this.height = height;
    }

    public final void setPreviewDisplay(SurfaceHolder holder) {
        this.surfaceHolder = holder;
        surfaceHolder.addCallback(this);
    }

    @Override
    public void surfaceCreated(SurfaceHolder holder) {

    }

    @Override
    public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {
        stopPreview();
        startPreview();
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder holder) {
        stopPreview();
    }

    public interface OnPreviewFrameCallback {
        void onPreviewFrame(byte[] data);
    }
    public void setPreviewCallback(OnPreviewFrameCallback onPreviewFrameCallback) {
        this.onPreviewFrameCallback = onPreviewFrameCallback;
    }
    public interface OnSizeChangeListener {
        void onChanged(int width, int height);
    }
    public void setOnSizeChangeListener(OnSizeChangeListener onSizeChangeListener) {
        this.onSizeChangeListener = onSizeChangeListener;
    }


    public abstract void startPreview();
    public abstract void stopPreview();
    public abstract void switchCamera();


}
