package org.dync.zxingscan;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.Button;

import com.yanzhenjie.permission.AndPermission;
import com.yanzhenjie.permission.PermissionListener;
import com.yanzhenjie.permission.Rationale;
import com.yanzhenjie.permission.RationaleListener;

import org.dync.zxinglibrary.utils.Constant;

import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

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
        mContext = this;
        mActivity = this;
        ButterKnife.bind(this);
    }

    @OnClick({R.id.create_code, R.id.scan_2code, R.id.scan_bar_code, R.id.scan_code})
    public void onViewClicked(View view) {
        Intent intent;
        switch (view.getId()) {
            case R.id.create_code: //生成码
                intent = new Intent(this, CreateCodeActivity.class);
                startActivity(intent);
//                requestPermission(mContext, CommonScanActivity.class, -1);
                break;
            case R.id.scan_2code: //扫描二维码
                requestPermission(mContext, CommonScanActivity.class, Constant.REQUEST_SCAN_MODE_QRCODE_MODE);
                break;
            case R.id.scan_bar_code://扫描条形码
                requestPermission(mContext, CommonScanActivity.class, Constant.REQUEST_SCAN_MODE_BARCODE_MODE);
                break;
            case R.id.scan_code://扫描条形码或者二维码
                requestPermission(mContext, CommonScanActivity.class, Constant.REQUEST_SCAN_MODE_ALL_MODE);
                break;
        }
    }

    private void requestPermission(final Context context, final Class<?> cls, final int mode) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            AndPermission.with(mActivity)
                    .requestCode(REQUEST_CODE_PERMISSION)
                    .permission(Manifest.permission.CAMERA)
                    .callback(new PermissionListener() {
                        @Override
                        public void onSucceed(int requestCode, @NonNull List<String> grantPermissions) {
                            startActivity(context, cls, mode);
                        }

                        @Override
                        public void onFailed(int requestCode, @NonNull List<String> deniedPermissions) {
                            // 用户否勾选了不再提示并且拒绝了权限，那么提示用户到设置中授权。
                            if (AndPermission.hasAlwaysDeniedPermission(mActivity, deniedPermissions)) {
                                // 第一种：用默认的提示语。
                                AndPermission.defaultSettingDialog(mActivity, REQUEST_CODE_SETTING).show();
                            }
                        }
                    })
                    .rationale(new RationaleListener() {
                        @Override
                        public void showRequestPermissionRationale(int requestCode, Rationale rationale) {
                            // 这里使用自定义对话框，如果不想自定义，用AndPermission默认对话框：
                            AndPermission.rationaleDialog(mContext, rationale).show();
                        }
                    })
                    .start();
        }else {
            startActivity(context, cls, mode);
        }
    }

    private static void startActivity(Context context, Class<?> cls, final int mode) {
        Intent intent = new Intent(context, cls);
        if(mode != -1) {
            intent.putExtra(Constant.REQUEST_SCAN_MODE, mode);
        }
        context.startActivity(intent);
    }
}
