package org.dync.zxingscan;

import android.app.Dialog;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.Window;
import android.view.WindowManager;

import androidx.annotation.FloatRange;
import androidx.annotation.LayoutRes;

import java.io.Serializable;

/**
 * Created by KathLine on 2016/11/17.</br>
 * <p>使用说明</p>
 * <pre>
 CustomDialog.Builder builder = new CustomDialog.Builder(mContext);
 CustomDialog dialog = builder.setFullScreen(true)
 .setExistDialogLined(true)
 .setBackgroundDrawable(true)
 .setLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
 .setOnInitListener(new CustomDialog.Builder.OnInitListener() {
@Override
public void init(CustomDialog customDialog) {

}
})
 .build();
 </pre>
 */
public class CustomDialog extends Dialog {

    protected Builder builder;

    protected CustomDialog(Context context, Builder builder) {
        super(context);
        this.builder = builder;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (builder.existDialogLined) {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
        }
        setContentView(builder.layoutId);
        Window window = getWindow();
        window.setGravity(builder.gravity);
        if(builder.animId != 0) {
            window.setWindowAnimations(builder.animId);
        }
        window.getDecorView().setPadding(0, 0, 0, 0);
        WindowManager.LayoutParams lp = window.getAttributes();
        if (builder.width != 0 && builder.height != 0) {
            lp.width = builder.width;
            lp.height = builder.height;
        }
        if (builder.isFullScreen) {
            window.setFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);//设置全屏，即没有系统状态栏
        }
        if (builder.backgroundDrawableable) {
            window.setBackgroundDrawable(new ColorDrawable(0));
        }else {
            window.setBackgroundDrawable(null);
        }
        if (builder.dimAmount < 0f || builder.dimAmount > 1f) {
            throw new RuntimeException("透明度必须在0~1之间");
        } else {
            lp.dimAmount = builder.dimAmount;
        }
        if (builder.offsetX != 0) {
            lp.x = builder.offsetX;
        }
        if (builder.offsetY != 0) {
            lp.y = builder.offsetY;
        }
        window.setAttributes(lp);
        setCancelable(builder.cancelable);
        if (builder.cancelable) {
            setCanceledOnTouchOutside(true);
        }else {
            setCanceledOnTouchOutside(false);
        }
        if(builder.onInitListener != null) {
            builder.onInitListener.init(this);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        /* 触摸外部弹窗 */
        if (isOutOfBounds(getContext(), event)) {
            if(builder.onTouchOutsideListener != null) {
                builder.onTouchOutsideListener.touchOutSide();
            }
        }
        return super.onTouchEvent(event);
    }

    /**
     * 判断当前用户触摸是否超出了Dialog的显示区域
     *
     * @param context
     * @param event
     * @return
     */
    private boolean isOutOfBounds(Context context, MotionEvent event) {
        final int x = (int) event.getX();
        final int y = (int) event.getY();
        final int slop = ViewConfiguration.get(context).getScaledWindowTouchSlop();
        final View decorView = getWindow().getDecorView();
        return (x < -slop) || (y < -slop) || (x > (decorView.getWidth() + slop))
                || (y > (decorView.getHeight() + slop));
    }

    public Builder getBuilder() {
        return builder;
    }

    public static class Builder implements Serializable {
        public Context context;
        public int layoutId;
        public int gravity;
        public int animId;
        public boolean backgroundDrawableable;
        public float dimAmount;
        public boolean cancelable;
        public boolean existDialogLined;
        public boolean isFullScreen;
        public int offsetX;
        public int offsetY;
        public int width;
        public int height;
        public OnInitListener onInitListener;
        public OnTouchOutsideListener onTouchOutsideListener;

        public Builder(Context context) {
            this.context = context;
            layoutId = android.R.layout.select_dialog_item;
            gravity = Gravity.CENTER;
            animId = 0;
            backgroundDrawableable = true;
            dimAmount = 0.5f;
            cancelable = true;
            existDialogLined = true;
            offsetX = 0;
            offsetY = 0;
            width = 0;
            height = 0;
        }

        public Builder setContentView(@LayoutRes int layoutId) {
            this.layoutId = layoutId;
            return this;
        }

        /**
         * 必须使用Gravity的静态常量，默认在中间弹出
         *
         * @param gravity 详见{@link Gravity}
         * @return
         * @see Gravity
         */
        public Builder setGravity(int gravity) {
            this.gravity = gravity;
            return this;
        }

