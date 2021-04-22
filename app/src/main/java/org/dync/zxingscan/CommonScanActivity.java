package org.dync.zxingscan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.SurfaceView;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.Result;
import com.google.zxing.ResultPoint;

import org.dync.zxinglibrary.ScanManager;
import org.dync.zxinglibrary.callback.ScanListener;
import org.dync.zxinglibrary.decod.Utils;
import org.dync.zxinglibrary.view.ViewfinderView;

import butterknife.BindView;
import butterknife.ButterKnife;

public class CommonScanActivity extends AppCompatActivity implements ScanListener, View.OnClickListener {

    static final String TAG = CommonScanActivity.class.getSimpleName();
    @BindView(R.id.surface_view)
    SurfaceView surfaceView;
    @BindView(R.id.viewfinder_view)
    ViewfinderView viewfinderView;
    @BindView(R.id.tv_scan_result)
    TextView tvScanResult;
    @BindView(R.id.img_switch_camera)
    ImageView imgSwitchCamera;
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

    private Context mContext;
    private Activity mActivity;
    private ScanManager scanManager;
    public static final String REQUEST_SCAN_MODE = "REQUEST_SCAN_MODE";
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
        scanMode = getIntent().getIntExtra(REQUEST_SCAN_MODE, ScanManager.SCANTYPE_ALL);

        initView();
    }

    void initView() {
        imgGallery.setOnClickListener(this);
        imgLight.setOnClickListener(this);
        btnRescan.setOnClickListener(this);
        imgExit.setOnClickListener(this);
        imgSwitchCamera.setOnClickListener(this);

        //构造出扫描管理器
        configViewFinderView(viewfinderView);
        scanManager = new ScanManager(this, surfaceView, viewfinderView, scanMode, this);
        scanManager.setPlayBeepAndVibrate(false, true);
        scanManager.onCreate();
    }

    private void configViewFinderView(ViewfinderView viewfinderView) {
        viewfinderView.setAnimatorDuration(1500);
        viewfinderView.setConorColor(Color.GREEN);
        viewfinderView.setCornorLength(60);
        viewfinderView.setLineColors(new int[]{Color.argb(10, 155, 255, 60),
                Color.argb(30, 155, 255, 60),
                Color.argb(50, 155, 255, 60),
                Color.argb(70, 155, 255, 60),
                Color.argb(90, 155, 255, 60),
                Color.argb(70, 155, 255, 60),
                Color.argb(50, 155, 255, 60),
                Color.argb(30, 155, 255, 60),
                Color.argb(10, 155, 255, 60),
                Color.argb(5, 155, 255, 60),});
        viewfinderView.setLinePercents(new float[]{0.1f, 0.2f, 0.3f, 0.4f, 0.5f, 0.6f, 0.7f, 0.8f, 0.9f, 1f});
    }

    @Override
    public void onResume() {
        scanManager.onResume();
        super.onResume();
        btnRescan.setVisibility(View.INVISIBLE);
        viewfinderView.setVisibility(View.VISIBLE);
    }

    @Override
    public void onPause() {
        scanManager.onPause();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        scanManager.onDestroy();
        super.onDestroy();
    }

    @Override
    public void scanResult(Result rawResult, Bitmap bitmap) {
        //扫描成功后，扫描器不会再连续扫描，如需连续扫描，调用reScan()方法。
        //scanManager.reScan();
//		Toast.makeText(that, "result="+rawResult.getText(), Toast.LENGTH_LONG).show();

        if (!scanManager.isScanning()) { //如果当前不是在扫描状态
            //设置再次扫描按钮出现
            btnRescan.setVisibility(View.VISIBLE);
            viewfinderView.setVisibility(View.VISIBLE);
            viewfinderView.drawResultBitmap(bitmap);
        }
        btnRescan.setVisibility(View.VISIBLE);
        viewfinderView.setVisibility(View.VISIBLE);
        tvScanResult.setVisibility(View.VISIBLE);
        tvScanResult.setText("结果：" + rawResult.getText());
    }

    void startScan() {
        if (btnRescan.getVisibility() == View.VISIBLE) {
            btnRescan.setVisibility(View.INVISIBLE);
            viewfinderView.setVisibility(View.VISIBLE);
            scanManager.restartPreviewAfterDelay(0);
            scanManager.drawViewfinder();
        }
    }

    @Override
    public void scanError(Exception e) {
        Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
        //相机扫描出错时
        if (e.getMessage() != null && e.getMessage().startsWith("相机")) {

        }
    }

    @Override
    public void foundPossibleResultPoint(ResultPoint point) {

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
            case R.id.img_switch_camera:
                scanManager.switchCamera();
                break;
            default:
                break;
        }
    }

}
