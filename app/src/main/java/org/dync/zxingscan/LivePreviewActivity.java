package org.dync.zxingscan;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.ActivityCompat;

import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.barcode.common.Barcode;
import com.google.mlkit.vision.common.InputImage;
import com.kathline.barcode.BitmapUtils;
import com.kathline.barcode.CameraImageGraphic;
import com.kathline.barcode.CameraSourcePreview;
import com.kathline.barcode.FrameMetadata;
import com.kathline.barcode.GraphicOverlay;
import com.kathline.barcode.MLKit;
import com.kathline.barcode.PermissionUtil;
import com.kathline.barcode.UriUtils;
import com.kathline.barcode.ViewfinderView;
import com.kathline.barcode.barcodescanner.WxGraphic;

import java.nio.ByteBuffer;
import java.util.List;

public class LivePreviewActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback,
        View.OnClickListener {

    private static final String TAG = "LivePreviewActivity";
    private Context context;

    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private MLKit mlKit;

    final int PHOTOREQUESTCODE = 1111;
    private TextView imgGallery;
    private TextView imgLight;
    private TextView imgExit;
    private ImageView imgSwitchCamera;
    private ImageView imgBack;
    private CameraSourcePreview previewView;
    private ViewfinderView viewfinderView;
    private ConstraintLayout previewBox;
    private TextView scanHint;
    private RelativeLayout bottomMask;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_live_preview);
        initViews();
        context = this;

        preview = findViewById(R.id.preview_view);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }

        imgGallery.setOnClickListener(this);
        imgLight.setOnClickListener(this);
        imgExit.setOnClickListener(this);
        imgSwitchCamera.setOnClickListener(this);
        imgBack.setOnClickListener(this);

        //构造出扫描管理器
        mlKit = new MLKit(this, preview, graphicOverlay);
        //是否扫描成功后播放提示音和震动
        mlKit.setPlayBeepAndVibrate(true, false);
        //仅识别二维码
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_AZTEC)
                        .build();
        mlKit.setBarcodeFormats(null);
        mlKit.setOnScanListener(new MLKit.OnScanListener() {
            @Override
            public void onSuccess(List<Barcode> barcodes, @NonNull GraphicOverlay graphicOverlay, InputImage image) {
                showScanResult(barcodes, graphicOverlay, image);
            }

            @Override
            public void onFail(int code, Exception e) {

            }
        });
    }

    private void showScanResult(List<Barcode> barcodes, @NonNull GraphicOverlay graphicOverlay, InputImage image) {
        if (barcodes.isEmpty()) {
            return;
        }
        Bitmap bitmap = null;
        ByteBuffer byteBuffer = image.getByteBuffer();
        if (byteBuffer != null) {
            FrameMetadata.Builder builder = new FrameMetadata.Builder();
            builder.setWidth(image.getWidth())
                    .setHeight(image.getHeight())
                    .setRotation(image.getRotationDegrees());
            bitmap = BitmapUtils.getBitmap(byteBuffer, builder.build());
        } else {
            bitmap = image.getBitmapInternal();
        }
        if (bitmap != null) {
            graphicOverlay.add(new CameraImageGraphic(graphicOverlay, bitmap));
        }
        for (int i = 0; i < barcodes.size(); ++i) {
            Barcode barcode = barcodes.get(i);
            WxGraphic graphic = new WxGraphic(graphicOverlay, barcode);
            graphic.setColor(getResources().getColor(R.color.colorAccent));
            Bitmap bitmapPaint = BitmapFactory.decodeResource(getResources(), R.mipmap.ico_wechat);
            graphic.setBitmap(bitmapPaint);
            graphic.setOnClickListener(new WxGraphic.OnClickListener() {
                @Override
                public void onClick(Barcode barcode) {
                    Toast.makeText(getApplicationContext(), "圆被点击: " + barcode.getRawValue(), Toast.LENGTH_SHORT).show();
                }
            });
            graphicOverlay.add(graphic);
        }
        if (barcodes.size() > 0) {
            imgBack.setVisibility(View.VISIBLE);
            imgSwitchCamera.setVisibility(View.INVISIBLE);
            bottomMask.setVisibility(View.GONE);
            mlKit.stopProcessor();
        }
    }

    private void requirePermission() {
        PermissionUtil.getInstance().with(this).requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, new PermissionUtil.PermissionListener() {
            @Override
            public void onGranted() {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, PHOTOREQUESTCODE);
            }

            @Override
            public void onDenied(List<String> deniedPermission) {
                PermissionUtil.getInstance().showDialogTips(getBaseContext(), deniedPermission, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
//                        finish();
                    }
                });
            }

            @Override
            public void onShouldShowRationale(List<String> deniedPermission) {
                requirePermission();
            }
        });
    }

    public void showPictures() {
        requirePermission();
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
                    if (cursor != null && cursor.moveToFirst()) {
                        int colum_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        photo_path = cursor.getString(colum_index);
                        if (photo_path == null) {
                            photo_path = UriUtils.getPath(getApplicationContext(), data.getData());
                        }
                        cursor.close();
                        mlKit.scanningImage(photo_path);
                        mlKit.setOnScanListener(new MLKit.OnScanListener() {
                            @Override
                            public void onSuccess(List<Barcode> barcodes, @NonNull GraphicOverlay graphicOverlay, InputImage image) {
                                showScanResult(barcodes, graphicOverlay, image);
                            }

                            @Override
                            public void onFail(int code, Exception e) {

                            }
                        });
                    }
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.img_gallery:
                showPictures();
                break;
            case R.id.img_light:
                mlKit.switchLight();
                break;
            case R.id.img_exit:
                finish();
                break;
            case R.id.img_switch_camera:
                mlKit.switchCamera();
                break;
            case R.id.img_back:
                mlKit.startProcessor();
                imgBack.setVisibility(View.GONE);
                imgSwitchCamera.setVisibility(View.VISIBLE);
                bottomMask.setVisibility(View.VISIBLE);
                break;
            default:
                break;
        }
    }

    private void initViews() {
        previewView = findViewById(R.id.preview_view);
        viewfinderView = findViewById(R.id.viewfinderView);
        graphicOverlay = findViewById(R.id.graphic_overlay);
        previewBox = findViewById(R.id.preview_box);
        imgBack = findViewById(R.id.img_back);
        imgSwitchCamera = findViewById(R.id.img_switch_camera);
        scanHint = findViewById(R.id.scan_hint);
        imgLight = findViewById(R.id.img_light);
        imgExit = findViewById(R.id.img_exit);
        imgGallery = findViewById(R.id.img_gallery);
        bottomMask = findViewById(R.id.bottom_mask);
    }
}
