package com.android.server;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.os.Binder;
import android.os.SystemProperties;
import android.util.Slog;
import com.android.server.intellicom.common.HwAppStateObserver;
import com.android.server.intellicom.common.HwSettingsObserver;
import com.android.server.intellicom.common.NetLinkManager;
import com.android.server.intellicom.common.NetRouteManager;
import com.android.server.intellicom.smartdualcard.SmartDualCardRecommendNotify;
import huawei.android.net.IConnectivityExManager;

public class HwConnectivityExService extends IConnectivityExManager.Stub {
    private static final boolean IS_NR_SLICES_SUPPORTED = HwFrameworkFactory.getHwInnerTelephonyManager().isNrSlicesSupported();
    private static final boolean IS_SMART_DUAL_CARD_PROP_ENABLE = SystemProperties.getBoolean("ro.odm.smart_dual_card", false);
    private static final String TAG = "HwConnectivityExService";
    private static final int UNBIND_NETID = 0;
    static String mSmartKeyguardLevel = "normal_level";
    static boolean useCtrlSocket = SystemProperties.getBoolean("ro.config.hw_useCtrlSocket", false);
    private Context mContext;
    private boolean mIsFixedAddress = false;

    public HwConnectivityExService(Context context) {
        this.mContext = context;
        if (IS_NR_SLICES_SUPPORTED || IS_SMART_DUAL_CARD_PROP_ENABLE) {
            HwAppStateObserver.getInstance().register(this.mContext);
        }
        if (IS_SMART_DUAL_CARD_PROP_ENABLE) {
            HwSettingsObserver.getInstance().init(this.mContext);
            NetLinkManager.getInstance().init(this.mContext);
            SmartDualCardRecommendNotify.getInstance().init(this.mContext);
        }
    }

    public void setSmartKeyguardLevel(String level) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        setStaticSmartKeyguardLevel(level);
    }

    private static void setStaticSmartKeyguardLevel(String level) {
        mSmartKeyguardLevel = level;
        Slog.d(TAG, "set mSmartKeyguardLevel = " + mSmartKeyguardLevel);
    }

    public void setApIpv4AddressFixed(boolean isFixed) {
        Slog.d(TAG, "Calling pid is " + Binder.getCallingPid() + " isFixed " + isFixed);
        int checkCallingOrSelfPermission = this.mContext.checkCallingOrSelfPermission("com.huawei.wifi.permission.WIFI_APIPV4FIXED");
        this.mContext.getPackageManager();
        if (checkCallingOrSelfPermission != 0) {
            Slog.e(TAG, "No com.huawei.wifi.permission.WIFI_APIPV4FIXED permission");
        } else {
            this.mIsFixedAddress = isFixed;
        }
    }

    public boolean isApIpv4AddressFixed() {
        Slog.d(TAG, "Calling pid is " + Binder.getCallingPid() + " isFixed " + this.mIsFixedAddress);
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
        Slog.d(TAG, "set useCtrlSocket.");
    }

    public boolean bindUidProcessToNetwork(int netId, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        if (netId != 0) {
            return NetRouteManager.getInstance().bindUidProcessToNetwork(netId, uid);
        }
        return NetRouteManager.getInstance().unbindUidProcessToNetwork(uid);
    }

    public boolean unbindAllUidProcessToNetwork(int netId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return NetRouteManager.getInstance().unbindAllUidProcessToNetwork(netId);
    }

    public boolean isUidProcessBindedToNetwork(int netId, int uid) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return NetRouteManager.getInstance().isUidProcessBindedToNetwork(netId, uid);
    }

    public boolean isAllUidProcessUnbindToNetwork(int netId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return NetRouteManager.getInstance().isAllUidProcessUnbindToNetwork(netId);
    }

    public int getNetIdBySlotId(int slotId) {
        this.mContext.enforceCallingOrSelfPermission("android.permission.CONNECTIVITY_INTERNAL", TAG);
        return NetLinkManager.getInstance().getNetIdBySlotId(slotId);
    }
}
