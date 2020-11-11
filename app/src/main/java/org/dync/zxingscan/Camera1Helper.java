package org.dync.zxingscan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.List;

public class Camera1Helper {

    private final String TAG = Camera1Helper.class.getSimpleName();
    private Context mContext;
    private SurfaceView mSurfaceView;
    private int mFrontCameraId = -1;
    private int mBackCameraId = -1;
    private Camera mCamera;
    private int mCameraId = -1;

    public Camera1Helper(SurfaceView surfaceView) {
        mSurfaceView = surfaceView;
        mContext = mSurfaceView.getContext();
        initCameraInfo();
        mSurfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
            @Override
            public void surfaceCreated(SurfaceHolder holder) {
                openCamera(getCameraId());
                setCameraSize();
                startPreview();// 开启预览
            }

            @Override
            public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

            }

            @Override
            public void surfaceDestroyed(SurfaceHolder holder) {
                stopPreview();// 停止预览
                closeCamera();// 关闭当前的摄像头
            }
        });
    }

    /**
     * 初始化摄像头信息。
     */
    private void initCameraInfo() {
        int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
        for (int cameraId = 0; cameraId < numberOfCameras; cameraId++) {
            Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
            Camera.getCameraInfo(cameraId, cameraInfo);
            if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                // 后置摄像头信息
                mBackCameraId = cameraId;
            } else if (cameraInfo.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                // 前置摄像头信息
                mFrontCameraId = cameraId;
            }
        }
    }

    /**
     * 开启指定摄像头
     */
    private void openCamera(int cameraId) {
        Camera camera = mCamera;
        if (camera != null) {
            throw new RuntimeException("You must close previous camera before open a new one.");
        }
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            mCamera = Camera.open(cameraId);
            mCameraId = cameraId;
            Log.d(TAG, "Camera[" + cameraId + "] has been opened.");
            mCamera.setDisplayOrientation(getCameraDisplayOrientation(mContext, cameraId));
        }
    }

    public int getCameraDisplayOrientation(Context context, int cameraId) {
        Camera.CameraInfo info =
                new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);
        int rotation = wm.getDefaultDisplay().getRotation();
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
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }
        if (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            if (result != 90 && result != 270) {//正常手机都是竖屏旋转90或270度
                if (rotation == Surface.ROTATION_90) {
                    result = (result + 270) % 360;
                } else if (rotation == Surface.ROTATION_270) {
                    result = (result + 90) % 360;
                }
            }
        }else {//正常手机都是横屏屏旋转0或180度
            if (result != 0 && result != 180) {
                if (rotation == Surface.ROTATION_0) {
                    result = (result + 90) % 360;
                } else if (rotation == Surface.ROTATION_180) {
                    result = (result + 270) % 360;
                }
            }
        }
        String msg = "PORTRAIT is " + (context.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) + ", Camera[" + cameraId + "] rotation is " + info.orientation + ", device rotation is " + degrees + ", result is " + result;
        Log.d(TAG, msg);
        return result;
    }

    /**
     * 关闭相机。
     */
    private void closeCamera() {
        Camera camera = mCamera;
        if (camera != null) {
            camera.release();
            mCameraId = -1;
            mCamera = null;
        }
    }

    /**
     * 判断指定的预览格式是否支持。
     */
    private boolean isPreviewFormatSupported(Camera.Parameters parameters, int format) {
        List<Integer> supportedPreviewFormats = parameters.getSupportedPreviewFormats();
        return supportedPreviewFormats != null && supportedPreviewFormats.contains(format);
    }

    /**
     * 设置预览大小
     *
     * @param camera
     * @param expectWidth
     * @param expectHeight
     */
    private void setPreviewSize(Camera camera, int expectWidth, int expectHeight) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = findBestPreviewSize(parameters.getSupportedPreviewSizes(), expectWidth, expectHeight);
        parameters.setPreviewSize(size.width, size.height);
        camera.setParameters(parameters);
    }

    /**
     * 设置拍摄的照片大小
     *
     * @param camera
     * @param expectWidth
     * @param expectHeight
     */
    private void setPictureSize(Camera camera, int expectWidth, int expectHeight) {
        Camera.Parameters parameters = camera.getParameters();
        Camera.Size size = findBestPreviewSize(parameters.getSupportedPictureSizes(), expectWidth, expectHeight);
        parameters.setPictureSize(size.width, size.height);
        camera.setParameters(parameters);
    }

    private static final int MIN_PREVIEW_PIXELS = 480 * 320; // normal screen
    private static final double MAX_ASPECT_DISTORTION = 0.05;
    public Camera.Size findBestPreviewSize(List<Camera.Size> rawSupportedSizes, int expectWidth, int expectHeight) {
        if (Log.isLoggable(TAG, Log.INFO)) {
            StringBuilder previewSizesString = new StringBuilder();
            for (Camera.Size size : rawSupportedSizes) {
                previewSizesString.append(size.width).append('x').append(size.height).append(' ');
            }
            Log.i(TAG, "Supported preview sizes: " + previewSizesString);
        }

        double screenAspectRatio;
        if(expectWidth < expectHeight){
            screenAspectRatio = expectWidth / (double) expectHeight;
        }else{
            screenAspectRatio = expectHeight / (double) expectWidth;
        }
        Log.i(TAG, "screenAspectRatio: " + screenAspectRatio);
        // Find a suitable size, with max resolution
        int maxResolution = 0;

        Camera.Size maxResPreviewSize = null;
        for (Camera.Size size : rawSupportedSizes) {
            int realWidth = size.width;
            int realHeight = size.height;
            int resolution = realWidth * realHeight;
            if (resolution < MIN_PREVIEW_PIXELS) {
                continue;
            }

            boolean isCandidatePortrait = realWidth < realHeight;
            int maybeFlippedWidth = isCandidatePortrait ? realWidth: realHeight ;
            int maybeFlippedHeight = isCandidatePortrait ? realHeight : realWidth;
            Log.i(TAG, String.format("maybeFlipped:%d * %d",maybeFlippedWidth,maybeFlippedHeight));

            double aspectRatio = maybeFlippedWidth / (double) maybeFlippedHeight;
            Log.i(TAG, "aspectRatio: " + aspectRatio);
            double distortion = Math.abs(aspectRatio - screenAspectRatio);
            Log.i(TAG, "distortion: " + distortion);
            if (distortion > MAX_ASPECT_DISTORTION) {
                continue;
            }

            if (maybeFlippedWidth == expectWidth && maybeFlippedHeight == expectHeight) {
                Log.i(TAG, "Found preview size exactly matching screen size: " + size);
                return size;
            }

            // Resolution is suitable; record the one with max resolution
            if (resolution > maxResolution) {
                maxResolution = resolution;
                maxResPreviewSize = size;
            }
        }

        // If no exact match, use largest preview size. This was not a great idea on older devices because
        // of the additional computation needed. We're likely to get here on newer Android 4+ devices, where
        // the CPU is much more powerful.
        if (maxResPreviewSize == null) {
            throw new IllegalStateException("Parameters contained no preview size!");
        }
        Log.i(TAG, "No suitable preview sizes, using default: " + maxResPreviewSize);
        return maxResPreviewSize;
    }

    /**
     * 设置预览 Surface。
     */
    private void setPreviewSurface(SurfaceHolder previewSurface) {
        Camera camera = mCamera;
        if (camera != null && previewSurface != null) {
            try {
                camera.setPreviewDisplay(previewSurface);
                Log.d(TAG, "setPreviewSurface() called");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 开始预览。
     */
    private void startPreview() {
        Camera camera = mCamera;
        SurfaceHolder previewSurface = mSurfaceView.getHolder();
        if (camera != null && previewSurface != null) {
            camera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {

                }
            });
            camera.startPreview();
            Log.d(TAG, "startPreview() called");
        }
    }

    /**
     * 停止预览。
     */
    private void stopPreview() {
        Camera camera = mCamera;
        if (camera != null) {
            camera.stopPreview();
            camera.setPreviewCallback(null);
            Log.d(TAG, "stopPreview() called");
        }
    }

    /**
     * 获取要开启的相机 ID，优先开启前置。
     */
    private int getCameraId() {
        if (hasFrontCamera()) {
            return mFrontCameraId;
        } else if (hasBackCamera()) {
            return mBackCameraId;
        } else {
            throw new RuntimeException("No available camera id found.");
        }
    }

    /**
     * 拍照。
     */
    public void takePicture(final CameraHelper.PictureCallback callback) {
        Camera camera = mCamera;
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            camera.setParameters(parameters);
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    if(callback != null) {
                        callback.onPictureTaken(data, camera);
                    }
                    mCamera.startPreview();
                }
            });
        }
    }

    public interface PictureCallback {
        void onPictureTaken(byte[] data, Camera camera);
    }

    public void switchCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
        if (numberOfCameras == 1) {
            return;
        }
        int cameraId = switchCameraId();// 切换摄像头 ID
        stopPreview();// 停止预览
        closeCamera();// 关闭当前的摄像头
        openCamera(cameraId);// 开启新的摄像头
        setCameraSize();
        startPreview();// 开启预览
    }

    private void setCameraSize() {
        int width = mSurfaceView.getWidth();
        int height = mSurfaceView.getHeight();
        setPreviewSize(mCamera, width, height);// 配置预览尺寸
//        setPictureSize(mCamera, width, height);
        setPreviewSurface(mSurfaceView.getHolder());// 配置预览 Surface
    }

    public void release() {
        stopPreview();// 停止预览
        closeCamera();// 关闭当前的摄像头
    }

    /**
     * 切换前后置时切换ID
     */
    private int switchCameraId() {
        if (mCameraId == mFrontCameraId && hasBackCamera()) {
            return mBackCameraId;
        } else if (mCameraId == mBackCameraId && hasFrontCamera()) {
            return mFrontCameraId;
        } else {
            throw new RuntimeException("No available camera id to switch.");
        }
    }

    /**
     * 判断是否有后置摄像头。
     *
     * @return true 代表有后置摄像头
     */
    private boolean hasBackCamera() {
        return mBackCameraId != -1;
    }

    /**
     * 判断是否有前置摄像头。
     *
     * @return true 代表有前置摄像头
     */
    private boolean hasFrontCamera() {
        return mFrontCameraId != -1;
    }
}
