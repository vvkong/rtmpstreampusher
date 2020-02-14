package com.godot.rtmppushstream.live;

import android.app.Activity;
import android.view.SurfaceHolder;

import com.godot.rtmppushstream.live.channel.AudioChannel;
import com.godot.rtmppushstream.live.channel.VideoChannel;
import com.godot.rtmppushstream.live.channel.utils.BaseCameraHelper;

/**
 * @Desc 说明
 * @Author Godot
 * @Date 2020-01-22
 * @Version 1.0
 * @Mail 809373383@qq.com
 */
public class LivePusher implements BaseCameraHelper.OnSizeChangeListener {
    static {
        System.loadLibrary("livepush");
    }

    private VideoChannel videoChannel;
    private AudioChannel audioChannel;

    public LivePusher(Activity activity, int width, int height, int bitrate, int fps) {
        nativeInit();
        videoChannel = new VideoChannel(this, activity, width, height, bitrate, fps);
        videoChannel.setOnSizeChangeListener(this);
        audioChannel = new AudioChannel(this);
    }


    public void switchCamera() {
        videoChannel.switchCamera();
    }

    public void startLive(String liveUrl) {
        nativeStart(liveUrl);
        videoChannel.startLive();
        audioChannel.startLive();
    }

    public void stopLive() {
        videoChannel.stopLive();
        audioChannel.stopLive();
        nativeStop();
    }

    public void setPreviewDisplay(SurfaceHolder holder) {
        videoChannel.setPreviewDisplay(holder);
    }

    @Override
    public void onChanged(int width, int height) {
        if( onSizeChangeListener != null ) {
            onSizeChangeListener.onChanged(width, height);
        }
    }


    public interface OnSizeChangeListener {
        void onChanged(int width, int height);
    }
    private OnSizeChangeListener onSizeChangeListener;
    public void setOnSizeChangeListener(OnSizeChangeListener onSizeChangeListener) {
        this.onSizeChangeListener = onSizeChangeListener;
    }

    private native void nativeInit();
    private native void nativeStart(String liveUrl);
    public native void nativePushVideo(byte[] data);
    public native void nativeSetVideoEncInfo(int width, int height, int fps, int bitrate);
    public native void nativeSetAudioEncInfo(int sampleInHz, int channels);
    private native void nativeStop();
    public native void realease();
    public native int getInputSample();
    public native void nativePushAudio(byte[] data, int len);

}
