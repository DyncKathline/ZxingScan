package org.dync.zxinglibrary;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.util.AttributeSet;
import android.util.TypedValue;

/**
 * 作者：王敏 on 2015/8/21 17:31
 * 类说明：画出扫描框的四个脚的脚边框，也可以直接用一张图片代替
 */
public class ScanImageView extends android.support.v7.widget.AppCompatImageView {
    private Context context;
    private Paint mPaint;
    private @ColorInt
    int mColor;

    public ScanImageView(Context context) {
        this(context, null);
        this.context = context;
    }

    public ScanImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        this.context = context;

        init();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();

        canvas.drawLine(0, 0, 0, t(18), mPaint);
        canvas.drawLine(0, 0, t(18), 0, mPaint);

        canvas.drawLine(0, height - t(18), 0, height, mPaint);
        canvas.drawLine(0, height, t(18), height, mPaint);

        canvas.drawLine(width - t(18), 0, width, 0, mPaint);
        canvas.drawLine(width, 0, width, t(18), mPaint);

        canvas.drawLine(width, height - t(18), width, height, mPaint);
        canvas.drawLine(width - t(18), height, width, height, mPaint);
    }

    private void init() {
        if (mPaint == null) {
            mPaint = new Paint();
        }
        if(mColor == 0) {
            mPaint.setColor(Color.rgb(9, 187, 7));
        }else {
            mPaint.setColor(mColor);
        }
        mPaint.setAntiAlias(true);
        mPaint.setStrokeWidth(t(5));
    }

    public int dp2px(float dpVal) {
        return (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP,
                dpVal, context.getResources().getDisplayMetrics());
    }

    public int t(float dpVal) {
        return dp2px(dpVal);
    }

    public void setMarginColor(@ColorInt int color) {
        mColor = color;
        mPaint.setColor(mColor);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        //   setMeasuredDimension(t(248),t(248));
    }
}
