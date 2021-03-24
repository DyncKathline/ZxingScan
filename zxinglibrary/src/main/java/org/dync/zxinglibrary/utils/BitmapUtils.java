package org.dync.zxinglibrary.utils;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

/**
 * @author BayMax·Yi
 * @time 2019/6/19 11:17
 * @modiffy
 * @modiffyTime 2019/6/19 11:17
 * @describe 类描述
 */
public class BitmapUtils {
    public static Bitmap decodeBitmapResource(Context context, int res, int viewWidth, int viewHeight) {
        float scale = calculateScale(context, res, viewWidth, viewHeight);
        if (scale < 1)
            return BitmapFactory.decodeResource(context.getResources(), res, null);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = false;
        scale = (float) Math.ceil(scale);
        options.inSampleSize = (int) scale;
        return BitmapFactory.decodeResource(context.getResources(), res, options);
    }

    private static float calculateScale(Context context, int res, int viewWidth, int viewHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeResource(context.getResources(), res, options);
        int width = options.outWidth;
        int height = options.outHeight;
        return Math.min(width * 1.0f / viewWidth, height * 1.0f / viewHeight);
    }

    public static int calculateInSampleSize(BitmapFactory.Options options,
                                            int reqWidth, int reqHeight) {
        // 源图片的高度和宽度
        final int height = options.outHeight;
        final int width = options.outWidth;
        //压缩当前图片占用内存不超过应用可用内存的3/4
        //ARGB_8888  一个像素占用4个字节
        //1兆字节(mb)=1048576字节(b)
        long FREE_MEMORY = ((int) Runtime.getRuntime().freeMemory())/1024/1024;
        while(reqHeight*reqWidth*4> FREE_MEMORY*1048576/4*3){
            reqHeight-=50;
            reqWidth-=50;
        }
        int inSampleSize = 1;
        if (height > reqHeight || width > reqWidth) {
            // 计算出实际宽高和目标宽高的比率
            final int heightRatio = Math.round((float) height / (float) reqHeight);
            final int widthRatio = Math.round((float) width / (float) reqWidth);
            // 选择宽和高中最小的比率作为inSampleSize的值，这样可以保证最终图片的宽和高
            // 一定都会大于等于目标的宽和高。
            inSampleSize = heightRatio < widthRatio ? heightRatio : widthRatio;
        }
        if(inSampleSize==0) return 1;
        Log.e("hongliang","inSampleSize=" + inSampleSize);
        return inSampleSize;
    }

    public static Bitmap decodeBitmapFromPath(String photo_path, int reqWidth, int reqHeight, boolean isFix) {
        // 第一次解析将inJustDecodeBounds设置为true，来获取图片大小
        final BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        if(isFix) {
            Bitmap scanBitmap = BitmapFactory.decodeFile(photo_path, options);
        }
        // 调用上面定义的方法计算inSampleSize值
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        // 使用获取到的inSampleSize值再次解析图片
        options.inJustDecodeBounds = false;
        return BitmapFactory.decodeFile(photo_path, options);
    }
}
