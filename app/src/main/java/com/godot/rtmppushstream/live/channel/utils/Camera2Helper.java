package com.godot.rtmppushstream.live.channel.utils;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.res.Configuration;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraCaptureSession;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraDevice;
import android.hardware.camera2.CameraManager;
import android.hardware.camera2.CameraMetadata;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.params.StreamConfigurationMap;
import android.media.MediaRecorder;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.util.Log;
import android.util.Size;
import android.view.SurfaceHolder;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

/**
 * @Desc 说明
 * @Author Godot
 * @Date 2020-02-04
 * @Version 1.0
 * @Mail 809373383@qq.com
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP )
public class Camera2Helper extends BaseCameraHelper {
    private static final String TAG = "Camera2Helper";

    private Semaphore cameraOpenCloseLock = new Semaphore(1);
    private int cameraId;
    private Integer mSensorOrientation;
    private CaptureRequest.Builder mPreviewBuilder;

    private CameraManager cameraManager;
    private CameraDevice cameraDevice;
    private Size previewSize;
    private Size videoSize;
    private CameraCaptureSession cameraCaptureSession;

    private CameraDevice.StateCallback deviceStateCallback = new CameraDevice.StateCallback() {
        @Override
        public void onOpened(@NonNull CameraDevice camera) {
            cameraDevice = camera;
            realStartPreview();
            cameraOpenCloseLock.release();
        }

        @Override
        public void onDisconnected(@NonNull CameraDevice camera) {
            cameraOpenCloseLock.release();
            cameraDevice.close();
            cameraDevice = null;
        }

        @Override
        public void onError(@NonNull CameraDevice camera, int error) {
            Log.i("bad-boy", "打开摄像头失败: " + error);
        }
    };

    private HandlerThread backgroundThread;
    private Handler backgroundHandler;

    public Camera2Helper(Activity activity, int width, int height) {
        super(activity, width, height);
        this.cameraId = CameraCharacteristics.LENS_FACING_FRONT;
        this.cameraManager = (CameraManager) activity.getApplication().getSystemService(Context.CAMERA_SERVICE);

        backgroundThread = new HandlerThread("CameraBackground");
        backgroundThread.start();
        backgroundHandler = new Handler(backgroundThread.getLooper());
    }

    @SuppressLint("MissingPermission")
    private void openCamera(int width, int height) {
        if (null == activity || activity.isFinishing()) {
            return;
        }
        try {
            Log.d(TAG, "tryAcquire");
            if (!cameraOpenCloseLock.tryAcquire(2500, TimeUnit.MILLISECONDS)) {
                throw new RuntimeException("Time out waiting to lock camera opening.");
            }
            // Choose the sizes for camera preview and video recording
            CameraCharacteristics characteristics = cameraManager.getCameraCharacteristics(String.valueOf(cameraId));
            StreamConfigurationMap map = characteristics.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP);
            mSensorOrientation = characteristics.get(CameraCharacteristics.SENSOR_ORIENTATION);
            if (map == null) {
                throw new RuntimeException("Cannot get available preview/video sizes");
            }
            videoSize = chooseVideoSize(map.getOutputSizes(MediaRecorder.class));
            // 输出比例相同，足够大的最接近值
            previewSize = chooseOptimalSize(map.getOutputSizes(SurfaceHolder.class), width, height, videoSize);

            int orientation = activity.getResources().getConfiguration().orientation;
            if( onSizeChangeListener != null ) {
                if (orientation == Configuration.ORIENTATION_LANDSCAPE) {
                    onSizeChangeListener.onChanged(previewSize.getWidth(), previewSize.getHeight());
                } else {
                    onSizeChangeListener.onChanged(previewSize.getHeight(), previewSize.getWidth());
                }
            }
            cameraManager.openCamera(String.valueOf(cameraId), deviceStateCallback, null);
        } catch (CameraAccessException e) {
            Toast.makeText(activity, "Cannot access the camera.", Toast.LENGTH_SHORT).show();
            activity.finish();
        } catch (NullPointerException e) {
            // Currently an NPE is thrown when the Camera2API is used but not supported on the
            // device this code runs.
            throw new RuntimeException("Currently an NPE is thrown when the Camera2API is used but not supported on the device this code runs.");
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera opening.");
        }
    }

    private Size chooseVideoSize(Size[] choices) {
        for (Size size : choices) {
            if (size.getWidth() == size.getHeight() * 4 / 3 && size.getWidth() <= 1080) {
                return size;
            }
        }
        Log.e(TAG, "Couldn't find any suitable video size");
        return choices[choices.length - 1];
    }

    private static Size chooseOptimalSize(Size[] choices, int width, int height, Size aspectRatio) {
        List<Size> bigEnough = new ArrayList<>();
        int w = aspectRatio.getWidth();
        int h = aspectRatio.getHeight();
        for (Size option : choices) {
            if (option.getHeight() == option.getWidth() * h / w && option.getWidth() >= width && option.getHeight() >= height) {
                bigEnough.add(option);
            }
        }
        // Pick the smallest of those, assuming we found any
        if (bigEnough.size() > 0) {
            return Collections.min(bigEnough, new CompareSizesByArea());
        } else {
            Log.e(TAG, "Couldn't find any suitable preview size");
            return choices[0];
        }
    }
    private void closeCamera() {
        try {
            cameraOpenCloseLock.acquire();
            closePreviewSession();
            if (null != cameraDevice) {
                cameraDevice.close();
                cameraDevice = null;
            }
        } catch (InterruptedException e) {
            throw new RuntimeException("Interrupted while trying to lock camera closing.");
        } finally {
            cameraOpenCloseLock.release();
        }
    }


    @SuppressLint("MissingPermission")
    @Override
    public void startPreview() {
        openCamera(width, height);
    }

    private void realStartPreview() {
        if (null == cameraDevice ||
                (surfaceHolder.getSurface() == null || (!surfaceHolder.getSurface().isValid()))
                || null == previewSize) {
            return;
        }
        try {
            closePreviewSession();

            mPreviewBuilder = cameraDevice.createCaptureRequest(CameraDevice.TEMPLATE_PREVIEW);
            mPreviewBuilder.addTarget(surfaceHolder.getSurface());
            cameraDevice.createCaptureSession(Collections.singletonList(surfaceHolder.getSurface()),
                    new CameraCaptureSession.StateCallback() {

                        @Override
                        public void onConfigured(@NonNull CameraCaptureSession session) {
                            cameraCaptureSession = session;
                            updatePreview();
                        }

                        @Override
                        public void onConfigureFailed(@NonNull CameraCaptureSession session) {
                            if (null != activity) {
                                Toast.makeText(activity, "Failed", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void updatePreview() {
        if (null == cameraDevice) {
            return;
        }
        try {
            setUpCaptureRequestBuilder(mPreviewBuilder);
            cameraCaptureSession.setRepeatingRequest(mPreviewBuilder.build(), null, backgroundHandler);
        } catch (CameraAccessException e) {
            e.printStackTrace();
        }
    }

    private void setUpCaptureRequestBuilder(CaptureRequest.Builder requestBuilder) {
        requestBuilder.set(CaptureRequest.CONTROL_MODE, CameraMetadata.CONTROL_MODE_AUTO);
    }

    private void closePreviewSession() {
        if( null != cameraCaptureSession ) {
            cameraCaptureSession.close();
            cameraCaptureSession = null;
        }
    }

    @Override
    public void stopPreview() {

    }

    @Override
    public void switchCamera() {
        if( CameraCharacteristics.LENS_FACING_FRONT == cameraId ) {
            cameraId = CameraCharacteristics.LENS_FACING_BACK;
        } else {
            cameraId = CameraCharacteristics.LENS_FACING_FRONT;
        }
        stopPreview();
        startPreview();
    }
}
