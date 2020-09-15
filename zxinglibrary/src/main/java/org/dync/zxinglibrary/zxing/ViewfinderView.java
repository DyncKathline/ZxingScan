package org.dync.zxinglibrary.zxing;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.view.View;

import com.google.zxing.ResultPoint;

import org.dync.zxinglibrary.R;
import org.dync.zxinglibrary.zxing.camera.CameraManager;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * 该视图是覆盖在相机的预览视图之上的一层视图。扫描区构成原理，其实是在预览视图上画四块遮罩层，
 * 中间留下的部分保持透明，并画上一条激光线，实际上该线条就是展示而已，与扫描功能没有任何关系。
 */
public final class ViewfinderView extends View {

    /**
     * 刷新界面的时间
     */
    private static final long ANIMATION_DELAY = 10L;
    private static final int OPAQUE = 0xFF;

    private int CORNER_PADDING;

    /**
     * 扫描框中的中间线的宽度
     */
    private static int MIDDLE_LINE_WIDTH;

    /**
     * 扫描框中的中间线的与扫描框左右的间隙
     */
    private static int MIDDLE_LINE_PADDING;

    /**
     * 中间那条线每次刷新移动的距离
     */
    private static final int SPEEN_DISTANCE = 3;

    /**
     * 四个绿色边角对应的长度
     */
    private int cornerLineH;

    /**
     * 四个绿色边角对应的宽度
     */
    private int cornerLineW;

    /**
     * 画笔对象的引用
     */
    private Paint paint;
    private int mColor;

    /**
     * 中间滑动线的最顶端位置
     */
    private int slideTop;

    /**
     * 中间滑动线的最底端位置
     */
    private int slideBottom;

    private static final int MAX_RESULT_POINTS = 20;

    private Bitmap resultBitmap;
    private Bitmap lineBitmap;

    /**
     * 遮掩层的颜色
     */
    private final int maskColor;
    private final int resultColor;

    private final int resultPointColor;
    private List<ResultPoint> possibleResultPoints;

    private List<ResultPoint> lastPossibleResultPoints;

    /**
     * 第一次绘制控件
     */
    boolean isFirst = true;

    private CameraManager cameraManager;

    public ViewfinderView(Context context, AttributeSet attrs) {
        super(context, attrs);

        //将像素转换成dp
        cornerLineH = dip2px(context, 2);
        cornerLineW = dip2px(context, 14);

        CORNER_PADDING = dip2px(context, 0.0F);
        MIDDLE_LINE_PADDING = dip2px(context, 5.0F);
        MIDDLE_LINE_WIDTH = dip2px(context, 2.0F);

        paint = new Paint(Paint.ANTI_ALIAS_FLAG); // 开启反锯齿

        Resources resources = getResources();
        maskColor = resources.getColor(R.color.viewfinder_mask); // 遮掩层颜色
        resultColor = resources.getColor(R.color.result_view);

        resultPointColor = resources.getColor(R.color.possible_result_points);
        possibleResultPoints = new ArrayList<ResultPoint>(5);
        lastPossibleResultPoints = null;

        lineBitmap = ((BitmapDrawable) getResources()
                .getDrawable(R.mipmap.qrcode_default_scan_line)).getBitmap();
    }

    public void setCameraManager(CameraManager cameraManager) {
        this.cameraManager = cameraManager;
    }

    @Override
    public void onDraw(Canvas canvas) {
        if (cameraManager == null) {
            return; // not ready yet, early draw before done configuring
        }
        Rect frame = cameraManager.getFramingRect();
        if (frame == null) {
            return;
        }

        // 绘制遮掩层
        drawCover(canvas, frame);

        if (resultBitmap != null) { // 绘制扫描结果的图
            // Draw the opaque result bitmap over the scanning rectangle
            paint.setAlpha(0xA0);
            canvas.drawBitmap(resultBitmap, null, frame, paint);
        } else {
            // 画扫描框边上的角
            drawRectEdges(canvas, frame);

            // 绘制扫描线
            drawScanningLine(canvas, frame);

            List<ResultPoint> currentPossible = possibleResultPoints;
            Collection<ResultPoint> currentLast = lastPossibleResultPoints;
            if (currentPossible.isEmpty()) {
                lastPossibleResultPoints = null;
            } else {
                possibleResultPoints = new ArrayList<ResultPoint>(5);
                lastPossibleResultPoints = currentPossible;
                paint.setAlpha(OPAQUE);
                paint.setColor(resultPointColor);
                for (ResultPoint point : currentPossible) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 6.0f, paint);
                }
            }
            if (currentLast != null) {
                paint.setAlpha(OPAQUE / 2);
                paint.setColor(resultPointColor);
                for (ResultPoint point : currentLast) {
                    canvas.drawCircle(frame.left + point.getX(), frame.top + point.getY(), 3.0f, paint);
                }
            }

