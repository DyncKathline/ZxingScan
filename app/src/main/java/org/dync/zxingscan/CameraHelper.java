package org.dync.zxingscan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.graphics.ImageFormat;
import android.graphics.SurfaceTexture;
import android.hardware.Camera;
import android.opengl.GLES11Ext;
import android.util.Log;
import android.view.Surface;
import android.view.WindowManager;

import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 无界面采集camera数据
 */
public class CameraHelper {

    private static final String TAG = "CameraHelper";

    private Context mContext;
    private int mCameraId;
    private int mFrontCameraId = -1;
    private int mBackCameraId = -1;
    private int facing = CAMERA_FACING_BACK;
    private Camera mCamera;
    private SurfaceTexture surfaceTexture = new SurfaceTexture(GLES11Ext.GL_TEXTURE_EXTERNAL_OES);
    private boolean shut = false;
    private PreviewCallback mPreviewCallback;

    public static final int CAMERA_FACING_BACK = Camera.CameraInfo.CAMERA_FACING_BACK;
    public static final int CAMERA_FACING_FRONT = Camera.CameraInfo.CAMERA_FACING_FRONT;
    public static int DEFAULT_REQUESTED_CAMERA_PREVIEW_WIDTH = 640;
    public static int DEFAULT_REQUESTED_CAMERA_PREVIEW_HEIGHT = 480;

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
        Log.d(TAG, "use Camera[" + cameraId + "].");
        Camera camera = mCamera;
        if (camera != null) {
            throw new RuntimeException("You must close previous camera before open a new one.");
        }
        if (ContextCompat.checkSelfPermission(mContext, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            try {
                mCamera = Camera.open(cameraId);
            }catch (Exception e) {
                e.printStackTrace();
            }
            if(mCamera != null) {
                mCameraId = cameraId;
                Log.d(TAG, "Camera[" + cameraId + "] has been opened.");
                int orientation = getCameraDisplayOrientation(mContext, cameraId);
                mCamera.setDisplayOrientation(orientation);
                Camera.Parameters parameters = mCamera.getParameters();
                parameters.setRotation(orientation);
                mCamera.setParameters(parameters);
            }
        }else {
            Log.e(TAG, "Camera permission hasn't opened.");
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

    public void setFacing(int facing) {
        this.facing = facing;
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
            int requestedCameraId = getIdForRequestedCamera(facing);
            if (requestedCameraId == -1) {
                int numCameras = Camera.getNumberOfCameras();
                int cameraId = 0;
                while (cameraId < numCameras) {
                    Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
                    Camera.getCameraInfo(cameraId, cameraInfo);
                    if (cameraInfo.facing == CAMERA_FACING_FRONT) {
                        break;
                    }
                    cameraId++;
                }
                if (cameraId == numCameras) {
                    Log.i(TAG, "No camera facing " + CAMERA_FACING_BACK + "; returning camera #0");
                    cameraId = 0;
                }
                requestedCameraId = cameraId;
            }
            openCamera(requestedCameraId);
            if(mCamera == null) {
                return;
            }

            SizePair sizePair =
                    selectSizePair(
                            mCamera,
                            DEFAULT_REQUESTED_CAMERA_PREVIEW_WIDTH,
                            DEFAULT_REQUESTED_CAMERA_PREVIEW_HEIGHT);

            Camera.Parameters parameters = mCamera.getParameters();
            parameters.setPreviewFormat(ImageFormat.NV21);
            parameters.setPreviewSize(sizePair.preview.getWidth(), sizePair.preview.getHeight());
            if(sizePair.picture != null) {
                parameters.setPictureSize(sizePair.picture.getWidth(), sizePair.picture.getHeight());
            }

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

    public Camera getCamera() {
        return mCamera;
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
    private int getIdForRequestedCamera(int facing) {
        Camera.CameraInfo cameraInfo = new Camera.CameraInfo();
        for (int i = 0; i < Camera.getNumberOfCameras(); ++i) {
            Camera.getCameraInfo(i, cameraInfo);
            if (cameraInfo.facing == facing) {
                return i;
            }
        }
        return -1;
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

    ///////////////////////////////////计算合适的camera预览和拍照宽高////////////////////////////////////////////
    /**
     * If the absolute difference between a preview size aspect ratio and a picture size aspect ratio
     * is less than this tolerance, they are considered to be the same aspect ratio.
     */
    private static final float ASPECT_RATIO_TOLERANCE = 0.01f;

    /**
     * Selects the most suitable preview and picture size, given the desired width and height.
     *
     * <p>Even though we only need to find the preview size, it's necessary to find both the preview
     * size and the picture size of the camera together, because these need to have the same aspect
     * ratio. On some hardware, if you would only set the preview size, you will get a distorted
     * image.
     *
     * @param camera the camera to select a preview size from
     * @param desiredWidth the desired width of the camera preview frames
     * @param desiredHeight the desired height of the camera preview frames
     * @return the selected preview and picture size pair
     */
    public static SizePair selectSizePair(Camera camera, int desiredWidth, int desiredHeight) {
        List<SizePair> validPreviewSizes = generateValidPreviewSizeList(camera);

        // The method for selecting the best size is to minimize the sum of the differences between
        // the desired values and the actual values for width and height.  This is certainly not the
        // only way to select the best size, but it provides a decent tradeoff between using the
        // closest aspect ratio vs. using the closest pixel area.
        SizePair selectedPair = null;
        int minDiff = Integer.MAX_VALUE;
        for (SizePair sizePair : validPreviewSizes) {
            Size size = sizePair.preview;
            int diff =
                    Math.abs(size.getWidth() - desiredWidth) + Math.abs(size.getHeight() - desiredHeight);
            if (diff < minDiff) {
                selectedPair = sizePair;
                minDiff = diff;
            }
        }

        return selectedPair;
    }

    /**
     * Stores a preview size and a corresponding same-aspect-ratio picture size. To avoid distorted
     * preview images on some devices, the picture size must be set to a size that is the same aspect
     * ratio as the preview size or the preview may end up being distorted. If the picture size is
     * null, then there is no picture size with the same aspect ratio as the preview size.
     */
    public static class SizePair {
        public final Size preview;
        @Nullable
        public final Size picture;

        SizePair(Camera.Size previewSize, @Nullable Camera.Size pictureSize) {
            preview = new Size(previewSize.width, previewSize.height);
            picture = pictureSize != null ? new Size(pictureSize.width, pictureSize.height) : null;
        }

        public SizePair(Size previewSize, @Nullable Size pictureSize) {
            preview = previewSize;
            picture = pictureSize;
        }
    }

    /**
     * Generates a list of acceptable preview sizes. Preview sizes are not acceptable if there is not
     * a corresponding picture size of the same aspect ratio. If there is a corresponding picture size
     * of the same aspect ratio, the picture size is paired up with the preview size.
     *
     * <p>This is necessary because even if we don't use still pictures, the still picture size must
     * be set to a size that is the same aspect ratio as the preview size we choose. Otherwise, the
     * preview images may be distorted on some devices.
     */
    public static List<SizePair> generateValidPreviewSizeList(Camera camera) {
        Camera.Parameters parameters = camera.getParameters();
        List<Camera.Size> supportedPreviewSizes = parameters.getSupportedPreviewSizes();
        List<Camera.Size> supportedPictureSizes = parameters.getSupportedPictureSizes();
        List<SizePair> validPreviewSizes = new ArrayList<>();
        for (Camera.Size previewSize : supportedPreviewSizes) {
            float previewAspectRatio = (float) previewSize.width / (float) previewSize.height;

            // By looping through the picture sizes in order, we favor the higher resolutions.
            // We choose the highest resolution in order to support taking the full resolution
            // picture later.
            for (Camera.Size pictureSize : supportedPictureSizes) {
                float pictureAspectRatio = (float) pictureSize.width / (float) pictureSize.height;
                if (Math.abs(previewAspectRatio - pictureAspectRatio) < ASPECT_RATIO_TOLERANCE) {
                    validPreviewSizes.add(new SizePair(previewSize, pictureSize));
                    break;
                }
            }
        }

        // If there are no picture sizes with the same aspect ratio as any preview sizes, allow all
        // of the preview sizes and hope that the camera can handle it.  Probably unlikely, but we
        // still account for it.
        if (validPreviewSizes.size() == 0) {
            Log.w(TAG, "No preview sizes have a corresponding same-aspect-ratio picture size");
            for (Camera.Size previewSize : supportedPreviewSizes) {
                // The null picture size will let us know that we shouldn't set a picture size.
                validPreviewSizes.add(new SizePair(previewSize, null));
            }
        }

        return validPreviewSizes;
    }

}
