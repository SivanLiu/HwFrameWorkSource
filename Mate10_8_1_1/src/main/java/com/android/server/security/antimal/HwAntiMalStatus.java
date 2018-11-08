package com.android.server.security.antimal;

import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.os.SystemProperties;
import android.text.TextUtils;
import android.util.Flog;
import android.util.Slog;
import com.android.internal.widget.LockPatternUtils;
import com.android.server.pm.HwCustPackageManagerService;
import com.android.server.security.deviceusage.HwDeviceUsageOEMINFO;
import com.android.server.wifipro.WifiProCommonUtils;
import com.huawei.cust.HwCustUtils;
import java.util.List;
import vendor.huawei.hardware.hwdisplay.displayengine.V1_0.HighBitsCompModeID;

public class HwAntiMalStatus {
    private static final int ANTIMAL_TYPE_ALLOWED_BY_PASSWORD = 10;
    private static final int ANTIMAL_TYPE_ALLOWED_BY_SYSTEMAPP = 11;
    private static final int ANTIMAL_TYPE_NOT_ALLOWED_SET_HOME = 20;
    private static final int ANTIMAL_TYPE_RESTORE_DEFAULT_LAUNCHER = 30;
    private static final long CALL_ENOUGH_TIME = 1800;
    private static final String DEFAULT_HW_LAUNCHER = "com.huawei.android.launcher";
    private static final boolean IS_CHINA_AREA = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    private static final long SCREENON_ENOUGH_TIME = 360000;
    private static final String TAG = "HwAntiMalStatus";
    private static HwCustPackageManagerService mCpms = ((HwCustPackageManagerService) HwCustUtils.createObj(HwCustPackageManagerService.class, new Object[0]));
    private String DEFAULT_CUST_LAUNCHER = "";
    private Context mContext;

    public HwAntiMalStatus(Context ctx) {
        this.mContext = ctx;
    }

    public boolean isAllowedSetHomeActivityForAntiMal(PackageInfo pkgInfo, int userId) {
        return isAllowedSetHomeActivityForAntiMal(pkgInfo, userId, true);
    }

    public boolean isAllowedSetHomeActivityForAntiMal(PackageInfo pkgInfo, int userId, boolean internal) {
        if (this.mContext == null || pkgInfo == null) {
            Slog.d(TAG, "Invalid params.");
            return true;
        } else if (!IS_CHINA_AREA) {
            Slog.d(TAG, "Not in valid area !");
            return true;
        } else if (isSupportHomeScreen()) {
            Slog.d(TAG, "Support home screen.");
            return true;
        } else {
            if (internal) {
                if ((pkgInfo.applicationInfo.flags & 1) == 1) {
                    Slog.d(TAG, "System app.");
                    Flog.bdReport(this.mContext, 128, "{package:" + pkgInfo.packageName + ",type:" + 11 + "}");
                    return true;
                } else if (isCustDefaultLauncher(pkgInfo.packageName)) {
                    Slog.d(TAG, "Cust default launcher.");
                    return true;
                }
            }
            Flog.bdReport(this.mContext, 128, "{package:" + pkgInfo.packageName + ",type:" + 20 + "}");
            Slog.d(TAG, "not allowed to set home activity.");
            return false;
        }
    }

    public void handleUserClearLockForAntiMal(int userId) {
        if (this.mContext == null) {
            Slog.d(TAG, "invalid params.");
        } else if (ActivityManager.getCurrentUser() != 0) {
            Slog.d(TAG, "not owner.");
        } else if (!isNeedRestrictForAntimal(true)) {
            Slog.d(TAG, "no need restrict for antimal.");
        } else if (isDefaultHwLauncher()) {
            Slog.d(TAG, "current launcher is default launcher.");
        } else {
            checkIfDefaultLauncherIsDisabled();
            Slog.d(TAG, "No need to set default launcher.");
        }
    }

    private boolean hasSetPassword() {
        return new LockPatternUtils(this.mContext).isSecure(ActivityManager.getCurrentUser());
    }

