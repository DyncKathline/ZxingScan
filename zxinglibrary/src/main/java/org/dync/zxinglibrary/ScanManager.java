/*
 * Copyright (C) 2008 ZXing authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.dync.zxinglibrary;

import android.Manifest;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.os.Handler;
import android.os.Message;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.Window;
import android.view.WindowManager;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.ResultPoint;
import com.google.zxing.common.HybridBinarizer;

import org.dync.zxinglibrary.callback.ScanListener;
import org.dync.zxinglibrary.camera.CameraManager;
import org.dync.zxinglibrary.decod.CaptureActivityHandler;
import org.dync.zxinglibrary.decod.DecodeThread;
import org.dync.zxinglibrary.decod.PhotoScanHandler;
import org.dync.zxinglibrary.decod.RGBLuminanceSource;
import org.dync.zxinglibrary.hardware.AmbientLightManager;
import org.dync.zxinglibrary.hardware.BeepManager;
import org.dync.zxinglibrary.utils.BitmapUtils;
import org.dync.zxinglibrary.utils.GestureDetectorUtil;
import org.dync.zxinglibrary.utils.InactivityTimer;
import org.dync.zxinglibrary.utils.PermissionUtil;
import org.dync.zxinglibrary.utils.PreferencesActivity;
import org.dync.zxinglibrary.view.ViewfinderView;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * This activity opens the camera and does the actual scanning on a background thread. It draws a
 * viewfinder to help the user place the barcode correctly, shows feedback as the image processing
 * is happening, and then overlays the results when a scan is successful.
 *
 * @author dswitkin@google.com (Daniel Switkin)
 * @author Sean Owen
 */
public class ScanManager {

    private static final String TAG = ScanManager.class.getSimpleName();

    boolean isOpenLight = false;
    boolean playBeep = true;
    boolean vibrate = true;

    private CameraManager cameraManager;
    private CaptureActivityHandler handler;
    private ViewfinderView viewfinderView;
    private boolean hasSurface;

    private FragmentActivity activity;


    private InactivityTimer inactivityTimer;
    private BeepManager beepManager;
    private AmbientLightManager ambientLightManager;
    private SurfaceView surfaceView;
    private SurfaceHolder.Callback callback;
    private int scanMode;
    private boolean isCheckPermission = false;

    public ScanListener scanListener;
    public final static int SCANTYPE_QR = 1;
    public final static int SCANTYPE_BARCODE = 2;
    public final static int SCANTYPE_ALL = 3;

    public ScanManager() {}

    public ScanManager(FragmentActivity activity, SurfaceView surfaceView, ViewfinderView viewfinderView, int scanMode, ScanListener listener) {
        this.activity = activity;
        this.surfaceView = surfaceView;
        this.viewfinderView = viewfinderView;
        this.scanMode = scanMode;
        this.scanListener = listener;
        setScanType(scanMode);
        onCreate();
    }

    public void onCreate() {
        Window window = activity.getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        hasSurface = false;
        if(inactivityTimer == null) {
            inactivityTimer = new InactivityTimer(activity);
        }
        if(beepManager == null) {
            beepManager = new BeepManager(activity);
        }
        if(ambientLightManager == null) {
            ambientLightManager = new AmbientLightManager(activity);
        }
    }

