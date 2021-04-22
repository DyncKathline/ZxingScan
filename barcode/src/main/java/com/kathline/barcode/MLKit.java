package com.kathline.barcode;

import android.Manifest;
import android.content.DialogInterface;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.kathline.barcode.barcodescanner.BarcodeScannerProcessor;

import java.io.IOException;
import java.util.List;

public class MLKit implements LifecycleObserver {

    private static final String TAG = "MLKit";
    private FragmentActivity activity;
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private boolean isAnalyze = true;
    public BarcodeScannerOptions options;

    public MLKit(FragmentActivity activity, CameraSourcePreview preview, GraphicOverlay graphicOverlay) {
        this.activity = activity;
        this.preview = preview;
        this.graphicOverlay = graphicOverlay;
        activity.getLifecycle().addObserver(this);
        onCreate();
    }

    public void onCreate() {
        requirePermission();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_RESUME)
    public void onResume() {
        Log.d(TAG, "onResume");
        createCameraSource();
        startCameraSource();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_PAUSE)
    public void onPause() {
        Log.d(TAG, "onPause");
        preview.stop();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    public interface OnScanListener {
        void onSuccess(List<Barcode> barcodes, @NonNull GraphicOverlay graphicOverlay);
        void onFail(int code, Exception e);
    }

    public OnScanListener onScanListener;

    public void setOnScanListener(OnScanListener listener) {
        onScanListener = listener;
    }

    private void requirePermission() {
        PermissionUtil.getInstance().with(activity).requestPermissions(new String[]{Manifest.permission.CAMERA,
                Manifest.permission.VIBRATE}, new PermissionUtil.PermissionListener() {
            @Override
            public void onGranted() {
                createCameraSource();
            }

            @Override
            public void onDenied(List<String> deniedPermission) {
                PermissionUtil.getInstance().showDialogTips( deniedPermission, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        activity.finish();
                    }
                });
            }

            @Override
            public void onShouldShowRationale(List<String> deniedPermission) {
                requirePermission();
            }
        });
    }

    /**
     * 设置是否分析图像，通过此方法可以动态控制是否分析图像，常用于中断扫码识别。如：连扫时，扫到结果，然后停止分析图像
     *
     * 1. 因为分析图像默认为true，如果想支持连扫，在{@link OnScanListener#onSuccess(List<Barcode>, GraphicOverlay)}返回true拦截即可。
     * 当连扫的处理逻辑比较复杂时，请在处理逻辑前通过调用setAnalyzeImage(false)来停止分析图像，
     * 等逻辑处理完后再调用setAnalyze(true)来继续分析图像。
     *
     * 2. 如果只是想拦截扫码结果回调自己处理逻辑，但并不想继续分析图像（即不想连扫），可通过
     * 调用setAnalyze(false)来停止分析图像。
     * @param isAnalyze
     */
    public void setAnalyze(boolean isAnalyze) {
        this.isAnalyze = isAnalyze;
    }

    public boolean isAnalyze() {
        return isAnalyze;
    }

    public void setBarcodeFormats(BarcodeScannerOptions options) {
        this.options = options;
    }

    public void switchCamera(boolean isFront) {
        if (cameraSource != null) {
            if (isFront) {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
            } else {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
            }
        }
        preview.stop();
        startCameraSource();
    }

    private void createCameraSource() {
        // If there's no existing cameraSource, create one.
        if (cameraSource == null) {
            cameraSource = new CameraSource(activity, graphicOverlay);
        }

        cameraSource.setMachineLearningFrameProcessor(new BarcodeScannerProcessor(activity, this));
    }

    /**
     * Starts or restarts the camera source, if it exists. If the camera source doesn't exist yet
     * (e.g., because onResume was called before the camera source was created), this will be called
     * again when the camera source is created.
     */
    private void startCameraSource() {
        if (cameraSource != null) {
            try {
                if (preview == null) {
                    Log.d(TAG, "resume: Preview is null");
                }
                if (graphicOverlay == null) {
                    Log.d(TAG, "resume: graphOverlay is null");
                }
                preview.start(cameraSource, graphicOverlay);
            } catch (IOException e) {
                Log.e(TAG, "Unable to start camera source.", e);
                cameraSource.release();
                cameraSource = null;
            }
        }
    }
}
