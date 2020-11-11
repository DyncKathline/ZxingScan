package org.dync.zxingscan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.support.v4.content.ContextCompat;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import java.io.IOException;

/**
 * 无界面采集camera数据
 */
public class CameraHelper {

    private static final String TAG = "CameraHelper";

    private Context mContext;
    private int mCameraId;
    private int mFrontCameraId = -1;
    private int mBackCameraId = -1;
    private Camera mCamera;
    private SurfaceTexture surfaceTexture = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
    private boolean shut = false;
    private PreviewCallback mPreviewCallback;

    public void setPreviewCallback(PreviewCallback callback) {
        this.mPreviewCallback = callback;
    }

    public CameraHelper(Context context) {
        mContext = context;
        initCameraInfo();
    }

    /**
     * 初始化摄像头信息。
     */
    private void initCameraInfo() {
        if(mContext.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA)) {
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
            int orientation = getCameraDisplayOrientation(mContext, cameraId);
            mCamera.setDisplayOrientation(orientation);
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setRotation(orientation);
            mCamera.setParameters(parameters);
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
        mCamera = null;
        if (camera != null) {
            camera.release();
            mCameraId = -1;
        }
    }

    public void switchCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
        if (numberOfCameras == 1) {
            return;
        }
        int cameraId = switchCameraId();// 切换摄像头 ID
        mCameraId = cameraId;
        stopPreview();
        startPreview();
    }

    public void startPreview() {
        try {
            openCamera(getCameraId());
            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            parameters.setPreviewSize(640, 480);
            parameters.setPictureSize(640, 480);

            mCamera.setParameters(parameters);
            mCamera.setPreviewCallback(new Camera.PreviewCallback() {
                @Override
                public void onPreviewFrame(byte[] data, Camera camera) {
                    if (mPreviewCallback != null) {
                        mPreviewCallback.onPreviewFrame(data);
                    }
                }
            });

            mCamera.setPreviewTexture(surfaceTexture);
            mCamera.startPreview();
        } catch (IOException e) {
            Log.e(TAG, "start preview failed");
            e.printStackTrace();
        }
    }

    public void stopPreview() {
        if (mCamera != null) {
            mCamera.setPreviewCallback(null);
            mCamera.stopPreview();
            mCamera.release();
            mCamera = null;
        }
    }

    public void releaseCamera() {
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

    /**
     * 拍照
     */
    public void takePicture(final PictureCallback callback) {
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

    public interface PreviewCallback {
        /**
         * 预览流回调
         *
         * @param data yuv格式的数据流
         */
        void onPreviewFrame(byte[] data);
    }

}
