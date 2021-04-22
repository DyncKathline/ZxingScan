package org.dync.zxingscan;

import android.content.Context;
import android.os.Bundle;
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
import com.google.mlkit.vision.barcode.BarcodeScannerOptions;
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
        //仅识别二维码
        BarcodeScannerOptions options =
                new BarcodeScannerOptions.Builder()
                        .setBarcodeFormats(
                                Barcode.FORMAT_QR_CODE,
                                Barcode.FORMAT_AZTEC)
                        .build();
        mlKit.setBarcodeFormats(options);
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
                                ivDialogContent.setVisibility(View.GONE);
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

            @Override
            public void onFail(int code, Exception e) {

            }
        });
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
