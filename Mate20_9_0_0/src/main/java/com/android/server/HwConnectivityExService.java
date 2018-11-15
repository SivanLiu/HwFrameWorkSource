package com.android.server;

import android.content.Context;
import android.os.Binder;
import android.os.SystemProperties;
import android.util.Slog;
import huawei.android.net.IConnectivityExManager.Stub;

public class HwConnectivityExService extends Stub {
    private static final String TAG = "HwConnectivityExService";
    static String mSmartKeyguardLevel = "normal_level";
    static boolean useCtrlSocket = SystemProperties.getBoolean("ro.config.hw_useCtrlSocket", false);
    private Context mContext;
    private boolean mIsFixedAddress = false;

    public HwConnectivityExService(Context context) {
        this.mContext = context;
    }

    public void setSmartKeyguardLevel(String level) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        setStaticSmartKeyguardLevel(level);
    }

    private static void setStaticSmartKeyguardLevel(String level) {
        mSmartKeyguardLevel = level;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set mSmartKeyguardLevel = ");
        stringBuilder.append(mSmartKeyguardLevel);
        Slog.d(str, stringBuilder.toString());
    }

    public void setApIpv4AddressFixed(boolean isFixed) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Calling pid is ");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(" isFixed ");
        stringBuilder.append(isFixed);
        Slog.d(str, stringBuilder.toString());
        int checkCallingOrSelfPermission = this.mContext.checkCallingOrSelfPermission("com.huawei.wifi.permission.WIFI_APIPV4FIXED");
        this.mContext.getPackageManager();
        if (checkCallingOrSelfPermission != 0) {
            Slog.e(TAG, "No com.huawei.wifi.permission.WIFI_APIPV4FIXED permission");
        } else {
            this.mIsFixedAddress = isFixed;
        }
    }

    public boolean isApIpv4AddressFixed() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Calling pid is ");
        stringBuilder.append(Binder.getCallingPid());
        stringBuilder.append(" isFixed ");
        stringBuilder.append(this.mIsFixedAddress);
        Slog.d(str, stringBuilder.toString());
        int checkCallingOrSelfPermission = this.mContext.checkCallingOrSelfPermission("com.huawei.wifi.permission.WIFI_APIPV4FIXED");
        this.mContext.getPackageManager();
        if (checkCallingOrSelfPermission == 0) {
            return this.mIsFixedAddress;
        }
        Slog.e(TAG, "No com.huawei.wifi.permission.WIFI_APIPV4FIXED permission");
        return false;
    }

    private static void setUseCtrlSocketStatic(boolean flag) {
        useCtrlSocket = flag;
    }

    public void setUseCtrlSocket(boolean flag) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        setUseCtrlSocketStatic(flag);
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("set useCtrlSocket = ");
        stringBuilder.append(useCtrlSocket);
        Slog.d(str, stringBuilder.toString());
    }
}
