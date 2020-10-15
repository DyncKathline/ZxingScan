package org.dync.zxinglibrary.zxing;

import android.app.Activity;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils;
import android.util.Log;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.view.View;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.Result;
import com.google.zxing.common.HybridBinarizer;

import org.dync.zxinglibrary.R;
import org.dync.zxinglibrary.zxing.camera.CameraManager;
import org.dync.zxinglibrary.zxing.decode.DecodeThread;
import org.dync.zxinglibrary.zxing.decode.PhotoScanHandler;
import org.dync.zxinglibrary.zxing.decode.RGBLuminanceSource;
import org.dync.zxinglibrary.zxing.utils.BeepManager;
import org.dync.zxinglibrary.zxing.utils.BitmapUtil;
import org.dync.zxinglibrary.zxing.utils.CaptureActivityHandler;
import org.dync.zxinglibrary.zxing.utils.InactivityTimer;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

public class ScanManager {

	boolean isHasSurface = false;
    CameraManager cameraManager;
    //用于拍摄扫描的handler
    CaptureActivityHandler handler;
    //用于照片扫描的handler,不可共用，图片扫描是不需要摄像机的
    PhotoScanHandler photoScanHandler;
    InactivityTimer inactivityTimer;
    public BeepManager beepManager;
    SurfaceView surfaceView = null;
    View scanContainer;
    final String TAG = ScanManager.class.getSimpleName();
    Activity activity;
    ScanListener listener;
    boolean isOpenLight = false;
    boolean playBeep = true;
    boolean vibrate = true;
    int orientation = -1;

    private int scanMode;//扫描模型（条形，二维码，全部）

    /**
     * 用于启动照相机扫描二维码，在activity的onCreate里面构造出来
     * 在activity的生命周期中调用此类相对应的生命周期方法
     *
     * @param activity      扫描的activity
     * @param surfaceView   预览的SurfaceView
     * @param scanContainer 扫描的布局，全屏布局
     */
    public ScanManager(Activity activity, SurfaceView surfaceView, View scanContainer, int scanMode, ScanListener listener) {
        this.activity = activity;
        this.surfaceView = surfaceView;
        this.scanContainer = scanContainer;
        this.listener = listener;
        this.scanMode = scanMode;
    }

    public void setPlayBeepAndVibrate(boolean playBeep, boolean vibrate) {
        this.playBeep = playBeep;
        this.vibrate = vibrate;
    }

    public void setConfiguration(int orientation) {
        this.orientation = orientation;
    }

    public void onResume() {
        // CameraManager must be initialized here, not in onCreate(). This is
        // necessary because we don't
        // want to open the camera driver and measure the screen size if we're
        // going to show the help on
        // first launch. That led to bugs where the scanning rectangle was the
        // wrong size and partially
        // off screen.
        inactivityTimer = new InactivityTimer(activity);
        beepManager = new BeepManager(activity);
        cameraManager = new CameraManager(activity);

        handler = null;
		surfaceView.getHolder().addCallback(new SurfaceHolder.Callback() {
			@Override
			public void surfaceCreated(SurfaceHolder holder) {
				if (holder == null) {
					Log.e(TAG, "*** WARNING *** surfaceCreated() gave us a null surface!");
				}
				if (!isHasSurface) {
					isHasSurface = true;
					initCamera();
				}
			}

			@Override
			public void surfaceChanged(SurfaceHolder holder, int format, int width, int height) {

			}

			@Override
			public void surfaceDestroyed(SurfaceHolder holder) {
				isHasSurface = false;
			}
		});

        inactivityTimer.onResume();
    }

    public void onPause() {
        if (handler != null) {
            handler.quitSynchronously();
            handler = null;
        }
        inactivityTimer.onPause();
        beepManager.close();
        cameraManager.closeDriver();
    }

    public void onDestroy() {
        inactivityTimer.shutdown();
    }

    void initCamera() {
        if (surfaceView == null) {
            throw new IllegalStateException("No SurfaceHolder provided");
        }
        if (cameraManager.isOpen()) {
            Log.w(TAG, "initCamera() while already open -- late SurfaceView callback?");
            return;
        }
        try {
            cameraManager.setConfiguration(orientation);
            cameraManager.openDriver(surfaceView);
            // Creating the handler starts the preview, which can also throw a
            // RuntimeException.
            if (handler == null) {
                handler = new CaptureActivityHandler(this, cameraManager, scanMode);
                Log.e(TAG, "handler new成功！:" + handler);
            }
        } catch (IOException ioe) {
            ioe.printStackTrace();
            listener.scanError(new Exception("相机打开出错，请检查是否被禁止了该权限！"));
        } catch (RuntimeException e) {
            e.printStackTrace();
            listener.scanError(new Exception("相机打开出错，请检查是否被禁止了该权限！"));
        }
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
        if (cameraManager.hasLight()) {
            if (isOpenLight) {
                cameraManager.offLight();
            } else {
                cameraManager.openLight();
            }
            isOpenLight = !isOpenLight;
        }
    }

    public Handler getHandler() {
        return handler;
    }

    public CameraManager getCameraManager() {
        return cameraManager;
    }

    public Activity getActivity() {
        return activity;
    }

    /**
     * 扫描成功的结果回调
     *
     * @param rawResult
     * @param bundle
     */
    public void handleDecode(Result rawResult, Bundle bundle) {
        inactivityTimer.onActivity();
        //扫描成功播放声音滴一下，可根据需要自行确定什么时候播
        beepManager.playBeepSoundAndVibrate(playBeep, vibrate);
        bundle.putInt("width", cameraManager.getFramingRect().width());
        bundle.putInt("height", cameraManager.getFramingRect().height());
        bundle.putString("result", rawResult.getText());
        listener.scanResult(rawResult, bundle);
    }

    public void handleDecodeError(Exception e) {
        listener.scanError(e);
    }

    int getStatusBarHeight() {
        try {
            Class<?> c = Class.forName("com.android.internal.R$dimen");
            Object obj = c.newInstance();
            Field field = c.getField("status_bar_height");
            int x = Integer.parseInt(field.get(obj).toString());
            return activity.getResources().getDimensionPixelSize(x);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return 0;
    }

    /**
     * 用于扫描本地图片二维码或者一维码
     *
     * @param photo_path2 本地图片的所在位置
     * @return
     */
    public void scanningImage(final String photo_path2) {
        if (TextUtils.isEmpty(photo_path2)) {
            listener.scanError(new Exception("photo url is null!"));
        }
        photoScanHandler = new PhotoScanHandler(this);
        new Thread(new Runnable() {

            @Override
            public void run() {
                //获取初始化的设置器
                Map<DecodeHintType, Object> hints = DecodeThread.getHints();
                hints.put(DecodeHintType.CHARACTER_SET, "utf-8");

//				Hashtable<DecodeHintType, String> hints = new Hashtable<DecodeHintType, String>();

                Bitmap bitmap = BitmapUtil.decodeBitmapFromPath(photo_path2, 600, 600, false);
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
                    bitmap = BitmapUtil.decodeBitmapFromPath(photo_path2, 600, 600, true);
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

    /**
     * 扫描一次后，如需再次扫描，请调用这个方法
     */
    public void reScan() {
        if (handler != null) {
            handler.sendEmptyMessage(R.id.restart_preview);
        }
    }

    public boolean isScanning() {
        if (handler != null) {
            return handler.isScanning();
        }
        return false;
    }

}
