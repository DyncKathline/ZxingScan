/*
 * Copyright 2020 Google LLC. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.kathline.barcode.barcodescanner;

import android.animation.ValueAnimator;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Region;
import android.os.Handler;
import android.os.Looper;
import android.view.MotionEvent;

import androidx.annotation.ColorInt;

import com.google.mlkit.vision.barcode.common.Barcode;
import com.kathline.barcode.GraphicOverlay;

import static java.lang.Math.max;
import static java.lang.Math.min;

/**
 * Graphic instance for rendering Barcode position and content information in an overlay view.
 * 仿微信扫码
 */
public class WxGraphic extends GraphicOverlay.Graphic {

    private static final int TEXT_COLOR = Color.BLACK;
    private static final int MARKER_COLOR = Color.WHITE;
    private static final float TEXT_SIZE = 54.0f;
    private static final float STROKE_WIDTH = 4.0f;

    private final Paint rectPaint;
    private final Paint barcodePaint;
    private final Barcode barcode;
    private final Paint labelPaint;
    private Paint paint;
    private float radius = 40f;
    private @ColorInt
    int color = Color.parseColor("#6200EE");
    Region circleRegion;
    Path circlePath;
    private Bitmap bitmap;
    private boolean isHideColor;

    public WxGraphic(GraphicOverlay overlay, Barcode barcode) {
        super(overlay);

        this.barcode = barcode;

        rectPaint = new Paint();
        rectPaint.setColor(MARKER_COLOR);
        rectPaint.setStyle(Paint.Style.STROKE);
        rectPaint.setStrokeWidth(STROKE_WIDTH);

        barcodePaint = new Paint();
        barcodePaint.setColor(TEXT_COLOR);
        barcodePaint.setTextSize(TEXT_SIZE);

        labelPaint = new Paint();
        labelPaint.setColor(MARKER_COLOR);
        labelPaint.setStyle(Paint.Style.FILL);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setStyle(Paint.Style.FILL);
        paint.setColor(color);
        paint.setStrokeWidth(6);

        circlePath = new Path();
        circleRegion = new Region();
    }

    public void setRadius(float radius) {
        this.radius = radius;
        postInvalidate();
    }

    public void setColor(@ColorInt int color) {
        this.color = color;
        paint.setColor(color);
        postInvalidate();
    }

    public void setBitmap(Bitmap bitmap) {
        setBitmap(bitmap, true);
    }

    /**
     * 当设置图片后，是否显示后面的圆
     * @param bitmap
     * @param isHideColor
     */
    public void setBitmap(Bitmap bitmap, boolean isHideColor) {
        this.bitmap = bitmap;
        this.isHideColor = isHideColor;
        postInvalidate();
    }

    /**
     * Draws the barcode block annotations for position, size, and raw value on the supplied canvas.
     */
    @Override
    public void draw(Canvas canvas) {
        if (barcode == null) {
            throw new IllegalStateException("Attempting to draw a null barcode.");
        }

        // Draws the bounding box around the BarcodeBlock.
        RectF rect = new RectF(barcode.getBoundingBox());
        // If the image is flipped, the left will be translated to right, and the right to left.
        float x0 = translateX(rect.left);
        float x1 = translateX(rect.right);
        rect.left = min(x0, x1);
        rect.right = max(x0, x1);
        rect.top = translateY(rect.top);
        rect.bottom = translateY(rect.bottom);

        circlePath.reset();
        // ▼在屏幕中间添加一个圆
        circlePath.addCircle(rect.left + (rect.right - rect.left) / 2f, rect.top + (rect.bottom - rect.top) / 2f, radius * mProgress, Path.Direction.CW);
        // ▼将剪裁边界设置为视图大小
        Region globalRegion = new Region((int) rect.left, (int) rect.top, (int) rect.right, (int) rect.bottom);
        // ▼将 Path 添加到 Region 中
        circleRegion.setPath(circlePath, globalRegion);
        Path circle = circlePath;
        if (bitmap != null) {
            if(isHideColor) {
                paint.setAlpha(0);
            }
            // 绘制圆
            canvas.drawPath(circle, paint);
            float x = rect.left + (rect.right - rect.left) / 2f;
            float y = rect.top + (rect.bottom - rect.top) / 2f;
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float scaleX = (radius) * mProgress / (width / 2f);
            float scaleY = (radius) * mProgress / (height / 2f);
            Matrix mMatrix = new Matrix();
            mMatrix.postScale(scaleX, scaleY, (width / 2f), (height / 2f));
            mMatrix.postTranslate(x - width / 2f, y - height / 2f);
            canvas.drawBitmap(bitmap, mMatrix, null);
        } else {
            // 绘制圆
            canvas.drawPath(circle, paint);
        }
        if (valueAnimator == null && !isDestroy) {
            startAnim();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                int x = (int) event.getX();
                int y = (int) event.getY();

                // ▼点击区域判断
                if (circleRegion.contains(x, y)) {
                    if (onClickListener != null) {
                        onClickListener.onClick(barcode);
                    }
                }
                break;
        }
        return true;
    }

    @Override
    public void OnDestroy() {
        super.OnDestroy();
        isDestroy = true;
        handler.removeCallbacks(resetRunnable);
        valueAnimator.cancel();
        valueAnimator = null;
    }

    public interface OnClickListener {
        void onClick(Barcode barcode);
    }

    private OnClickListener onClickListener;

    public void setOnClickListener(OnClickListener listener) {
        onClickListener = listener;
    }

    private float mProgress = 1;
    private ValueAnimator valueAnimator;
    private Handler handler = new Handler(Looper.getMainLooper());
    private Runnable resetRunnable;
    private boolean isDestroy;

    private void startAnim() {
        valueAnimator = ValueAnimator.ofFloat(1, 0.8f, 1, 0.8f, 1);
        valueAnimator.setDuration(2000);
        valueAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                mProgress = (float) animation.getAnimatedValue();
                if (animation.getCurrentPlayTime() >= animation.getDuration()) {
                    if (handler != null && !isDestroy) {
                        handler.postDelayed(resetRunnable = new Runnable() {
                            @Override
                            public void run() {
                                startAnim();
                            }
                        }, 1000);
                    }
                }
                postInvalidate();
            }
        });
        valueAnimator.start();
    }

}