    public boolean isNeedRestrictForAntimal(boolean needRestrict) {
        if (!needRestrict) {
            return false;
        }
        HwDeviceUsageOEMINFO mHwDeviceUsageOEMINFO = HwDeviceUsageOEMINFO.getInstance();
        if (mHwDeviceUsageOEMINFO.getScreenOnTime() < SCREENON_ENOUGH_TIME || mHwDeviceUsageOEMINFO.getTalkTime() < CALL_ENOUGH_TIME) {
            return true;
        }
        return false;
    }

    private static Intent getMainIntent() {
        Intent mainIntent = new Intent("android.intent.action.MAIN");
        mainIntent.addCategory("android.intent.category.HOME");
        mainIntent.addCategory("android.intent.category.DEFAULT");
        return mainIntent;
    }

    private boolean isDefaultHwLauncher() {
        ResolveInfo res = this.mContext.getPackageManager().resolveActivity(getMainIntent(), 0);
        if (res == null || res.activityInfo == null || TextUtils.isEmpty(res.activityInfo.packageName)) {
            Slog.e(TAG, "isDefaultHwLauncher param is null.");
            return false;
        } else if (res.activityInfo.packageName.equals("com.huawei.android.launcher") || (!TextUtils.isEmpty(getCustDefaultLauncher()) && res.activityInfo.packageName.equals(getCustDefaultLauncher()))) {
            return true;
        } else {
            return false;
        }
    }

    private boolean isCustDefaultLauncher(String pkgName) {
        if (TextUtils.equals(pkgName, getCustDefaultLauncher())) {
            return true;
        }
        return false;
    }

    private void checkIfDefaultLauncherIsDisabled() {
        PackageManager packageManager = this.mContext.getPackageManager();
        if (packageManager.getApplicationEnabledSetting("com.huawei.android.launcher") == 2) {
            Slog.i(TAG, "Huawei Launcher is Disabled!");
            packageManager.setApplicationEnabledSetting("com.huawei.android.launcher", 1, 1);
        }
        if (!TextUtils.isEmpty(getCustDefaultLauncher()) && packageManager.getApplicationEnabledSetting(getCustDefaultLauncher()) == 2) {
            Slog.i(TAG, "Cust Launcher is Disabled!");
            packageManager.setApplicationEnabledSetting(getCustDefaultLauncher(), 1, 1);
        }
    }

    private void setDefaultLauncher() {
        List<ResolveInfo> resolveInfos = this.mContext.getPackageManager().queryIntentActivities(getMainIntent(), 0);
        if (resolveInfos != null) {
            int i;
            int infoSize = resolveInfos.size();
            for (i = 0; i < infoSize; i++) {
                ResolveInfo resolveInfo = (ResolveInfo) resolveInfos.get(i);
                if (resolveInfo != null) {
                    this.mContext.getPackageManager().clearPackagePreferredActivities(resolveInfo.activityInfo.packageName);
                }
            }
            int find = -1;
            ComponentName[] set = new ComponentName[infoSize];
            for (i = 0; i < infoSize; i++) {
                ResolveInfo info = (ResolveInfo) resolveInfos.get(i);
                set[i] = new ComponentName(info.activityInfo.packageName, info.activityInfo.name);
                if (info.activityInfo.packageName.equals("com.huawei.android.launcher") || (!TextUtils.isEmpty(getCustDefaultLauncher()) && info.activityInfo.packageName.equals(getCustDefaultLauncher()))) {
                    find = i;
                    break;
                }
            }
            if (find != -1) {
                IntentFilter inf = new IntentFilter("android.intent.action.MAIN");
                inf.addCategory("android.intent.category.HOME");
                inf.addCategory("android.intent.category.DEFAULT");
                this.mContext.getPackageManager().addPreferredActivity(inf, HighBitsCompModeID.MODE_COLOR_ENHANCE, set, set[find]);
            }
        }
    }

    private String getCustDefaultLauncher() {
        if (TextUtils.isEmpty(this.DEFAULT_CUST_LAUNCHER)) {
            this.DEFAULT_CUST_LAUNCHER = mCpms.getCustDefaultLauncher(this.mContext);
        }
        return this.DEFAULT_CUST_LAUNCHER;
    }

    public boolean isSupportHomeScreen() {
        return this.mContext.getPackageManager().hasSystemFeature("android.software.home_screen");
    }
}
