package org.dync.zxingscan;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Matrix;
import android.graphics.Rect;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.SurfaceView;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.BarcodeFormat;

import org.dync.zxinglibrary.encode.EncodeHelper;
import org.dync.zxinglibrary.utils.QRCode;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class CreateCodeActivity extends AppCompatActivity {

    @BindView(R.id.et_code_key)
    EditText etCodeKey;
    @BindView(R.id.btn_create_code)
    Button btnCreateCode;
    @BindView(R.id.btn_create_code_and_img)
    Button btnCreateCodeAndImg;
    @BindView(R.id.iv_2_code)
    ImageView iv2Code;
    @BindView(R.id.iv_bar_code)
    ImageView ivBarCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_create_code);
        ButterKnife.bind(this);

        RelativeLayout flCamera = findViewById(R.id.fl_camera);
        SurfaceView surfaceView = new SurfaceView(this);
        flCamera.addView(surfaceView);
        final Camera1Helper mCameraHelper = new Camera1Helper(surfaceView);
        flCamera.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(mCameraHelper != null) {
                    mCameraHelper.switchCamera();
                }
            }
        });
    }

    @OnClick({R.id.et_code_key, R.id.btn_create_code, R.id.btn_create_code_and_img, R.id.iv_2_code, R.id.iv_bar_code})
    public void onViewClicked(View view) {
        String key = etCodeKey.getText().toString();
        switch (view.getId()) {
            case R.id.btn_create_code: //生成码
                if (TextUtils.isEmpty(key)) {
                    Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
                } else {
                    create2Code(key);
                    createBarCode(key);
                }
                break;
            case R.id.btn_create_code_and_img: //生成码
                if (!TextUtils.isEmpty(key)) {
                    Bitmap bitmap = create2Code(key);
                    Bitmap headBitmap = getHeadBitmap(60);
                    if (bitmap != null && headBitmap != null) {
                        createQRCodeBitmapWithPortrait(bitmap, headBitmap);
                    }
                } else {
                    Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
                }
                break;
        }
    }

    private Bitmap createBarCode(String key) {
        Bitmap qrCode = null;
        try {
            EncodeHelper helper = new EncodeHelper();
            qrCode = helper.encodeCode(key, BarcodeFormat.CODE_128, 400, 100);
            ivBarCode.setImageBitmap(qrCode);
        } catch (Exception e) {
            Toast.makeText(this, "输入的内容条形码不支持！", Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
        return qrCode;
    }

    /**
     * 生成二维码
     *
     * @param key
     */
    private Bitmap create2Code(String key) {
        Bitmap qrCode = null;
        qrCode = QRCode.createQRCodeWithLogo2(key, 500, QRCode.drawableToBitmap(getResources().getDrawable(R.drawable.icon)));
        iv2Code.setImageBitmap(qrCode);
        return qrCode;
    }

    /**
     * 初始化头像图片
     */
    private Bitmap getHeadBitmap(int size) {
        try {
            // 这里采用从asset中加载图片abc.jpg
            Bitmap portrait = BitmapFactory.decodeResource(getResources(), R.drawable.icon);
            // 对原有图片压缩显示大小
            Matrix mMatrix = new Matrix();
            float width = portrait.getWidth();
            float height = portrait.getHeight();
            mMatrix.setScale(size / width, size / height);
            return Bitmap.createBitmap(portrait, 0, 0, (int) width,
                    (int) height, mMatrix, true);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * 在二维码上绘制头像
     */
    private void createQRCodeBitmapWithPortrait(Bitmap qr, Bitmap portrait) {
        // 头像图片的大小
        int portrait_W = portrait.getWidth();
        int portrait_H = portrait.getHeight();

        // 设置头像要显示的位置，即居中显示
        int left = (qr.getWidth() - portrait_W) / 2;
        int top = (qr.getHeight() - portrait_H) / 2;
        int right = left + portrait_W;
        int bottom = top + portrait_H;
        Rect rect1 = new Rect(left, top, right, bottom);

        // 取得qr二维码图片上的画笔，即要在二维码图片上绘制我们的头像
        Canvas canvas = new Canvas(qr);

        // 设置我们要绘制的范围大小，也就是头像的大小范围
        Rect rect2 = new Rect(0, 0, portrait_W, portrait_H);
        // 开始绘制
        canvas.drawBitmap(portrait, rect2, rect1, null);
    }
}
