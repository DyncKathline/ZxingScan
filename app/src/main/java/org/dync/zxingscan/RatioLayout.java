package org.dync.zxingscan;

import android.content.Context;
import android.content.res.TypedArray;
import android.util.AttributeSet;
import android.widget.FrameLayout;

public class RatioLayout extends FrameLayout {

    private float mRatio;//图片的宽高比
    public static final int RELATIVE_WIDTH = 0;//控件的宽度固定，根据比例求出高度
    public static final int RELATIVE_HEIGHT = 1;//控件的高度固定，根据比例求出宽度
    private int mRatioMode = RELATIVE_WIDTH;

    public RatioLayout(Context context) {
        this(context, null);
    }

    public RatioLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public RatioLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        initAttributes(context, attrs);
    }

    private void initAttributes(Context context, AttributeSet attrs) {
        TypedArray array = context.obtainStyledAttributes(attrs, R.styleable.RatioLayout);
        for (int i = 0; i < array.getIndexCount(); i++) {
            switch (i) {
                case R.styleable.RatioLayout_ratio:
                    String dimensionRatio = array.getString(i);
                    int dimensionRatioSide = -1;
                    if (dimensionRatio == null) {
                        break;
                    }

                    int len = dimensionRatio.length();
                    int commaIndex = dimensionRatio.indexOf(44);//,
                    if (commaIndex > 0 && commaIndex < len - 1) {
                        String dimension = dimensionRatio.substring(0, commaIndex);
                        if (dimension.equalsIgnoreCase("W")) {
                            dimensionRatioSide = RELATIVE_WIDTH;
                        } else if (dimension.equalsIgnoreCase("H")) {
                            dimensionRatioSide = RELATIVE_HEIGHT;
                        }

                        ++commaIndex;
                    } else {
                        commaIndex = 0;
                    }

                    int colonIndex = dimensionRatio.indexOf(58);//:
                    String r;
                    if (colonIndex >= 0 && colonIndex < len - 1) {
                        r = dimensionRatio.substring(commaIndex, colonIndex);
                        String denominator = dimensionRatio.substring(colonIndex + 1);
                        if (r.length() > 0 && denominator.length() > 0) {
                            try {
                                float nominatorValue = Float.parseFloat(r);
                                float denominatorValue = Float.parseFloat(denominator);
                                if (nominatorValue > 0.0F && denominatorValue > 0.0F) {
                                    if (dimensionRatioSide == RELATIVE_HEIGHT) {
                                        mRatio = Math.abs(denominatorValue / nominatorValue);
                                    } else {
                                        mRatio = Math.abs(nominatorValue / denominatorValue);
                                    }
                                }
                            } catch (NumberFormatException var16) {
                            }
                        }
                    } else {
                        r = dimensionRatio.substring(commaIndex);
                        if (r.length() > 0) {
                            try {
                                mRatio = Float.parseFloat(r);
                            } catch (NumberFormatException var15) {
                            }
                        }
                    }
                    break;
                case R.styleable.RatioLayout_ratioMode:
                    mRatioMode = array.getInt(i, RELATIVE_WIDTH);
                    break;
            }
        }
        array.recycle();
    }

    public void setRatio(float ratio) {
        mRatio = ratio;
    }

    public void setRatioMode(int ratioMode) {
        this.mRatioMode = ratioMode;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        // MeasureSpec.EXACTLY 确定值, 比如把宽高值写死,或者match_parent
        // MeasureSpec.AT_MOST 至多, 能撑多大就多大, 类似wrap_content
        // MeasureSpec.UNSPECIFIED 未指定大小
        if (widthMode == MeasureSpec.EXACTLY && heightMode != MeasureSpec.EXACTLY) {
            int width = MeasureSpec.getSize(widthMeasureSpec);
            //拿到了控件的宽
            int height = (int) ((width / mRatio));
            setMeasuredDimension(width, height);
            setAndMeasureChilds(width, height);
        } else if (widthMode != MeasureSpec.EXACTLY && heightMode == MeasureSpec.EXACTLY) {
            //如果高度已经确定的话
            int height = MeasureSpec.getSize(heightMeasureSpec);
            //拿到了控件的宽
            int width = (int) ((height * mRatio));
            setMeasuredDimension(width, height);
            setAndMeasureChilds(width, height);
        } else {
            super.setMeasuredDimension(widthMeasureSpec, heightMeasureSpec);
        }
    }

    /**
     * 得到子控件应有的宽和高,然后调用方法测量子控件的宽和高 * @param width * @param height
     */
    private void setAndMeasureChilds(int width, int height) {
        int childWidth = width - getPaddingLeft() - getPaddingRight();
        int childHeight = height - getPaddingTop() - getPaddingBottom();
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
        measureChildren(childWidthMeasureSpec, childHeightMeasureSpec);
    }

}