        /**
         * 设置Dialog弹出和Dialog退出的动画
         *
         * @param animId
         * @return
         */
        public Builder setAnimId(int animId) {
            this.animId = animId;
            return this;
        }

        /**
         * Creates a new set of layout parameters with the specified width
         * and height.
         *
         * @param width  the width, either set WindowManager.LayoutParams.WRAP_CONTENT or
         *               WindowManager.LayoutParams.FILL_PARENT (replaced by WindowManager.LayoutParams.MATCH_PARENT in
         *               API Level 8), or a fixed size in pixels
         * @param height the height, either set WindowManager.LayoutParams.WRAP_CONTENT or
         *               WindowManager.LayoutParams.FILL_PARENT (replaced by WindowManager.LayoutParams.MATCH_PARENT in
         *               API Level 8), or a fixed size in pixels
         * @return
         */
        public Builder setLayoutParams(int width, int height) {
            this.width = width;
            this.height = height;
            return this;
        }

        /**
         * 设置Dialog的位置<br>
         * lp.x与lp.y表示相对于原始位置的偏移.
         * 当参数值包含Gravity.LEFT时,对话框出现在左边,所以lp.x就表示相对左边的偏移,负值无效.
         * 当参数值包含Gravity.RIGHT时,对话框出现在右边,所以lp.x就表示相对右边的偏移,负值无效.
         * 当参数值包含Gravity.TOP时,对话框出现在上边,所以lp.y就表示相对上边的偏移,负值无效.
         * 当参数值包含Gravity.BOTTOM时,对话框出现在下边,所以lp.y就表示相对下边的偏移,负值无效.
         * 当参数值包含Gravity.CENTER_HORIZONTAL时
         * ,对话框水平居中,所以lp.x就表示在水平居中的位置移动lp.x像素,正值向右移动,负值向左移动.
         * 当参数值包含Gravity.CENTER_VERTICAL时
         * ,对话框垂直居中,所以lp.y就表示在垂直居中的位置移动lp.y像素,正值向右移动,负值向左移动.
         * gravity的默认值为Gravity.CENTER
         *
         * @param x x小于0左移，大于0右移
         * @param y y小于0上移，大于0下移
         * @return
         */
        public Builder setOffset(int x, int y) {
            this.offsetX = x;
            this.offsetY = y;
            return this;
        }

        /**
         * 是否给Dialog的背景设置透明，默认false
         *
         * @param backgroundDrawableable
         * @return
         */
        public Builder setBackgroundDrawable(boolean backgroundDrawableable) {
            this.backgroundDrawableable = backgroundDrawableable;
            return this;
        }

        /**
         * 设置Dialog之外的背景透明度，0~1之间，默认值 0.5f，半透明，越小也透明
         *
         * @param dimAmount
         * @return
         */
        public Builder setDimAmount(@FloatRange(from = 0, to = 1.0) float dimAmount) {
            this.dimAmount = dimAmount;
            return this;
        }

        /**
         * 设置Dialog是否可以关闭在Dialog之外的区域，默认true
         *
         * @param cancelable
         * @return
         */
        public Builder setCancelable(boolean cancelable) {
            this.cancelable = cancelable;
            return this;
        }

        /**
         * 如果存在Holo主题下Dialog有蓝色线(含有标题栏)可以尝试调用该方法，默认不存在
         *
         * @param existDialogLined
         * @return
         */
        public Builder setExistDialogLined(boolean existDialogLined) {
            this.existDialogLined = existDialogLined;
            return this;
        }

        /**
         * 是否设置全屏模式，指的是去除系统状态栏，默认不去除
         *
         * @param isFullScreen
         * @return
         */
        public Builder setFullScreen(boolean isFullScreen) {
            this.isFullScreen = isFullScreen;
            return this;
        }

        /**
         * 设置子View
         *
         * @param listener OnInitListener
         * @return Builder
         */
        public Builder setOnInitListener(OnInitListener listener) {
            this.onInitListener = listener;
            return this;
        }

        public Builder setOnTouchOutsideListener(OnTouchOutsideListener listener){
            onTouchOutsideListener = listener;
            return this;
        }

        public interface OnTouchOutsideListener{
            void touchOutSide();
        }

        public interface OnInitListener {
            /**
             * 绑定控件
             *
             * @param customDialog
             */
            void init(CustomDialog customDialog);
        }

        public CustomDialog build() {
            CustomDialog dialog = new CustomDialog(context, this);
            dialog.show();
            return dialog;
        }
    }
}
