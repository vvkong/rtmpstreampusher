package com.godot.rtmppushstream.live.channel;

import android.app.Activity;
import android.view.SurfaceHolder;

import com.godot.rtmppushstream.live.LivePusher;
import com.godot.rtmppushstream.live.channel.utils.BaseCameraHelper;
import com.godot.rtmppushstream.live.channel.utils.CameraHelper;

/**
 * @Desc 说明
 * @Author Godot
 * @Date 2020-01-22
 * @Version 1.0
 * @Mail 809373383@qq.com
 */
public class VideoChannel implements CameraHelper.OnSizeChangeListener,BaseCameraHelper.OnPreviewFrameCallback {

    private int fps;
    private int bitrate;
    private BaseCameraHelper cameraHelper;
    private LivePusher livePusher;
    private boolean isLiving;
    private BaseCameraHelper.OnSizeChangeListener onSizeChangeListener;
    public VideoChannel(LivePusher livePusher, Activity activity, int width, int height, int bitrate, int fps) {
        this.livePusher = livePusher;
        this.bitrate = bitrate;
        this.fps = fps;
        cameraHelper = BaseCameraHelper.getCameraHelper(activity, width, height);
        cameraHelper.setPreviewCallback(this);
        cameraHelper.setOnSizeChangeListener(this);
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        cameraHelper.setPreviewDisplay(holder);
    }

    public void switchCamera() {
        cameraHelper.switchCamera();
    }

    public void stopLive() {
        isLiving = false;
    }

    public void startLive() {
        isLiving = true;
    }

    @Override
    public void onPreviewFrame(byte[] data) {
        //Log.d("black-cat", "onPreviewFrame");
        if( isLiving ) {
            livePusher.nativePushVideo(data);
        }
    }

    @Override
    public void onChanged(int width, int height) {
        if( onSizeChangeListener != null ) {
            onSizeChangeListener.onChanged(width, height);
        }
        livePusher.nativeSetVideoEncInfo(width, height, fps, bitrate);
    }

    public void setOnSizeChangeListener(BaseCameraHelper.OnSizeChangeListener onSizeChangeListener) {
        this.onSizeChangeListener = onSizeChangeListener;
    }

}
