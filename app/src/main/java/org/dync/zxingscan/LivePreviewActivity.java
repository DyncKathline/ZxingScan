package org.dync.zxingscan;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.google.mlkit.vision.barcode.Barcode;
import com.kathline.barcode.CameraSourcePreview;
import com.kathline.barcode.GraphicOverlay;
import com.kathline.barcode.MLKit;
import com.kathline.barcode.barcodescanner.BarcodeGraphic;

import java.util.List;

public class LivePreviewActivity extends AppCompatActivity
        implements ActivityCompat.OnRequestPermissionsResultCallback,
        CompoundButton.OnCheckedChangeListener {

    private static final String TAG = "LivePreviewActivity";
    private Context context;

    private CameraSourcePreview preview;
    private GraphicOverlay graphicOverlay;
    private MLKit mlKit;

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

        ToggleButton facingSwitch = findViewById(R.id.facing_switch);
        facingSwitch.setOnCheckedChangeListener(this);

        mlKit = new MLKit(this, preview, graphicOverlay);
        mlKit.setOnScanListener(new MLKit.OnScanListener() {
            @Override
            public void onSuccess(List<Barcode> barcodes, @NonNull GraphicOverlay graphicOverlay) {
                if (barcodes.isEmpty()) {
                    return;
                }
                mlKit.setAnalyze(false);
                StringBuilder stringBuilder = new StringBuilder();
                for (int i = 0; i < barcodes.size(); ++i) {
                    Barcode barcode = barcodes.get(i);
                    graphicOverlay.add(new BarcodeGraphic(graphicOverlay, barcode));
                    stringBuilder.append("[" + i + "] ").append(barcode.getRawValue()).append("\n");
                }
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

                                tvDialogContent.setText(stringBuilder.toString());
                                Bitmap bitmap = loadBitmapFromView(graphicOverlay);
                                ivDialogContent.setImageBitmap(bitmap);
                                btnDialogCancel.setOnClickListener(new View.OnClickListener() {
                                    @Override
                                    public void onClick(View v) {
                                        customDialog.dismiss();
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

            @Override
            public void onFail(int code, Exception e) {

            }
        });
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

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        Log.d(TAG, "Set facing");
        mlKit.switchCamera(isChecked);
    }

    @Override
    public void onResume() {
        super.onResume();
//        mlKit.onResume();
    }

    /**
     * Stops the camera.
     */
    @Override
    protected void onPause() {
        super.onPause();
//        mlKit.onPause();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
//        mlKit.onDestroy();
    }
}
