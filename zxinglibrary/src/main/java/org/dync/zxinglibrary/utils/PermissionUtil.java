package org.dync.zxinglibrary.utils;

import android.Manifest;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.fragment.app.FragmentManager;

import org.dync.zxinglibrary.R;

import java.util.ArrayList;
import java.util.List;

/**
 * PermissionUtil.getInstance().with(this).requestPermissions()
 */
public class PermissionUtil {

    private static final String TAG = "PermissionsUtil";

    private PermissionFragment fragment;

    private static PermissionUtil mInstance;

    public static PermissionUtil getInstance() {
        if (mInstance == null) {
            synchronized (PermissionUtil.class) {
                if (mInstance == null) {
                    mInstance = new PermissionUtil();
                }
            }
        }
        return mInstance;
    }

    public PermissionUtil with(@NonNull FragmentActivity activity) {
        fragment = getPermissionsFragment(activity);
        return this;
    }

    public PermissionUtil with(@NonNull Fragment fragmentX) {
        fragment = getPermissionsFragment(fragmentX);
        return this;
    }

    public void showDialogTips(List<String> permission, DialogInterface.OnClickListener onDenied) {
        if (fragment.getContext() == null) {
            return;
        }
        AlertDialog alertDialog = new AlertDialog.Builder(fragment.getContext()).setTitle("权限被禁用").setMessage(
                String.format("您拒绝了相关权限，无法正常使用本功能。请前往 设置->应用管理->%s->权限管理中启用 %s 权限",
                        fragment.getContext().getString(R.string.app_name),
                        listToString(permission)
                )).setCancelable(false)
                .setNegativeButton("返回", onDenied)
                .setPositiveButton("去设置", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        String manufacturer = Build.MANUFACTURER;
                        String model = Build.MODEL;//手机型号，如MI 6，MI 9 SE
                        if(manufacturer.equalsIgnoreCase("xiaomi")) {
                            Intent intent=new Intent();
                            //model.toUpperCase().contains("MI 6")
                            intent.setAction("miui.intent.action.APP_PERM_EDITOR");
                            intent.putExtra("extra_pkgname", fragment.getContext().getPackageName());
                            fragment.getContext().startActivity(intent);
                        }else {
                            //第二个参数为包名
                            Uri uri = Uri.fromParts("package", fragment.getContext().getPackageName(), null);
                            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                            intent.setData(uri);
                            fragment.getContext().startActivity(intent);
                        }
                    }
                }).create();
        alertDialog.show();

    }

    public static String listToString(List<String> list) {
        StringBuilder builder = new StringBuilder();
        int size = list.size();
        for (int i = 0; i < size; i++) {
            if (i < size - 1) {
                builder.append(list.get(i)).append(",");
            } else {
                builder.append(list.get(i));
            }
        }
        return builder.toString();
    }

    private PermissionFragment getPermissionsFragment(FragmentActivity activity) {
        PermissionFragment fragment = (PermissionFragment) activity.getSupportFragmentManager().findFragmentByTag(TAG);
        boolean isNewInstance = fragment == null;
        if (isNewInstance) {
            fragment = new PermissionFragment();
            FragmentManager fragmentManager = activity.getSupportFragmentManager();
            fragmentManager
                    .beginTransaction()
                    .add(fragment, TAG)
                    .commitNow();
        }

        return fragment;
    }

    private PermissionFragment getPermissionsFragment(Fragment fragmentX) {
        PermissionFragment fragment = (PermissionFragment) fragmentX.getChildFragmentManager().findFragmentByTag(TAG);
        boolean isNewInstance = fragment == null;
        if (isNewInstance) {
            fragment = new PermissionFragment();
            FragmentManager fragmentManager = fragmentX.getChildFragmentManager();
            fragmentManager
                    .beginTransaction()
                    .add(fragment, TAG)
                    .commitNow();
        }

        return fragment;
    }

    /**
     * 外部调用申请权限
     *
     * @param permissions 申请的权限
     * @param listener    监听权限接口
     */
    public void requestPermissions(String[] permissions, PermissionListener listener) {
        fragment.setListener(listener);
        fragment.requestPermissions(permissions);
    }

    public interface PermissionListener {
        void onGranted();

        void onDenied(List<String> deniedPermission);

        void onShouldShowRationale(List<String> deniedPermission);
    }

    public static class PermissionFragment extends Fragment {
        /**
         * 申请权限的requestCode
         */
        private static final int PERMISSIONS_REQUEST_CODE = 1;

        /**
         * 权限监听接口
         */
        private PermissionListener listener;

        public void setListener(PermissionListener listener) {
            this.listener = listener;
        }

        @Override
        public void onCreate(@Nullable Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setRetainInstance(true);
        }

        /**
         * 申请权限
         *
         * @param permissions 需要申请的权限
         */
        public void requestPermissions(@NonNull String[] permissions) {
            List<String> requestPermissionList = new ArrayList<>();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                //找出所有未授权的权限
                for (String permission : permissions) {
                    if (ContextCompat.checkSelfPermission(getContext(), permission) != PackageManager.PERMISSION_GRANTED) {
                        requestPermissionList.add(permission);
                    }
                }
                if (requestPermissionList.isEmpty()) {
                    //已经全部授权
                    permissionAllGranted();
                } else {
                    //申请授权
                    requestPermissions(requestPermissionList.toArray(new String[requestPermissionList.size()]), PERMISSIONS_REQUEST_CODE);
                }
            } else {
                //已经全部授权
                permissionAllGranted();
            }

        }

        /**
         * fragment回调处理权限的结果
         *
         * @param requestCode  请求码 要等于申请时候的请求码
         * @param permissions  申请的权限
         * @param grantResults 对应权限的处理结果
         */
        @Override
        public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
            super.onRequestPermissionsResult(requestCode, permissions, grantResults);
            if (requestCode != PERMISSIONS_REQUEST_CODE) {
                return;
            }

            if (grantResults.length > 0) {
                List<String> deniedPermissionList = new ArrayList<>();
                for (int i = 0; i < grantResults.length; i++) {
                    if (grantResults[i] != PackageManager.PERMISSION_GRANTED) {
                        deniedPermissionList.add(permissions[i]);
                    }
                }

                if (deniedPermissionList.isEmpty()) {
                    //已经全部授权
                    permissionAllGranted();
                } else {

                    //勾选了对话框中”Don’t ask again”的选项, 返回false
                    for (String deniedPermission : deniedPermissionList) {
                        boolean flag = shouldShowRequestPermissionRationale(deniedPermission);
                        if (flag) {
                            //拒绝授权
                            permissionShouldShowRationale(deniedPermissionList);
                            return;
                        }
                    }
                    //拒绝授权
                    permissionHasDenied(deniedPermissionList);

                }


            }

        }

        /**
         * 权限全部已经授权
         */
        private void permissionAllGranted() {
            if (listener != null) {
                listener.onGranted();
            }
        }

        /**
         * 有权限被拒绝
         *
         * @param deniedList 被拒绝的权限
         */
        private void permissionHasDenied(List<String> deniedList) {
            if (listener != null) {
                listener.onDenied(deniedList);
            }
        }

        /**
         * 权限被拒绝并且勾选了不在询问
         *
         * @param deniedList 勾选了不在询问的权限
         */
        private void permissionShouldShowRationale(List<String> deniedList) {
            if (listener != null) {
                listener.onShouldShowRationale(deniedList);
            }
        }
    }

    /**
     * 是否是 Android 11 及以上版本
     */
    static boolean isAndroid11() {
        return Build.VERSION.SDK_INT >= 11;//Build.VERSION_CODES.R
    }

    /**
     * 是否是 Android 10 及以上版本
     */
    static boolean isAndroid10() {
        return Build.VERSION.SDK_INT >= 10;//Build.VERSION_CODES.Q
    }

    /**
     * 是否是 Android 9.0 及以上版本
     */
    static boolean isAndroid9() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.P;
    }

    /**
     * 是否是 Android 8.0 及以上版本
     */
    static boolean isAndroid8() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O;
    }

    /**
     * 是否是 Android 7.0 及以上版本
     */
    static boolean isAndroid7() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.N;
    }

    /**
     * 是否是 Android 6.0 及以上版本
     */
    static boolean isAndroid6() {
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.M;
    }

    /**
     * 判断某个权限集合是否包含特殊权限
     */
    static boolean containsSpecialPermission(List<String> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return false;
        }

        for (String permission : permissions) {
            if (isSpecialPermission(permission)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 判断某个权限是否是特殊权限
     */
    static boolean isSpecialPermission(String permission) {
        return Permission.MANAGE_EXTERNAL_STORAGE.equals(permission) ||
                Permission.REQUEST_INSTALL_PACKAGES.equals(permission) ||
                Permission.SYSTEM_ALERT_WINDOW.equals(permission) ||
                Permission.NOTIFICATION_SERVICE.equals(permission) ||
                Permission.WRITE_SETTINGS.equals(permission);
    }

    public static class Permission {
        /**
         * 外部存储权限（特殊权限，需要 Android 11 及以上）
         */
        public static final String MANAGE_EXTERNAL_STORAGE = "android.permission.MANAGE_EXTERNAL_STORAGE";

        /**
         * 安装应用权限（特殊权限，需要 Android 8.0 及以上）
         */
        public static final String REQUEST_INSTALL_PACKAGES = "android.permission.REQUEST_INSTALL_PACKAGES";

        /**
         * 通知栏权限（特殊权限，需要 Android 6.0 及以上，注意此权限不需要在清单文件中注册也能申请）
         */
        public static final String NOTIFICATION_SERVICE = "android.permission.NOTIFICATION_SERVICE";

        /**
         * 悬浮窗权限（特殊权限，需要 Android 6.0 及以上）
         */
        public static final String SYSTEM_ALERT_WINDOW = "android.permission.SYSTEM_ALERT_WINDOW";

        /**
         * 系统设置权限（特殊权限，需要 Android 6.0 及以上）
         */
        public static final String WRITE_SETTINGS = "android.permission.WRITE_SETTINGS";
    }

    public static class PermissionSettingPage {
        /**
         * 根据传入的权限自动选择最合适的权限设置页
         */
        public static Intent getSmartPermissionIntent(Context context, List<String> deniedPermissions) {
            // 如果失败的权限里面不包含特殊权限
            if (deniedPermissions == null || deniedPermissions.isEmpty() || !PermissionUtil.containsSpecialPermission(deniedPermissions)) {
                return PermissionSettingPage.getApplicationDetailsIntent(context);
            }

            // 如果当前只有一个权限被拒绝了
            if (deniedPermissions.size() == 1) {

                String permission = deniedPermissions.get(0);
                if (Permission.MANAGE_EXTERNAL_STORAGE.equals(permission)) {
                    return getStoragePermissionIntent(context);
                }

                if (Permission.REQUEST_INSTALL_PACKAGES.equals(permission)) {
                    return getInstallPermissionIntent(context);
                }

                if (Permission.SYSTEM_ALERT_WINDOW.equals(permission)) {
                    return getWindowPermissionIntent(context);
                }

                if (Permission.NOTIFICATION_SERVICE.equals(permission)) {
                    return getNotifyPermissionIntent(context);
                }

                if (Permission.WRITE_SETTINGS.equals(permission)) {
                    return getSettingPermissionIntent(context);
                }

                return getApplicationDetailsIntent(context);
            }

            if (PermissionUtil.isAndroid11() && deniedPermissions.size() == 3 &&
                    (deniedPermissions.contains(Permission.MANAGE_EXTERNAL_STORAGE) &&
                            deniedPermissions.contains(Manifest.permission.READ_EXTERNAL_STORAGE) &&
                            deniedPermissions.contains(Manifest.permission.WRITE_EXTERNAL_STORAGE))) {
                return getStoragePermissionIntent(context);
            }

            return PermissionSettingPage.getApplicationDetailsIntent(context);
        }

        /**
         * 获取应用详情界面意图
         */
        static Intent getApplicationDetailsIntent(Context context) {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + context.getPackageName()));
            return intent;
        }

        /**
         * 获取安装权限设置界面意图
         */
        static Intent getInstallPermissionIntent(Context context) {
            Intent intent = null;
            if (PermissionUtil.isAndroid8()) {
                intent = new Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
            }
            if (intent == null || !areActivityIntent(context, intent)) {
                intent = getApplicationDetailsIntent(context);
            }
            return intent;
        }

        /**
         * 获取悬浮窗权限设置界面意图
         */
        static Intent getWindowPermissionIntent(Context context) {
            Intent intent = null;
            if (PermissionUtil.isAndroid6()) {
                intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
                // 在 Android 11 上面不能加包名跳转，因为就算加了也没有效果
                // 还有人反馈在 Android 11 的 TV 模拟器上会出现崩溃的情况
                // https://developer.android.google.cn/reference/android/provider/Settings#ACTION_MANAGE_OVERLAY_PERMISSION
                if (!PermissionUtil.isAndroid11()) {
                    intent.setData(Uri.parse("package:" + context.getPackageName()));
                }
            }

            if (intent == null || !areActivityIntent(context, intent)) {
                intent = getApplicationDetailsIntent(context);
            }
            return intent;
        }

        /**
         * 获取通知栏权限设置界面意图
         */
        static Intent getNotifyPermissionIntent(Context context) {
            Intent intent = null;
            if (PermissionUtil.isAndroid8()) {
                intent = new Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
                intent.putExtra(Settings.EXTRA_APP_PACKAGE, context.getPackageName());
                //intent.putExtra(Settings.EXTRA_CHANNEL_ID, context.getApplicationInfo().uid);
            }
            if (intent == null || !areActivityIntent(context, intent)) {
                intent = getApplicationDetailsIntent(context);
            }
            return intent;
        }

        /**
         * 获取系统设置权限界面意图
         */
        static Intent getSettingPermissionIntent(Context context) {
            Intent intent = null;
            if (PermissionUtil.isAndroid6()) {
                intent = new Intent(Settings.ACTION_MANAGE_WRITE_SETTINGS);
                intent.setData(Uri.parse("package:" + context.getPackageName()));
            }
            if (intent == null || !areActivityIntent(context, intent)) {
                intent = getApplicationDetailsIntent(context);
            }
            return intent;
        }

        /**
         * 获取存储权限设置界面意图
         */
        static Intent getStoragePermissionIntent(Context context) {
            Intent intent = null;
            if (PermissionUtil.isAndroid11()) {
                intent = new Intent("android.settings.MANAGE_APP_ALL_FILES_ACCESS_PERMISSION");//Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION
                intent.setData(Uri.parse("package:" + context.getPackageName()));
            }
            if (intent == null || !areActivityIntent(context, intent)) {
                intent = getApplicationDetailsIntent(context);
            }
            return intent;
        }

        /**
         * 判断这个意图的 Activity 是否存在
         */
        private static boolean areActivityIntent(Context context, Intent intent) {
            return !context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY).isEmpty();
        }
    }

}
