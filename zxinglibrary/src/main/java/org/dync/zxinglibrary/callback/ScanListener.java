package org.dync.zxinglibrary.callback;

import android.graphics.Bitmap;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

public interface ScanListener {
    /**
     * 返回扫描结果
     * @param rawResult  结果对象
     * @param bitmap  存放了截图，或者是空的
     */
    public void scanResult(Result rawResult, Bitmap bitmap);
    /**
     * 扫描抛出的异常
     * @param e
     */
    public void scanError(Exception e);

    public void foundPossibleResultPoint(ResultPoint point);
}
