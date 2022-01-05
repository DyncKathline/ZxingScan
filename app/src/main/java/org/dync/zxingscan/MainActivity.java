package org.dync.zxingscan;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.view.GestureDetector;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

import org.dync.zxinglibrary.ScanManager;
import org.dync.zxinglibrary.utils.PreferencesActivity;
import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static org.dync.zxingscan.CommonScanActivity.REQUEST_SCAN_MODE;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_PERMISSION = 100;
    private static final int REQUEST_CODE_SETTING = 300;
    private Context mContext;
    private Activity mActivity;

    @BindView(R.id.create_code)
    Button createCode;
    @BindView(R.id.scan_2code)
    Button scan2code;
    @BindView(R.id.scan_bar_code)
    Button scanBarCode;
    @BindView(R.id.scan_code)
    Button scanCode;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
//        new GestureDetector();
        mContext = this;
        mActivity = this;
        ButterKnife.bind(this);
    }

    @OnClick({R.id.create_code, R.id.scan_2code, R.id.scan_bar_code, R.id.scan_code, R.id.config, R.id.MLKit})
    public void onViewClicked(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.create_code: //生成码
                intent = new Intent(this, CreateCodeActivity.class);
                startActivity(intent);
//                requestPermission(mContext, CommonScanActivity.class, -1);
                break;
            case R.id.scan_2code: //扫描二维码
                requestPermission(mContext, CommonScanActivity.class, ScanManager.SCANTYPE_QR);
                break;
            case R.id.scan_bar_code://扫描条形码
                requestPermission(mContext, CommonScanActivity.class, ScanManager.SCANTYPE_BARCODE);
                break;
            case R.id.scan_code://扫描条形码或者二维码
                requestPermission(mContext, CommonScanActivity.class, ScanManager.SCANTYPE_ALL);
                break;
            case R.id.config:
                startActivity(new Intent(this, PreferencesActivity.class));
                break;
                case R.id.MLKit:
                startActivity(new Intent(this, LivePreviewActivity.class));
                break;
        }
    }

    private void requestPermission(final Context context, final Class<?> cls, final int mode) {
        startActivity(context, cls, mode);
    }

    private static void startActivity(Context context, Class<?> cls, final int mode) {
        Intent intent = new Intent(context, cls);
        if(mode != -1) {
            intent.putExtra(REQUEST_SCAN_MODE, mode);
        }
        context.startActivity(intent);
    }
}
