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

package org.dync.zxinglibrary.view;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.Shader;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;

import org.dync.zxinglibrary.R;
import org.dync.zxinglibrary.camera.CameraManager;

/**
 * @Description: java类作用描述
 * @Author: BayMax·Yi
 * @CreateDate: 2019/6/18
 * @UpdateUser: 更新者
 * @UpdateDate: 2019/6/18
 * @UpdateRemarkateUser: @Version: 1.0
 */
public final class ViewfinderView extends View {

    private static final int ANIMATOR_DURATION = 2000;
    private static final int DEFAULT_STROKWIDTH = 10;
    private static final int DEFAULT_CORNORLENGTH = 50;
    private final int[] DEFAULT_LINECOLORS = {
            Color.argb(10, 0, 255, 0),
            Color.argb(30, 0, 255, 0),
            Color.argb(50, 0, 255, 0),
            Color.argb(70, 0, 255, 0),
            Color.argb(90, 0, 255, 0),
            Color.argb(70, 0, 255, 0),
            Color.argb(50, 0, 255, 0),
            Color.argb(30, 0, 255, 0),
            Color.argb(10, 0, 255, 0),
            Color.argb(5, 0, 255, 0),
    };

    private final float[] DEFAULT_LINEPERCENTS = {0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f};

    private final int DEFAULT_CONOR_COLOR = Color.YELLOW;
    private int cornorStrokeWidth = DEFAULT_STROKWIDTH;
    private int cornorLength = DEFAULT_CORNORLENGTH;
    private int animatorDuretion = ANIMATOR_DURATION;
    private int conorColor = DEFAULT_CONOR_COLOR;
    private int[] lineColors = DEFAULT_LINECOLORS;
    private float[] linePercents = DEFAULT_LINEPERCENTS;

    private Paint cornorPaint;
    private Paint lineScanePaint;
    private Path path;
    private final Paint paint;

    private CameraManager cameraManager;
    private boolean animatorStarted;
    private int lineY;
    private final int maskColor;
    private ValueAnimator animator;
    private Shader mShader;
    private boolean reDrawAnim;
    private Bitmap resultBitmap;

