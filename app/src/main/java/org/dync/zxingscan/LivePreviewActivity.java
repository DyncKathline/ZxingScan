package org.dync.zxingscan;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Bundle;
import android.provider.MediaStore;
import android.text.style.ClickableSpan;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.mlkit.vision.barcode.Barcode;
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
import com.google.mlkit.vision.common.InputImage;
import com.kathline.barcode.BitmapUtils;
import com.kathline.barcode.CameraImageGraphic;
import com.kathline.barcode.CameraSourcePreview;
import com.kathline.barcode.FrameMetadata;
import com.kathline.barcode.GraphicOverlay;
import com.kathline.barcode.MLKit;
import com.kathline.barcode.PermissionUtil;
import com.kathline.barcode.Utils;
import com.kathline.barcode.barcodescanner.BarcodeGraphic;

import com.kathline.barcode.ViewfinderView;

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(TAG, "onCreate");

        setContentView(R.layout.activity_live_preview);
        context = this;

        preview = findViewById(R.id.preview_view);
        if (preview == null) {
            Log.d(TAG, "Preview is null");
        }
        graphicOverlay = findViewById(R.id.graphic_overlay);
        if (graphicOverlay == null) {
            Log.d(TAG, "graphicOverlay is null");
        }
        ViewfinderView viewfinderView = findViewById(R.id.viewfinderView);
        imgGallery = findViewById(R.id.img_gallery);
        imgLight = findViewById(R.id.img_light);
        imgExit = findViewById(R.id.img_exit);
        imgSwitchCamera = findViewById(R.id.img_switch_camera);

        imgGallery.setOnClickListener(this);
        imgLight.setOnClickListener(this);
        imgExit.setOnClickListener(this);
        imgSwitchCamera.setOnClickListener(this);

        //构造出扫描管理器
        configViewFinderView(viewfinderView);
        mlKit = new MLKit(this, preview, graphicOverlay);
        //是否扫描成功后播放提示音和震动
        mlKit.setPlayBeepAndVibrate(true, true);
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

        mlKit.setAnalyze(false);
        CustomDialog.Builder builder = new CustomDialog.Builder(context);
        CustomDialog dialog = builder
                .setContentView(R.layout.barcode_result_dialog)
                .setLayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
                .setOnInitListener(new CustomDialog.Builder.OnInitListener() {
                    @Override
                    public void init(CustomDialog customDialog) {
                        Button btnDialogCancel = customDialog.findViewById(R.id.btnDialogCancel);
                        Button btnDialogOK = customDialog.findViewById(R.id.btnDialogOK);
                        TextView tvDialogContent = customDialog.findViewById(R.id.tvDialogContent);
                        ImageView ivDialogContent = customDialog.findViewById(R.id.ivDialogContent);

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
                        } else {
                            ivDialogContent.setVisibility(View.GONE);
                        }
                        SpanUtils spanUtils = SpanUtils.with(tvDialogContent);
                        for (int i = 0; i < barcodes.size(); ++i) {
                            Barcode barcode = barcodes.get(i);
                            BarcodeGraphic graphic = new BarcodeGraphic(graphicOverlay, barcode);
                            graphicOverlay.add(graphic);
                            Rect boundingBox = barcode.getBoundingBox();
                            spanUtils.append(String.format("(%d,%d)", boundingBox.left, boundingBox.top))
                                    .append(barcode.getRawValue())
                                    .setClickSpan(i % 2 == 0 ? getResources().getColor(R.color.colorPrimary) : getResources().getColor(R.color.colorAccent), false, new View.OnClickListener() {
                                @Override
                                public void onClick(View v) {
                                    Toast.makeText(getApplicationContext(), barcode.getRawValue(), Toast.LENGTH_SHORT).show();
                                }
                            })
                                    .setBackgroundColor(i % 2 == 0 ? getResources().getColor(R.color.colorAccent) : getResources().getColor(R.color.colorPrimary))
                                    .appendLine()
                                    .appendLine();
                        }
                        spanUtils.create();
                        Bitmap bitmapFromView = loadBitmapFromView(graphicOverlay);
                        ivDialogContent.setImageBitmap(bitmapFromView);

                        btnDialogCancel.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                customDialog.dismiss();
                                finish();
                            }
                        });
                        btnDialogOK.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                customDialog.dismiss();
                                mlKit.setAnalyze(true);
                            }
                        });
                    }
                })
                .build();
    }

    public static Bitmap loadBitmapFromView(View v) {
        v.setDrawingCacheEnabled(true);
        v.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        v.setDrawingCacheBackgroundColor(Color.WHITE);

        int w = v.getWidth();
        int h = v.getHeight();

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bmp);

        c.drawColor(Color.WHITE);
        /** 如果不设置canvas画布为白色，则生成透明 */

        v.layout(0, 0, w, h);
        v.draw(c);

        return bmp;
    }

    private void configViewFinderView(ViewfinderView viewfinderView) {

    }

    private void requirePermission() {
        PermissionUtil.getInstance().with(this).requestPermissions(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, new PermissionUtil.PermissionListener() {
            @Override
            public void onGranted() {
//        Intent intent = new Intent(Intent.ACTION_PICK);
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
                    if (cursor.moveToFirst()) {
                        int colum_index = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA);
                        photo_path = cursor.getString(colum_index);
                        if (photo_path == null) {
                            photo_path = Utils.getPath(getApplicationContext(), data.getData());
                        }
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
                Log.d(TAG, "Set facing");
                mlKit.switchCamera();
                break;
            default:
                break;
        }
    }
}
