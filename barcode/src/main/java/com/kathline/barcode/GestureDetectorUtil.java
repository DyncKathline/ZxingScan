package com.kathline.barcode;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.hardware.Camera;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;

import java.util.ArrayList;
import java.util.List;

public class GestureDetectorUtil {

    private final String TAG = "GestureDetectorUtil";
    private View surfaceView;
    private float oldDist = 1f;

    @SuppressLint("ClickableViewAccessibility")
    public GestureDetectorUtil(View surfaceView, final Camera camera) {
        this.surfaceView = surfaceView;
        surfaceView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getPointerCount() == 1) {
//                    handleFocusMetering(event, camera);
                } else {
                    switch (event.getAction() & MotionEvent.ACTION_MASK) {
                        case MotionEvent.ACTION_POINTER_DOWN:
                            oldDist = getFingerSpacing(event);
                            break;
                        case MotionEvent.ACTION_MOVE:
                            float newDist = getFingerSpacing(event);
                            if (newDist > oldDist) {
//                                Log.e(TAG, "进入放大手势");
                                handleZoom(true, camera);
                            } else if (newDist < oldDist) {
//                                Log.e(TAG, "进入缩小手势");
                                handleZoom(false, camera);
                            }
                            oldDist = newDist;
                            break;
                    }
                }
                return true;
            }
        });
    }

    private void handleZoom(boolean isZoomIn, Camera camera) {
//        Log.e(TAG, "进入缩小放大方法");
        Camera.Parameters params = camera.getParameters();
        if (params.isZoomSupported()) {
            int maxZoom = params.getMaxZoom();
            int zoom = params.getZoom();
            if (isZoomIn && zoom < maxZoom) {
//                Log.e(TAG, "进入放大方法zoom=" + zoom);
                zoom++;
            } else if (zoom > 0) {
//                Log.e(TAG, "进入缩小方法zoom=" + zoom);
                zoom--;
            }
            params.setZoom(zoom);
            camera.setParameters(params);
        } else {
            Log.i(TAG, "zoom not supported");
        }
    }

    private void handleFocusMetering(MotionEvent event, Camera camera) {
        Log.e("Camera", "进入handleFocusMetering");
        Camera.Parameters params = camera.getParameters();
        Camera.Size previewSize = params.getPreviewSize();
        Rect focusRect = calculateTapArea(event.getX(), event.getY(), 1f, previewSize);
        Rect meteringRect = calculateTapArea(event.getX(), event.getY(), 1.5f, previewSize);
        final String currentFocusMode = params.getFocusMode();
        if (params
                .getSupportedFocusModes()
                .contains(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO)) {
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_VIDEO);
        } else {
            Log.i(TAG, "Camera auto focus is not supported on this device.");
            params.setFocusMode(Camera.Parameters.FOCUS_MODE_AUTO); // 设置对焦模式
            if (params.getMaxNumFocusAreas() > 0) {
                List<Camera.Area> focusAreas = new ArrayList<>();
                focusAreas.add(new Camera.Area(focusRect, 800));
                params.setFocusAreas(focusAreas);
            } else {
                Log.i(TAG, "focus areas not supported");
            }
            if (params.getMaxNumMeteringAreas() > 0) {
                List<Camera.Area> meteringAreas = new ArrayList<>();
                meteringAreas.add(new Camera.Area(meteringRect, 800));
                params.setMeteringAreas(meteringAreas);
            } else {
                Log.i(TAG, "metering areas not supported");
            }
        }
        try {
            camera.cancelAutoFocus();// 每次对焦前，需要先取消对焦
            //本人使用的小米手机在设置聚焦区域的时候经常会出异常，看日志发现是框架层的字符串转int的时候出错了，
            //目测是小米修改了框架层代码导致，在此try掉，对实际聚焦效果没影响
            camera.setParameters(params);
            camera.autoFocus(new Camera.AutoFocusCallback() {
                @Override
                public void onAutoFocus(boolean success, Camera camera) {
                    Camera.Parameters params = camera.getParameters();
                    params.setFocusMode(currentFocusMode);
                    camera.setParameters(params);
                }
            });
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static float getFingerSpacing(MotionEvent event) {
        float x = event.getX(0) - event.getX(1);
        float y = event.getY(0) - event.getY(1);
        Log.e("Camera", "getFingerSpacing ，计算距离 = " + (float) Math.sqrt(x * x + y * y));
        return (float) Math.sqrt(x * x + y * y);
    }

    private static Rect calculateTapArea(float x, float y, float coefficient, Camera.Size previewSize) {
        float focusAreaSize = 300;
        int areaSize = Float.valueOf(focusAreaSize * coefficient).intValue();
        int centerX = (int) (x / previewSize.width - 1000);
        int centerY = (int) (y / previewSize.height - 1000);
        int left = clamp(centerX - areaSize / 2, -1000, 1000);
        int top = clamp(centerY - areaSize / 2, -1000, 1000);
        RectF rectF = new RectF(left, top, left + areaSize, top + areaSize);
        return new Rect(Math.round(rectF.left), Math.round(rectF.top), Math.round(rectF.right), Math.round(rectF.bottom));
    }

    private static int clamp(int x, int min, int max) {
        if (x > max) {
            return max;
        }
        if (x < min) {
            return min;
        }
        return x;
    }

}
