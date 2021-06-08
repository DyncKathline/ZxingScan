package com.kathline.barcode;

import android.Manifest;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.graphics.Rect;
import android.hardware.Camera;
import android.text.TextUtils;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.NonNull;
import androidx.fragment.app.FragmentActivity;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.LifecycleObserver;
import androidx.lifecycle.OnLifecycleEvent;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScanner;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.BarcodeScanning;
import com.google.mlkit.vision.common.InputImage;
import com.kathline.barcode.barcodescanner.BarcodeGraphic;
import com.kathline.barcode.barcodescanner.BarcodeScannerProcessor;
import com.kathline.barcode.hardware.BeepManager;

import java.io.IOException;
import java.util.List;

public class MLKit implements LifecycleObserver {

    private static final String TAG = "MLKit";
    private FragmentActivity activity;
    private CameraSource cameraSource = null;
    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private boolean isAnalyze = true;//是否分析结果
    private boolean isContinuousScanning = true;//是否连续扫描
    public BarcodeScannerOptions options;

    private BeepManager beepManager;
    boolean isOpenLight = false;
    boolean playBeep = true;
    boolean vibrate = true;
    private BarcodeScanner barcodeScanner;

    public MLKit(FragmentActivity activity, CameraSourcePreview preview, GraphicOverlay graphicOverlay) {
        this.activity = activity;
        this.preview = preview;
        this.graphicOverlay = graphicOverlay;
        activity.getLifecycle().addObserver(this);
        onCreate();
    }

    public void onCreate() {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if(beepManager == null) {
            beepManager = new BeepManager(activity);
        }
        createCameraSource();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onStart() {
        Log.d(TAG, "onStart");
        createCameraSource();
        startCameraSource();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onStop() {
        Log.d(TAG, "onStop");
        preview.stop();
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_DESTROY)
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        if (cameraSource != null) {
            cameraSource.release();
        }
    }

    public synchronized void scanningImage(String photoPath) {
        if (TextUtils.isEmpty(photoPath)) {
            onScanListener.onFail(2, new Exception("photo url is null!"));
        }
        Bitmap bitmap = BitmapUtils.decodeBitmapFromPath(photoPath, 600, 600, false);

        detectInImage(bitmap, graphicOverlay);
    }

    private void detectInImage(Bitmap bitmap, final GraphicOverlay graphicOverlay) {
        InputImage image = InputImage.fromBitmap(bitmap, 0);
        if(options != null) {
            barcodeScanner = BarcodeScanning.getClient(options);
        }else {
            barcodeScanner = BarcodeScanning.getClient();
        }
        // Or, to specify the formats to recognize:
        // BarcodeScanner scanner = BarcodeScanning.getClient(options);
        // [END get_detector]

        // [START run_detector]
        Task<List<Barcode>> result = barcodeScanner.process(image)
                .addOnSuccessListener(new OnSuccessListener<List<Barcode>>() {
                    @Override
                    public void onSuccess(List<Barcode> barcodes) {
                        if (barcodes.isEmpty()) {
                            Log.v(TAG, "No barcode has been detected");
                        }
                        if(isAnalyze()) {
                            if(onScanListener != null) {
                                if(!barcodes.isEmpty()) {
                                    playBeepAndVibrate();
                                }
                                onScanListener.onSuccess(barcodes, graphicOverlay,
                                        InputImage.fromBitmap(bitmap, 0));
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.e(TAG, "Barcode detection failed " + e);
                        if(onScanListener != null) {
                            onScanListener.onFail(1, e);
                        }
                    }
                });
    }

    public interface OnScanListener {
        void onSuccess(List<Barcode> barcodes, @NonNull GraphicOverlay graphicOverlay, InputImage image);
        void onFail(int code, Exception e);
    }

    public OnScanListener onScanListener;

    public void setOnScanListener(OnScanListener listener) {
        onScanListener = listener;
    }

    /**
     * 设置是否分析图像，通过此方法可以动态控制是否分析图像，常用于中断扫码识别。如：连扫时，扫到结果，然后停止分析图像
     *
     * 1. 因为分析图像默认为true，如果想支持连扫，设置setAnalyze(false)即可。
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

    public void switchCamera() {
        int numberOfCameras = Camera.getNumberOfCameras();// 获取摄像头个数
        if (numberOfCameras == 1) {
            return;
        }
        if(cameraSource != null) {
            if(cameraSource.getCameraFacing() == CameraSource.CAMERA_FACING_FRONT) {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_BACK);
            }else {
                cameraSource.setFacing(CameraSource.CAMERA_FACING_FRONT);
            }
        }
        preview.stop();
        startCameraSource();
    }

    public void setPlayBeepAndVibrate(boolean playBeep, boolean vibrate) {
        this.playBeep = playBeep;
        this.vibrate = vibrate;
    }

    public void playBeepAndVibrate() {
        if(beepManager != null) {
            beepManager.playBeepSoundAndVibrate(playBeep, vibrate);
        }
    }

    public boolean hasLight() {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    /**
     * 开关闪关灯
     */
    public void switchLight() {
        if (hasLight()) {
            if (isOpenLight) {
                closeTorch();
            } else {
                openTorch();
            }
            isOpenLight = !isOpenLight;
        }
    }

    public void openTorch() {
        if(cameraSource != null) {
            cameraSource.setTorch(true);
        }
    }

    public void closeTorch() {
        if(cameraSource != null) {
            cameraSource.setTorch(false);
        }
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
            requirePermission(new CallBack() {
                @Override
                public void call() {
                    try {
                        if (preview == null) {
                            Log.d(TAG, "resume: Preview is null");
                        }
                        if (graphicOverlay == null) {
                            Log.d(TAG, "resume: graphOverlay is null");
                        }
                        preview.start(cameraSource, graphicOverlay);
                        cameraSource.setOnCameraListener(new CameraSource.OnCameraListener() {
                            @Override
                            public void open(Camera camera) {
                                new GestureDetectorUtil(preview, camera);
                            }
                        });
                    } catch (IOException e) {
                        Log.e(TAG, "Unable to start camera source.", e);
                        cameraSource.release();
                        cameraSource = null;
                    }
                }
            });
        }
    }

    private interface CallBack {
        void call();
    }

    private void requirePermission(CallBack callBack) {
        PermissionUtil.getInstance().with(activity).requestPermissions(new String[]{Manifest.permission.CAMERA,
                Manifest.permission.VIBRATE}, new PermissionUtil.PermissionListener() {
            @Override
            public void onGranted() {
                if(callBack != null) {
                    callBack.call();
                }
            }

            @Override
            public void onDenied(List<String> deniedPermission) {
                PermissionUtil.getInstance().showDialogTips(activity, deniedPermission, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        activity.finish();
                    }
                });
            }

            @Override
            public void onShouldShowRationale(List<String> deniedPermission) {
//                requirePermission(callBack);
            }
        });
    }
}