    public void onResume() {
        cameraManager = new CameraManager(activity.getApplication());
        viewfinderView.setCameraManager(cameraManager);
        handler = null;
        beepManager.updatePrefs();
        ambientLightManager.start(cameraManager);

        inactivityTimer.onResume();
        if (hasSurface) {
            initCamera(surfaceView.getHolder());
        } else {
            callback = new SurfaceHolder.Callback() {

                @Override
                public void surfaceCreated(SurfaceHolder holder) {
                    if (holder == null) {
                        Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
                    }
                    if (!hasSurface) {
                        hasSurface = true;
                        requirePermission();
                    }
                }

                @Override
                public void surfaceDestroyed(SurfaceHolder holder) {
                    hasSurface = false;
                }

                @Override
                public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

                }
            };
            surfaceView.getHolder().addCallback(callback);
        }
    }

    private void requirePermission() {
        PermissionUtil.getInstance().with(activity).requestPermissions(new String[]{Manifest.permission.CAMERA,
                Manifest.permission.VIBRATE}, new PermissionUtil.PermissionListener() {
            @Override
            public void onGranted() {
                isCheckPermission = true;
                initCamera(surfaceView.getHolder());
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

    public void onPause() {
        if(isCheckPermission) {
            if (handler != null) {
                handler.quitSynchronously();
                handler = null;
            }
            cameraManager.closeDriver();
        }
        inactivityTimer.onPause();
        ambientLightManager.stop();
        beepManager.close();
        if (!hasSurface) {
            if (surfaceView != null) surfaceView.getHolder().removeCallback(callback);
        }
    }

    public void onDestroy() {
        inactivityTimer.shutdown();
        if (viewfinderView != null) {
            viewfinderView.destoryAnimator();
        }
    }

    public void handleDecode(Result rawResult, Bitmap barcode, float scaleFactor) {
        inactivityTimer.onActivity();
        //扫描成功播放声音滴一下，可根据需要自行确定什么时候播
        beepManager.playBeepSoundAndVibrate(playBeep, vibrate);

        boolean fromLiveScan = barcode != null;
        if (fromLiveScan) {
            // Then not from history, so beep/vibrate and we have an image to draw on
            beepManager.playBeepSoundAndVibrate();
            drawResultPoints(barcode, scaleFactor, rawResult);
        }
        scanListener.scanResult(rawResult, barcode);
    }

    public void handleDecodeError(Exception e) {
        scanListener.scanError(e);
    }

    /**
     * Superimpose a line for 1D or dots for 2D to highlight the key features of the barcode.
     *
     * @param barcode   A bitmap of the captured image.
     * @param scaleFactor amount by which thumbnail was scaled
     * @param rawResult The decoded results which contains the points to draw.
     */
    private void drawResultPoints(Bitmap barcode, float scaleFactor, Result rawResult) {
        ResultPoint[] points = rawResult.getResultPoints();
        if (points != null && points.length > 0) {
            Canvas canvas = new Canvas(barcode);
            Paint paint = new Paint();
            paint.setColor(activity.getResources().getColor(R.color.result_points));
            if (points.length == 2) {
                paint.setStrokeWidth(4.0f);
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
            } else if (points.length == 4 &&
                    (rawResult.getBarcodeFormat() == BarcodeFormat.UPC_A ||
                            rawResult.getBarcodeFormat() == BarcodeFormat.EAN_13)) {
                // Hacky special case -- draw two lines, for the barcode and metadata
                drawLine(canvas, paint, points[0], points[1], scaleFactor);
                drawLine(canvas, paint, points[2], points[3], scaleFactor);
            } else {
                paint.setStrokeWidth(10.0f);
                for (ResultPoint point : points) {
                    if (point != null) {
                        canvas.drawPoint(scaleFactor * point.getX(), scaleFactor * point.getY(), paint);
                    }
                }
            }
        }
    }

    private static void drawLine(Canvas canvas, Paint paint, ResultPoint a, ResultPoint b, float scaleFactor) {
        if (a != null && b != null) {
            canvas.drawLine(scaleFactor * a.getX(),
                    scaleFactor * a.getY(),
                    scaleFactor * b.getX(),
                    scaleFactor * b.getY(),
                    paint);
        }
    }

    private synchronized void initCamera(SurfaceHolder surfaceHolder) {
        if (surfaceHolder == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.openDriver(surfaceHolder);
            new GestureDetectorUtil(surfaceView, cameraManager.getCamera().getCamera());
            // Creating the handler starts the preview, which can also throw a RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, null, null, null, cameraManager);
            }
            //解决授权camera权限扫描不出现问题
//            restartPreviewAfterDelay(0);
            drawViewfinder();
        } catch (IOException ioe) {
            Log.w(TAG, ioe);
            scanListener.scanError(ioe);
        } catch (RuntimeException e) {
            // Barcode Scanner has seen crashes in the wild of this variety:
            // java.?lang.?RuntimeException: Fail to connect to camera service
            Log.w(TAG, "Unexpected error initializing camera", e);
            scanListener.scanError(e);
        }
    }

    public ViewfinderView getViewfinderView() {
        return viewfinderView;
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public void beepAndVibrate() {
        beepManager.playBeepSoundAndVibrate();
    }

    public boolean hasLight() {
        return activity.getPackageManager().hasSystemFeature(PackageManager.FEATURE_CAMERA_FLASH);
    }

    public void switchCamera() {
        if (cameraManager != null) {
            cameraManager.switchCamera();
        }
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
        cameraManager.setTorch(true);
    }

    public void closeTorch() {
        cameraManager.setTorch(false);
    }

    public final void drawViewfinder() {
        viewfinderView.drawViewfinder();
    }

    public void setViewfinderView(ViewfinderView viewfinderView) {
        this.viewfinderView = viewfinderView;
    }

    public void setActivity(FragmentActivity activity) {
        this.activity = activity;
    }

    public void setSurfaceHolder(SurfaceView surfaceView) {
        this.surfaceView = surfaceView;
    }

    public void setScanListener(ScanListener scanListener) {
        this.scanListener = scanListener;
    }

    public void setPlayBeepAndVibrate(boolean playBeep, boolean vibrate) {
        this.playBeep = playBeep;
        this.vibrate = vibrate;
    }

    public Activity getActivity() {
        return activity;
    }

    public void setScanType(int scanType) {
        this.scanMode = scanType;
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(activity);
        switch (scanType) {
            case SCANTYPE_QR:
                prefs.edit().putBoolean(PreferencesActivity.KEY_DECODE_1D_PRODUCT, false).commit();
                prefs.edit().putBoolean(PreferencesActivity.KEY_DECODE_1D_INDUSTRIAL, false).commit();
                prefs.edit().putBoolean(PreferencesActivity.KEY_DECODE_QR, true).commit();
                break;
            case SCANTYPE_BARCODE:
                prefs.edit().putBoolean(PreferencesActivity.KEY_DECODE_1D_PRODUCT, true).commit();
                prefs.edit().putBoolean(PreferencesActivity.KEY_DECODE_1D_INDUSTRIAL, true).commit();
                prefs.edit().putBoolean(PreferencesActivity.KEY_DECODE_QR, false).commit();
                break;
            case SCANTYPE_ALL:
            default:
                prefs.edit().putBoolean(PreferencesActivity.KEY_DECODE_1D_PRODUCT, true).commit();
                prefs.edit().putBoolean(PreferencesActivity.KEY_DECODE_1D_INDUSTRIAL, true).commit();
                prefs.edit().putBoolean(PreferencesActivity.KEY_DECODE_QR, true).commit();
        }
    }
    /**
     * 扫描一次后，如需再次扫描，请调用这个方法
     */
    public void restartPreviewAfterDelay(long delayMS) {
        if (handler != null) {
            handler.sendEmptyMessageDelayed(R.id.restart_preview, delayMS);
        }
    }

    public boolean isScanning() {
        if (handler != null) {
            return handler.isScanning();
        }
        return false;
    }

    /**
     * 用于扫描本地图片二维码或者一维码
     *
     * @param photo_path2 本地图片的所在位置
     * @return
     */
    public void scanningImage(final String photo_path2) {
        if (TextUtils.isEmpty(photo_path2)) {
            scanListener.scanError(new Exception("photo url is null!"));
        }
        final PhotoScanHandler photoScanHandler = new PhotoScanHandler(this);
        new Thread(new Runnable() {

            @Override
            public void run() {
                //获取初始化的设置器
                Map<DecodeHintType, Object> hints = DecodeThread.getHints();
                hints.put(DecodeHintType.CHARACTER_SET, "utf-8");

//				Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();

                Bitmap bitmap = BitmapUtils.decodeBitmapFromPath(photo_path2, 600, 600, false);
                Log.i(TAG, "run: bitmap w: " + bitmap.getWidth() + ", h: " + bitmap.getHeight());
                RGBLuminanceSource source = new RGBLuminanceSource(bitmap);
                BinaryBitmap bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
                MultiFormatReader multiFormatReader = new MultiFormatReader();
                try {
                    Message msg = Message.obtain();
                    msg.what = PhotoScanHandler.PHOTODECODEOK;
                    msg.obj = multiFormatReader.decode(bitmap1, hints);
                    photoScanHandler.sendMessage(msg);
                } catch (Exception e) {
                    bitmap = BitmapUtils.decodeBitmapFromPath(photo_path2, 600, 600, true);
                    Log.i(TAG, "run: bitmap w: " + bitmap.getWidth() + ", h: " + bitmap.getHeight());
                    source = new RGBLuminanceSource(bitmap);
                    bitmap1 = new BinaryBitmap(new HybridBinarizer(source));
                    multiFormatReader = new MultiFormatReader();
                    try {
                        Message msg = Message.obtain();
                        msg.what = PhotoScanHandler.PHOTODECODEOK;
                        msg.obj = multiFormatReader.decode(bitmap1, hints);
                        photoScanHandler.sendMessage(msg);
                    } catch (Exception e1) {
                        Message msg = Message.obtain();
                        msg.what = PhotoScanHandler.PHOTODECODEERROR;
                        msg.obj = new Exception("图片有误，或者图片模糊！");
                        photoScanHandler.sendMessage(msg);
                    }
                }
            }
        }).start();
    }
}