            // 只刷新扫描框的内容，其他地方不刷新
            postInvalidateDelayed(ANIMATION_DELAY, frame.left, frame.top, frame.right, frame.bottom);

        }
    }

    /**
     * 绘制扫描线
     *
     * @param canvas
     * @param frame  扫描框
     */
    private void drawScanningLine(Canvas canvas, Rect frame) {

        // 初始化中间线滑动的最上边和最下边
        if (isFirst) {
            isFirst = false;
            slideTop = frame.top;
            slideBottom = frame.bottom;
        }

        // 绘制中间的线,每次刷新界面，中间的线往下移动SPEEN_DISTANCE
        slideTop += SPEEN_DISTANCE;
        if (slideTop >= slideBottom) {
            slideTop = frame.top;
        }

        // 从图片资源画扫描线
        Rect lineRect = new Rect();
        lineRect.left = frame.left + MIDDLE_LINE_PADDING;
        lineRect.right = frame.right - MIDDLE_LINE_PADDING;
        lineRect.top = slideTop;
        lineRect.bottom = (slideTop + MIDDLE_LINE_WIDTH);
        canvas.drawBitmap(lineBitmap, null, lineRect, paint);

    }

    /**
     * 绘制遮掩层
     */
    private void drawCover(Canvas canvas, Rect frame) {
        // 获取屏幕的宽和高
        int width = canvas.getWidth();
        int height = canvas.getHeight();

        // Draw the exterior (i.e. outside the framing rect) darkened
        paint.setColor(resultBitmap != null ? resultColor : maskColor);

        // 画出扫描框外面的阴影部分，共四个部分，扫描框的上面到屏幕上面，扫描框的下面到屏幕下面
        // 扫描框的左边面到屏幕左边，扫描框的右边到屏幕右边
        canvas.drawRect(0, 0, width, frame.top, paint);
        canvas.drawRect(0, frame.top, frame.left, frame.bottom + 1, paint);
        canvas.drawRect(frame.right + 1, frame.top, width, frame.bottom + 1,
                paint);
        canvas.drawRect(0, frame.bottom + 1, width, height, paint);
    }

    /**
     * 描绘方形的四个角
     */
    private void drawRectEdges(Canvas canvas, Rect frame) {
        // 设置画笔颜色和透明度
        paint.setColor(mColor);
        paint.setAlpha(OPAQUE);

        //画扫描框边上的角，总共8个部分
        canvas.drawRect(frame.left, frame.top, frame.left + cornerLineH, frame.top + cornerLineW, paint);
        canvas.drawRect(frame.left, frame.top, frame.left + cornerLineW, frame.top + cornerLineH, paint);
        canvas.drawRect(frame.right - cornerLineH, frame.top, frame.right, frame.top + cornerLineW, paint);
        canvas.drawRect(frame.right - cornerLineW, frame.top, frame.right, frame.top + cornerLineH, paint);
        canvas.drawRect(frame.left, frame.bottom - cornerLineW, frame.left + cornerLineH, frame.bottom, paint);
        canvas.drawRect(frame.left, frame.bottom - cornerLineH, frame.left + cornerLineW, frame.bottom, paint);
        canvas.drawRect(frame.right - cornerLineH, frame.bottom - cornerLineW, frame.right, frame.bottom, paint);
        canvas.drawRect(frame.right - cornerLineW, frame.bottom - cornerLineH, frame.right, frame.bottom, paint);
    }

    /**
     * 设置扫描线
     * @param bitmap
     */
    public void setScanningLine(Bitmap bitmap) {
        lineBitmap = bitmap;
        invalidate();
    }

    /**
     * 设置颜色
     *
     * @param laserColor
     */
    public void setLaserColor(@ColorInt int laserColor) {
        this.mColor = laserColor;
        paint.setColor(this.mColor);
        invalidate();
    }

    public void setCornerLineH(int cornerLineH) {
        this.cornerLineH = cornerLineH;
    }

    public void setCornerLineW(int cornerLineW) {
        this.cornerLineW = cornerLineW;
    }

    public void drawViewfinder() {
        Bitmap resultBitmap = this.resultBitmap;
        this.resultBitmap = null;
        if (resultBitmap != null) {
            resultBitmap.recycle();
        }
        invalidate();
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

    public void addPossibleResultPoint(ResultPoint point) {
        List<ResultPoint> points = possibleResultPoints;
        synchronized (points) {
            points.add(point);
            int size = points.size();
            if (size > MAX_RESULT_POINTS) {
                // trim it
                points.subList(0, size - MAX_RESULT_POINTS / 2).clear();
            }
        }
    }

    /**
     * dp转px
     *
     * @param context
     * @param dipValue
     * @return
     */
    public int dip2px(Context context, float dipValue) {
        final float scale = context.getResources().getDisplayMetrics().density;
        return (int) (dipValue * scale + 0.5f);
    }

}
