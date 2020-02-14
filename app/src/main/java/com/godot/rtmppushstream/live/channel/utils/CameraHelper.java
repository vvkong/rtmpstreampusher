package com.godot.rtmppushstream.live.channel.utils;

import android.app.Activity;
import android.graphics.ImageFormat;
import android.hardware.Camera;
import android.util.Log;
import android.view.Surface;

import java.io.IOException;
import java.util.List;

/**
 * @Desc 说明
 * @Author Godot
 * @Date 2020-01-22
 * @Version 1.0
 * @Mail 809373383@qq.com
 */
public class CameraHelper extends BaseCameraHelper implements Camera.PreviewCallback {
    private int cameraId;
    private Camera camera;
    private byte[] buffer;

    public CameraHelper(Activity activity, int width, int height) {
        super(activity, width, height);
        this.cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
    }

    @Override
    public void switchCamera() {
        if (cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            cameraId = Camera.CameraInfo.CAMERA_FACING_BACK;
        } else {
            cameraId = Camera.CameraInfo.CAMERA_FACING_FRONT;
        }
        stopPreview();
        startPreview();
    }

    @Override
    public void startPreview() {
        try {
            camera = Camera.open(cameraId);
            Camera.Parameters parameters = camera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            setPreviewSize(parameters);
            setPreviewDisplayOrientation(parameters);
            camera.setParameters(parameters);
            // YUV420
            buffer = new byte[width * height * 3 / 2];
            rData = new byte[width*height*3/2];
            camera.addCallbackBuffer(buffer);
            camera.setPreviewCallbackWithBuffer(this);
            camera.setPreviewDisplay(surfaceHolder);
            if (onSizeChangeListener != null) {
                // 需要旋转
                onSizeChangeListener.onChanged(height, width);
            }
            camera.startPreview();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void setPreviewDisplayOrientation(Camera.Parameters parameters) {
        Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }
        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            // compensate the mirror
            result = (360 - result) % 360;
        } else {
            // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        // 看此函数注释即可知道如何写
        camera.setDisplayOrientation(result);
    }

    private void setPreviewSize(Camera.Parameters parameters) {
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        if( supportedPreviewSizes != null && supportedPreviewSizes.size() > 0 ) {
            Camera.Size size = supportedPreviewSizes.get(0);
            int minDiff = Math.abs(size.width*size.height - width*height);
            supportedPreviewSizes.remove(0);
            for( Camera.Size it : supportedPreviewSizes ) {
                int diff =  Math.abs(it.width*it.height - width*height);
                if( diff < minDiff ) {
                    minDiff = diff;
                    size = it;
                }
            }
            width = size.width;
            height = size.height;
            parameters.setPreviewSize(width, height);

            Log.i("black-cat", "w: " + width + ", h: " + height);
        }
    }

    @Override
    public void stopPreview() {
        if (camera != null) {
            camera.setPreviewCallback(null);
            camera.stopPreview();
            camera.release();
            camera = null;
        }
    }


    @Override
    public void onPreviewFrame(byte[] data, Camera camera) {
        //Log.d("black-cat", "onPreviewFrame");
        if (onPreviewFrameCallback != null) {
            rotate90(data);
            onPreviewFrameCallback.onPreviewFrame(rData);
        }
        camera.addCallbackBuffer(buffer);
    }

    /**
     1 2 3 4 5
     6 7 8 9 10
     // 左转90
     5 10
     4 9
     3 8
     2 7
     1 6
     // 右转90
     6 1
     7 2
     8 3
     9 4
     10 5
     */
    byte[] rData = null;
    private void rotate90(byte[] data) {
        int ySize = width * height;
        int vuHeight = height / 2;

        int index = 0;
        // 一行一行遍历老的数据放到新图数组
        // 依次放即可
        if( cameraId == Camera.CameraInfo.CAMERA_FACING_FRONT ) {
            // 左转90
            for (int i = 0; i < width; i++) {
                int nPos = width - 1;
                for (int j = 0; j < height; j++) {
                    rData[index++] = data[nPos - i];
                    nPos += width;
                }
            }
            for (int i = 0; i < width; i += 2) {
                int nPos = ySize + width - 1;
                for (int j = 0; j < vuHeight; j++) {
                    // v
                    rData[index++] = data[nPos - i - 1];
                    // u
                    rData[index++] = data[nPos - i];
                    nPos += width;
                }
            }
        } else {
            // 右转90
            for (int i = 0; i < width; i++) {
                int nPos = (height - 1)*width;
                for (int j = 0; j < height; j++) {
                    rData[index++] = data[nPos + i];
                    nPos -= width;
                }
            }
            for (int i = 0; i < width; i+=2) {
                int nPos = ySize + (vuHeight - 1)*width;
                for (int j = 0; j < vuHeight; j++) {
                    rData[index++] = data[nPos + i];
                    rData[index++] = data[nPos + i + 1];
                    nPos -= width;
                }
            }

        }

    }
}