    // This constructor is used when the class is built from an XML resource.
    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        // Initialize these once for performance rather than calling them every time in onDraw().
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask);

        path = new Path();
        initDefaultSize();
        initCornorPaint();
        initScannerLinePaint();
    }

    private void initDefaultSize() {
        int screenWidth = getContext().getResources().getDisplayMetrics().widthPixels;
        int screenHight = getContext().getResources().getDisplayMetrics().heightPixels;
        Bundle metaData = getContext().getApplicationInfo().metaData;
        double scale = 1;

        if (metaData != null) {
            double scaleX = metaData.getInt("screen_width") * 1.0 / screenWidth;
            double scaleY = metaData.getInt("screen_height") * 1.0 / screenHight;
            scale = Math.min(scaleX, scaleY);
        }
        cornorStrokeWidth *= scale;
        cornorLength *= scale;
    }

    private void initScannerLinePaint() {
        lineScanePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        lineScanePaint.setStrokeWidth(cornorStrokeWidth);
        lineScanePaint.setStyle(Paint.Style.STROKE);
    }

    private void initCornorPaint() {
        cornorPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cornorPaint.setStrokeWidth(cornorStrokeWidth);
        cornorPaint.setColor(conorColor);
        cornorPaint.setStyle(Paint.Style.STROKE);
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @SuppressLint("DrawAllocation")
    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = cameraManager.getFramingRect();
        Rect previewFrame = cameraManager.getFramingRectInPreview();
        if (frame == null || previewFrame == null) {
            return;
        }
        int width = canvas.getWidth();
        int height = canvas.getHeight();
        if (resultBitmap != null) { // 绘制扫描结果的图
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(0xA0);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            // Draw the exterior (i.e. outside the framing rect) darkened
            paint.setColor(maskColor);
            canvas.drawRect(0, 0, width, frame.top, paint);
            canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
            canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1, paint);
            canvas.drawRect(0, frame.bottom + 1, width, height, paint);
            drawCornor(canvas, frame);
            // drawLine(canvas, frame);
            int sx = frame.left;
            int ex = frame.right;
            if (!animatorStarted) {
                animatorStarted = true;
                startAnimator(frame.top + cornorStrokeWidth / 2, frame.bottom - cornorStrokeWidth / 2, animatorDuretion);
            }

            drawLine(canvas, sx, lineY, ex, lineY);
        }
    }

    private void startAnimator(int startY, int endY, int duration) {
        animator = ValueAnimator.ofInt(startY, endY);
        animator.setDuration(duration);
        animator.setRepeatMode(ValueAnimator.REVERSE);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setInterpolator(new AccelerateDecelerateInterpolator());
        animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animation) {
                lineY = ((int) animation.getAnimatedValue());
                postInvalidate();
            }
        });
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationStart(Animator animation) {
                super.onAnimationStart(animation);
                if (reDrawAnim) reDrawAnim = false;
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                super.onAnimationCancel(animation);
            }

            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                if (reDrawAnim)
                    postInvalidate();
            }
        });
        animator.start();
    }

    private void drawCornor(Canvas canvas, Rect frame) {
        path.reset();
        canvas.save();
        cornorStrokeWidth /= 2;
        int framWidth = frame.right - frame.left;
        int framHeight = frame.bottom - frame.top;
        canvas.translate(frame.left + framWidth / 2, frame.top + framHeight / 2);
        path.moveTo(-framWidth / 2 - cornorStrokeWidth, -framHeight / 2 + cornorLength);
        path.lineTo(-framWidth / 2 - cornorStrokeWidth, -framHeight / 2 - cornorStrokeWidth);
        path.lineTo(-framWidth / 2 + cornorLength, -framHeight / 2 - cornorStrokeWidth);


        path.moveTo(framWidth / 2 + cornorStrokeWidth, -framHeight / 2 + cornorLength);
        path.lineTo(framWidth / 2 + cornorStrokeWidth, -framHeight / 2 - cornorStrokeWidth);
        path.lineTo(framWidth / 2 - cornorLength, -framHeight / 2 - cornorStrokeWidth);


        path.moveTo(framWidth / 2 + cornorStrokeWidth, framHeight / 2 - cornorLength);
        path.lineTo(framWidth / 2 + cornorStrokeWidth, framHeight / 2 + cornorStrokeWidth);
        path.lineTo(framWidth / 2 - cornorLength, framHeight / 2 + cornorStrokeWidth);


        path.moveTo(-framWidth / 2 - cornorStrokeWidth, framHeight / 2 - cornorLength);
        path.lineTo(-framWidth / 2 - cornorStrokeWidth, framHeight / 2 + cornorStrokeWidth);
        path.lineTo(-framWidth / 2 + cornorLength, framHeight / 2 + cornorStrokeWidth);

        canvas.drawPath(path, cornorPaint);
        path.reset();
        cornorStrokeWidth *= 2;
        canvas.restore();
    }

    private void drawLine(Canvas canvas, int sx, int sy, int ex, int ey) {


        mShader = new LinearGradient(sx, sy, ex, ey, lineColors, linePercents, Shader.TileMode.CLAMP);

        lineScanePaint.setShader(mShader);
        path.reset();
        path.moveTo(sx, sy);
        path.lineTo(ex, ey);
        //path.addRect();
        canvas.drawPath(path, lineScanePaint);
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
    }

    public void setCornorStrokeWidth(int cornorStrokeWidth) {

        if (this.cornorStrokeWidth == cornorStrokeWidth) return;
        this.cornorStrokeWidth = cornorStrokeWidth;
        postInvalidate();
    }

    public void setCornorLength(int cornorLength) {

        if (this.cornorLength == cornorLength) return;
        this.cornorLength = cornorLength;
        postInvalidate();

    }

    public void setAnimatorDuration(int animatorDuretion) {
        if (this.animatorDuretion == animatorDuretion) return;
        this.animatorDuretion = animatorDuretion;
        if (animator != null && animator.isRunning()) {
            animator.end();
            reDrawAnim = true;
        }
    }

    public void setConorColor(int conorColor) {
        if (this.conorColor == conorColor) return;
        this.conorColor = conorColor;
        cornorPaint.setColor(this.conorColor);
        postInvalidate();
    }

    public void setLineColors(int[] lineColors) {
        if (this.lineColors == lineColors) return;
        this.lineColors = lineColors;
        postInvalidate();
    }

    public void setLinePercents(float[] linePercents) {

        if (this.linePercents == linePercents) return;
        this.linePercents = linePercents;
        postInvalidate();
    }

    public void setAnimator(ValueAnimator animator) {
        this.animator = animator;
        postInvalidate();
    }

    public void destoryAnimator() {
        if (animator != null && animator.isRunning()) animator.end();
    }

    /**
     * Draw a bitmap with the result points highlighted instead of the live
     * scanning display.
     *
     * @param barcode An image of the decoded barcode.
     */
    public void drawResultBitmap(Bitmap barcode) {
        resultBitmap = barcode;
        invalidate();
    }
}
