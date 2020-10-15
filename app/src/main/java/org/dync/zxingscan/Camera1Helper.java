package org.dync.zxingscan;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.hardware.Camera;
import android.support.v4.content.ContextCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.WindowManager;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
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
            assert mCamera != null;
            mCamera.setDisplayOrientation(getCameraDisplayOrientation(mContext, cameraId, mCamera));
//            if (mContext.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
//                mCamera.setDisplayOrientation(90);
//            } else {//如果是横屏
//                mCamera.setDisplayOrientation(0);
//            }
        }
    }

    public int getCameraDisplayOrientation(Context activity, int cameraId, Camera camera) {
        android.hardware.Camera.CameraInfo info =
                new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);

//        int rotation = activity.getWindowManager().getDefaultDisplay()
//                .getRotation();
        WindowManager wm = (WindowManager) activity.getSystemService(Context.WINDOW_SERVICE);
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
        Camera.Size size = calculatePerfectSize(parameters.getSupportedPreviewSizes(),
                expectWidth, expectHeight, CalculateType.Lower);
        size = getBestSize(expectWidth, expectHeight, parameters.getSupportedPreviewSizes());
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
        Camera.Size size = calculatePerfectSize(parameters.getSupportedPictureSizes(),
                expectWidth, expectHeight, CalculateType.Max);
        size = getBestSize(expectWidth, expectHeight, parameters.getSupportedPictureSizes());
        parameters.setPictureSize(size.width, size.height);
        camera.setParameters(parameters);
    }


    //获取与指定宽高相等或最接近的尺寸
    private Camera.Size getBestSize(int targetWidth, int targetHeight, List<Camera.Size> sizeList) {
        Camera.Size bestSize = null;
        double targetRatio = (targetHeight * 1d / targetWidth);  //目标大小的宽高比
        double minDiff = targetRatio;

        for (Camera.Size size : sizeList) {
            double supportedRatio = (size.width * 1d / size.height);
            Log.d(TAG, String.format("系统支持的尺寸 : %d * %d ,    比例%f", size.width, size.height, supportedRatio));
        }

        for (Camera.Size size : sizeList) {
            if (size.width == targetHeight && size.height == targetWidth) {
                bestSize = size;
                break;
            }

            double supportedRatio = (size.width * 1d / size.height);
            if (Math.abs(supportedRatio - targetRatio) < minDiff) {
                minDiff = Math.abs(supportedRatio - targetRatio);
                bestSize = size;
            }
        }
        Log.d(TAG, String.format("目标尺寸 ：%d * %d ，   比例  %f", targetWidth, targetHeight, targetRatio));
        Log.d(TAG, String.format("最优尺寸 ：%d * %d", bestSize != null ? bestSize.height : 0, bestSize != null ? bestSize.width : 0));
        return bestSize;
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
    public void takePicture() {
        Camera camera = mCamera;
        if (camera != null) {
            Camera.Parameters parameters = camera.getParameters();
            camera.setParameters(parameters);
            camera.takePicture(null, null, new Camera.PictureCallback() {
                @Override
                public void onPictureTaken(byte[] data, Camera camera) {
                    mCamera.startPreview();
                    // 在使用完 Buffer 之后记得回收复用。
//                    camera.addCallbackBuffer(data);
                }
            });
        }
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
        DisplayMetrics mDisplayMetrics = mContext.getResources()
                .getDisplayMetrics();
        int mScreenWidth = mDisplayMetrics.widthPixels;
        int mScreenHeight = mDisplayMetrics.heightPixels;
//        int width = mScreenWidth;
//        int height = mScreenHeight;
        int width = mSurfaceView.getWidth();
        int height = mSurfaceView.getHeight();
        setPreviewSize(mCamera, width, height);// 配置预览尺寸
        setPictureSize(mCamera, width, height);
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

    enum CalculateType {
        Min,            // 最小
        Max,            // 最大
        Larger,         // 大一点
        Lower,          // 小一点
    }

    /**
     * 计算最完美的Size
     *
     * @param sizes
     * @param expectWidth
     * @param expectHeight
     * @return
     */
    private static Camera.Size calculatePerfectSize(List<Camera.Size> sizes, int expectWidth,
                                                    int expectHeight, CalculateType calculateType) {
        sortList(sizes); // 根据宽度进行排序

        // 根据当前期望的宽高判定
        List<Camera.Size> bigEnough = new ArrayList<>();
        List<Camera.Size> noBigEnough = new ArrayList<>();
        for (Camera.Size size : sizes) {
            if (size.height * expectWidth / expectHeight == size.width) {
                if (size.width > expectWidth && size.height > expectHeight) {
                    bigEnough.add(size);
                } else {
                    noBigEnough.add(size);
                }
            }
        }
        // 根据计算类型判断怎么如何计算尺寸
        Camera.Size perfectSize = null;
        switch (calculateType) {
            // 直接使用最小值
            case Min:
                // 不大于期望值的分辨率列表有可能为空或者只有一个的情况，
                // Collections.min会因越界报NoSuchElementException
                if (noBigEnough.size() > 1) {
                    perfectSize = Collections.min(noBigEnough, new CompareAreaSize());
                } else if (noBigEnough.size() == 1) {
                    perfectSize = noBigEnough.get(0);
                }
                break;

            // 直接使用最大值
            case Max:
                // 如果bigEnough只有一个元素，使用Collections.max就会因越界报NoSuchElementException
                // 因此，当只有一个元素时，直接使用该元素
                if (bigEnough.size() > 1) {
                    perfectSize = Collections.max(bigEnough, new CompareAreaSize());
                } else if (bigEnough.size() == 1) {
                    perfectSize = bigEnough.get(0);
                }
                break;

            // 小一点
            case Lower:
                // 优先查找比期望尺寸小一点的，否则找大一点的，接受范围在0.8左右
                if (noBigEnough.size() > 0) {
                    Camera.Size size = Collections.max(noBigEnough, new CompareAreaSize());
                    if (((float) size.width / expectWidth) >= 0.8
                            && ((float) size.height / expectHeight) > 0.8) {
                        perfectSize = size;
                    }
                } else if (bigEnough.size() > 0) {
                    Camera.Size size = Collections.min(bigEnough, new CompareAreaSize());
                    if (((float) expectWidth / size.width) >= 0.8
                            && ((float) (expectHeight / size.height)) >= 0.8) {
                        perfectSize = size;
                    }
                }
                break;

            // 大一点
            case Larger:
                // 优先查找比期望尺寸大一点的，否则找小一点的，接受范围在0.8左右
                if (bigEnough.size() > 0) {
                    Camera.Size size = Collections.min(bigEnough, new CompareAreaSize());
                    if (((float) expectWidth / size.width) >= 0.8
                            && ((float) (expectHeight / size.height)) >= 0.8) {
                        perfectSize = size;
                    }
                } else if (noBigEnough.size() > 0) {
                    Camera.Size size = Collections.max(noBigEnough, new CompareAreaSize());
                    if (((float) size.width / expectWidth) >= 0.8
                            && ((float) size.height / expectHeight) > 0.8) {
                        perfectSize = size;
                    }
                }
                break;
        }
        // 如果经过前面的步骤没找到合适的尺寸，则计算最接近expectWidth * expectHeight的值
        if (perfectSize == null) {
            Camera.Size result = sizes.get(0);
            boolean widthOrHeight = false; // 判断存在宽或高相等的Size
            // 辗转计算宽高最接近的值
            for (Camera.Size size : sizes) {
                // 如果宽高相等，则直接返回
                if (size.width == expectWidth && size.height == expectHeight) {
                    result = size;
                    break;
                }
                // 仅仅是宽度相等，计算高度最接近的size
                if (size.width == expectWidth) {
                    widthOrHeight = true;
                    if (Math.abs(result.height - expectHeight) > Math.abs(size.height - expectHeight)) {
                        result = size;
                        break;
                    }
                }
                // 高度相等，则计算宽度最接近的Size
                else if (size.height == expectHeight) {
                    widthOrHeight = true;
                    if (Math.abs(result.width - expectWidth) > Math.abs(size.width - expectWidth)) {
                        result = size;
                        break;
                    }
                }
                // 如果之前的查找不存在宽或高相等的情况，则计算宽度和高度都最接近的期望值的Size
                else if (!widthOrHeight) {
                    if (Math.abs(result.width - expectWidth) > Math.abs(size.width - expectWidth)
                            && Math.abs(result.height - expectHeight) > Math.abs(size.height - expectHeight)) {
                        result = size;
                    }
                }
            }
            perfectSize = result;
        }
        return perfectSize;
    }

    /**
     * 分辨率由大到小排序
     *
     * @param list
     */
    private static void sortList(List<Camera.Size> list) {
        Collections.sort(list, new CompareAreaSize());
    }

    /**
     * 比较器
     */
    private static class CompareAreaSize implements Comparator<Camera.Size> {
        @Override
        public int compare(Camera.Size pre, Camera.Size after) {
            return Long.signum((long) pre.width * pre.height -
                    (long) after.width * after.height);
        }
    }
}
