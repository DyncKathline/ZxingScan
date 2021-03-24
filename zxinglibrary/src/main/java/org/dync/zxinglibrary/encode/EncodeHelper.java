package org.dync.zxinglibrary.encode;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.text.TextUtils;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import org.dync.zxinglibrary.utils.BitmapUtils;

import java.util.HashMap;
import java.util.Map;

/**
 * @author BayMax·Yi
 * @time 2019/6/19 10:15
 * @modiffy
 * @modiffyTime 2019/6/19 10:15
 * @describe 类描述
 */
public class EncodeHelper {
    public static final int WRAP = 1;
    private static final int MIN_HEIGHT = 50;
    final float DEFAULT_SCALE = 1.0f / 4;

    private final int COLOR_BLACK = 0xff000000;
    private final int COLOR_WHITE = 0x00000000;
    private String charSet;
    private Bitmap.Config config;

    public EncodeHelper() {
        if (config == null) config = Bitmap.Config.ARGB_8888;
        if (TextUtils.isEmpty(charSet)) charSet = "UTF-8";
    }

    public Bitmap encodeQr(String content, int bitmapWidth, int bitmapHeight) {
        return encodeSimple(content, BarcodeFormat.QR_CODE, bitmapWidth, bitmapHeight);
    }

    private Map<String, Object> encodeImp(String content, BarcodeFormat barcodeFormat, int bitmapWidth, int bitmapHeight) {
        if (content == null) return null;
        Bitmap bitmap = null;
        int realWidth = 0;
        int realHeight = 0;
        Map<EncodeHintType, Object> encodeHints = new HashMap<EncodeHintType, Object>();
        encodeHints.put(EncodeHintType.CHARACTER_SET, charSet);
        try {
            BitMatrix matrix = new MultiFormatWriter().encode(content, barcodeFormat, bitmapWidth,
                    bitmapHeight < MIN_HEIGHT ? MIN_HEIGHT : bitmapHeight, encodeHints);
            int encodeWidth = matrix.getWidth();
            int encodeHeight = matrix.getHeight();
            bitmapWidth = (bitmapWidth == WRAP) ? encodeWidth : bitmapWidth;
            bitmapHeight = (bitmapHeight == WRAP) ? encodeHeight < MIN_HEIGHT ? MIN_HEIGHT : encodeHeight : bitmapHeight;
            bitmap = Bitmap.createBitmap(bitmapWidth, bitmapHeight, config);
            for (int i = 0; i < encodeHeight; i++) {
                for (int j = 0; j < encodeWidth; j++) {
                    boolean bol = matrix.get(j, i);
                    if (bol) {
                        realWidth = j;
                        realHeight = i;
                    }
                    bitmap.setPixel(j, i, bol ? COLOR_BLACK : COLOR_WHITE);
                }
            }
        } catch (WriterException e) {
            e.printStackTrace();
            return new HashMap<>(0);
        }
        Map<String, Object> dataMap = new HashMap<>();
        dataMap.put("bitmap", bitmap);
        dataMap.put("realWidth", realWidth);
        dataMap.put("realHeight", realHeight);
        return dataMap;
    }

    public Bitmap encodeQrLogo(String content, int bitmapWidth, int bitmapHeight, Bitmap logo) {
        if (content == null) return null;
        Map<String, Object> dataMap = encodeImp(content, BarcodeFormat.QR_CODE, bitmapWidth, bitmapHeight);
        Bitmap bitmap = (Bitmap) dataMap.get("bitmap");
        int realWidth = (int) dataMap.get("realWidth");
        int realHeight = (int) dataMap.get("realHeight");
        if (logo == null) return bitmap;
        Canvas canvas = new Canvas(bitmap);
        canvas.drawBitmap(bitmap, 0, 0, null);
        canvas.save();
        int srcWidth = bitmap.getWidth();
        int srcHeight = bitmap.getHeight();
        float scale = Math.max(realWidth * 1.0f / srcWidth, realHeight * 1.0f / srcHeight) * DEFAULT_SCALE;
        float scaleWidth = realWidth * scale;
        float scaleHeight = realHeight * scale;
        float logoScale = Math.min(scaleWidth * 1.0f / logo.getWidth(), scaleHeight * 1.0f / logo.getHeight());
        Matrix matrix = new Matrix();
        matrix.preScale(logoScale, logoScale);
        Bitmap bmLogo = Bitmap.createBitmap(logo, 0, 0, logo.getWidth(), logo.getHeight(), matrix, true);
        //    canvas.scale(scale, scale, srcWidth / 2, srcHeight / 2);
        canvas.drawBitmap(bmLogo, srcWidth / 2 - bmLogo.getWidth() / 2,
                srcHeight / 2 - bmLogo.getHeight() / 2, new Paint(Paint.ANTI_ALIAS_FLAG));
        canvas.restore();
        return bitmap;
    }

    public Bitmap encodeQrLogo(String content, Context context, int bitmapWidth, int bitmapHeight,
                               int res) {
        Bitmap bitmap = BitmapUtils.decodeBitmapResource(context, res, bitmapWidth, bitmapHeight);
        return encodeQrLogo(content, bitmapWidth, bitmapHeight, bitmap);
    }

    public Bitmap encodeCode(String content, BarcodeFormat barcodeFormat, int bitmapWidth, int bitmapHeight) {
        return encodeSimple(content, barcodeFormat, bitmapWidth, bitmapHeight);
    }

   /* public Bitmap encodeCodeNum(String content, BarcodeFormat barcodeFormat, int bitmapWidth, int bitmapHeight) {
        //-return encodeSimple(content, barcodeFormat, bitmapWidth, bitmapHeight);
    }*/

    private Bitmap encodeSimple(String content, BarcodeFormat barcodeFormat, int bitmapWidth, int bitmapHeight) {
        Map<String, Object> dataMap = encodeImp(content, barcodeFormat, bitmapWidth, bitmapHeight);
        return (Bitmap) dataMap.get("bitmap");
    }

    public void setCharSet(String charSet) {
        this.charSet = charSet;
    }


    public void setConfig(Bitmap.Config config) {
        this.config = config;
    }
}
