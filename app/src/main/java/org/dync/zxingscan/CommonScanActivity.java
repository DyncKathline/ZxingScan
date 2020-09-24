package org.dync.zxingscan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.view.Surface;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.zxing.Result;

import org.dync.zxinglibrary.utils.Constant;
import org.dync.zxinglibrary.zxing.ScanListener;
import org.dync.zxinglibrary.zxing.ScanManager;
import org.dync.zxinglibrary.zxing.ViewfinderView;
import org.dync.zxinglibrary.zxing.decode.DecodeThread;
import org.dync.zxinglibrary.zxing.decode.Utils;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CommonScanActivity extends AppCompatActivity implements ScanListener, View.OnClickListener {

    static final String TAG = CommonScanActivity.class.getSimpleName();
    @BindView(R.id.capture_preview)
    SurfaceView capturePreview;
    @BindView(R.id.scan_container)
    ViewfinderView scanContainer;
    @BindView(R.id.img_back)
    ImageView imgBack;
    @BindView(R.id.tv_title)
    TextView tvTitle;
    @BindView(R.id.title_bar)
    RelativeLayout titleBar;
    @BindView(R.id.tv_scan_result)
    TextView tvScanResult;
    @BindView(R.id.top_mask)
    RelativeLayout topMask;
    @BindView(R.id.left_mask)
    ImageView leftMask;
    @BindView(R.id.scan_hint)
    TextView scanHint;
    @BindView(R.id.img_light)
    TextView imgLight;
    @BindView(R.id.img_exit)
    TextView imgExit;
    @BindView(R.id.img_gallery)
    TextView imgGallery;
    @BindView(R.id.btn_rescan)
    Button btnRescan;
    @BindView(R.id.bottom_mask)
    RelativeLayout bottomMask;
    @BindView(R.id.capture_container)
    RelativeLayout captureContainer;

    private Context mContext;
    private Activity mActivity;
    private ScanManager scanManager;
    final int PHOTOREQUESTCODE = 1111;
    private int scanMode;//扫描模型（条形，二维码，全部）


    @Override
    public void onCreate(Bundle icicle) {
        super.onCreate(icicle);

        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setContentView(R.layout.activity_common_scan);
        mContext = this;
        mActivity = this;
        ButterKnife.bind(this);
        scanMode = getIntent().getIntExtra(Constant.REQUEST_SCAN_MODE, Constant.REQUEST_SCAN_MODE_ALL_MODE);

        initView();
    }

    void initView() {
        switch (scanMode) {
            case DecodeThread.BARCODE_MODE:
                tvTitle.setText(R.string.scan_barcode_title);
                scanHint.setText(R.string.scan_barcode_hint);
                break;
            case DecodeThread.QRCODE_MODE:
                tvTitle.setText(R.string.scan_qrcode_title);
                scanHint.setText(R.string.scan_qrcode_hint);
                break;
            case DecodeThread.ALL_MODE:
                tvTitle.setText(R.string.scan_allcode_title);
                scanHint.setText(R.string.scan_allcode_hint);
                break;
        }
        imgGallery.setOnClickListener(this);
        imgBack.setOnClickListener(this);
        imgLight.setOnClickListener(this);
        btnRescan.setOnClickListener(this);
        imgExit.setOnClickListener(this);

        //构造出扫描管理器
        scanManager = new ScanManager(this, capturePreview, scanContainer, scanMode, this);
        scanManager.setPlayBeepAndVibrate(false, true);
        if (getBaseContext().getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
            scanManager.setConfiguration(90);
        } else {//如果是横屏
            scanManager.setConfiguration(0);
//            scanManager.setConfiguration(180);
        }
        scanContainer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                scanManager.switchCamera();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        scanManager.onResume();
        btnRescan.setVisibility(View.INVISIBLE);
        scanContainer.setVisibility(View.VISIBLE);
        scanContainer.setCameraManager(scanManager.getCameraManager());
        scanContainer.setLaserColor(getResources().getColor(R.color.colorAccent));
        scanContainer.setScanningLine(((BitmapDrawable) getResources()
                .getDrawable(R.mipmap.pic_scan_3scanlight)).getBitmap());
    }

    @Override
    public void onPause() {
        super.onPause();
        scanManager.onPause();
    }

    @Override
    public void scanResult(Result rawResult, Bundle bundle) {
        //扫描成功后，扫描器不会再连续扫描，如需连续扫描，调用reScan()方法。
        //scanManager.reScan();
//		Toast.makeText(that, "result="+rawResult.getText(), Toast.LENGTH_LONG).show();

        if (!scanManager.isScanning()) { //如果当前不是在扫描状态
            //设置再次扫描按钮出现
            btnRescan.setVisibility(View.VISIBLE);
            scanContainer.setVisibility(View.VISIBLE);
            Bitmap barcode = null;
            byte[] compressedBitmap = bundle.getByteArray(DecodeThread.BARCODE_BITMAP);
            if (compressedBitmap != null) {
                barcode = BitmapFactory.decodeByteArray(compressedBitmap, 0, compressedBitmap.length, null);
                barcode = barcode.copy(Bitmap.Config.ARGB_8888, true);
            }
            scanContainer.drawResultBitmap(barcode);
        }
        btnRescan.setVisibility(View.VISIBLE);
        scanContainer.setVisibility(View.VISIBLE);
        tvScanResult.setVisibility(View.VISIBLE);
        tvScanResult.setText("结果：" + rawResult.getText());
    }

    void startScan() {
        if (btnRescan.getVisibility() == View.VISIBLE) {
            btnRescan.setVisibility(View.INVISIBLE);
            scanContainer.setVisibility(View.VISIBLE);
            scanContainer.drawResultBitmap(null);
            scanManager.reScan();
        }
    }

    @Override
    public void scanError(Exception e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        //相机扫描出错时
        if (e.getMessage() != null && e.getMessage().startsWith("相机")) {

        }
    }

    public void showPictures(int requestCode) {
//        Intent intent = new Intent(Intent.ACTION_PICK);
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        startActivityForResult(intent, requestCode);
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        String photo_path;
        if (resultCode == RESULT_OK) {
            switch (requestCode) {
                case PHOTOREQUESTCODE:
                    String[] proj = {MediaStore.Images.Media.DATA};
                    Cursor cursor = this.getContentResolver().query(data.getData(), proj, null, null, null);
                    if (cursor.moveToFirst()) {
                        int colum_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        photo_path = cursor.getString(colum_index);
                        if (photo_path == null) {
                            photo_path = Utils.getPath(getApplicationContext(), data.getData());
                        }
                        scanManager.scanningImage(photo_path);
                    }
            }
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_gallery:
                showPictures(PHOTOREQUESTCODE);
                break;
            case R.id.img_light:
                scanManager.switchLight();
                break;
            case R.id.img_exit:
                finish();
                break;
            case R.id.btn_rescan://再次开启扫描
                startScan();
                break;
            case R.id.img_back:
                finish();
                break;
            default:
                break;
        }
    }

}
