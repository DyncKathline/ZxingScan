package org.dync.zxinglibrary.zxing.encode;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.support.annotation.ColorInt;
import android.text.TextPaint;
import android.text.TextUtils;
import android.util.Log;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;

import java.io.UnsupportedEncodingException;
import java.util.Hashtable;

/**
 * @author 刘红亮  2015年4月29日  下午3:27:08
 *         用于生成二维码
 */
public final class EncodingHandler {
    static final int BLACK = 0xff000000;
    static final int WHITE = 0xFFFFFFFF;
    /**
     * 生成二维码图片
     *
     * @param str            要往二维码中写入的内容,需要utf-8格式
     * @param widthAndHeight 图片的宽高，正方形
     * @return 返回一个二维码bitmap
     * @throws WriterException
     * @throws UnsupportedEncodingException
     */
    public static Bitmap create2Code(String str, int widthAndHeight) throws WriterException, UnsupportedEncodingException {
        BitMatrix matrix = new MultiFormatWriter().encode(str, BarcodeFormat.QR_CODE, widthAndHeight, widthAndHeight, getEncodeHintMap());
        return BitMatrixToBitmap(matrix);
    }

    /**
     * 生成条形码图片
     * @param str 要往二维码中写入的内容,需要utf-8格式
     * @param width 图片的宽
     * @param height 图片的高
     * @return 返回一个条形bitmap
     * @throws Exception
     */
    public static Bitmap createBarCode(String str, Integer width, Integer height) throws Exception {
            BitMatrix bitMatrix = new MultiFormatWriter().encode(str, BarcodeFormat.CODE_128, width, height, getEncodeHintMap());
            return BitMatrixToBitmap(bitMatrix);
    }
    /**
     * 条形码下面添加文本信息
     * @param src
     * @param code
     * @param textSize
     * @param textColor
     * @return
     */
    public static Bitmap addCode(Bitmap src, String code, int textSize, @ColorInt int textColor, int offset) {
        if (src == null) {
            return null;
        }

        if (TextUtils.isEmpty(code)) {
            return src;
        }

        //获取图片的宽高
        int srcWidth = src.getWidth();
        int srcHeight = src.getHeight();

        if (srcWidth <= 0 || srcHeight <= 0) {
            return null;
        }

        Bitmap bitmap = Bitmap.createBitmap(srcWidth, srcHeight + textSize + offset * 2, Bitmap.Config.ARGB_8888);
        try {
            Canvas canvas = new Canvas(bitmap);
            canvas.drawBitmap(src, 0, 0, null);
            TextPaint paint = new TextPaint();
            paint.setTextSize(textSize);
            paint.setColor(textColor);
            paint.setTextAlign(Paint.Align.CENTER);
            canvas.drawText(code,srcWidth/2,srcHeight + textSize /2 + offset,paint);
            canvas.save();
            canvas.restore();
        } catch (Exception e) {
            bitmap = null;
            e.printStackTrace();
        }

        return bitmap;
    }
    /**
     * 获得设置好的编码参数
     * @return 编码参数
     */
    private static Hashtable<EncodeHintType, Object> getEncodeHintMap() {
        Hashtable<EncodeHintType, Object> hints= new Hashtable<EncodeHintType, Object>();
        //设置编码为utf-8
        hints.put(EncodeHintType.CHARACTER_SET, "utf-8");
        // 设置QR二维码的纠错级别——这里选择最高H级别
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        return hints;
    }
    /**
     * BitMatrix转换成Bitmap
     *
     * @param matrix
     * @return
     */
    private static Bitmap BitMatrixToBitmap(BitMatrix matrix) {
        int width = matrix.getWidth();
        int height = matrix.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                if(matrix.get(x,y)){
                    pixels[offset + x] =BLACK; //上面图案的颜色
                }else{
                    pixels[offset + x] =WHITE;//底色
                }
            }
        }
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        Log.e("hongliang","width:"+bitmap.getWidth()+" height:"+bitmap.getHeight());
        return bitmap;
    }
}
