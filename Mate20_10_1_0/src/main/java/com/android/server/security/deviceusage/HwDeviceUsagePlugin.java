package com.android.server.security.deviceusage;

import android.content.Context;
import android.os.IBinder;
import android.os.SystemProperties;
import android.util.Log;
import android.util.Slog;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import com.android.server.security.core.IHwSecurityPlugin;
import com.android.server.wifipro.WifiProCommonUtils;
import huawei.android.security.IHwDeviceUsagePlugin;

public class HwDeviceUsagePlugin extends IHwDeviceUsagePlugin.Stub implements IHwSecurityPlugin {
    public static final Creator CREATOR = new Creator() {
        /* class com.android.server.security.deviceusage.HwDeviceUsagePlugin.AnonymousClass1 */

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public IHwSecurityPlugin createPlugin(Context context) {
            if (HwDeviceUsagePlugin.IS_HW_DEBUG) {
                Slog.d(HwDeviceUsagePlugin.TAG, "createPlugin");
            }
            return new HwDeviceUsagePlugin(context);
        }

        @Override // com.android.server.security.core.IHwSecurityPlugin.Creator
        public String getPluginPermission() {
            return HwDeviceUsagePlugin.MANAGE_USE_SECURITY;
        }
    };
    private static final int INVALID_TIME = -1;
    private static final boolean IS_CHINA_RELEASE_VERSION = "CN".equalsIgnoreCase(SystemProperties.get(WifiProCommonUtils.KEY_PROP_LOCALE, ""));
    /* access modifiers changed from: private */
    public static final boolean IS_HW_DEBUG = (Log.HWINFO || (Log.HWModuleLog && Log.isLoggable(TAG, 4)));
    private static final boolean IS_PHONE = MemoryConstant.MEM_SCENE_DEFAULT.equals(SystemProperties.get("ro.build.characteristics"));
    private static final String MANAGE_USE_SECURITY = "com.huawei.permission.MANAGE_USE_SECURITY";
    private static final String TAG = "HwDeviceUsagePlugin";
    private final Context mContext;
    private HwDeviceUsageCollection mHwDeviceUsageCollection;
    private ActivationMonitor mMonitor = new ActivationMonitor(this.mContext);

    public HwDeviceUsagePlugin(Context context) {
        this.mContext = context;
    }

    /* JADX DEBUG: Multi-variable search result rejected for r0v0, resolved type: com.android.server.security.deviceusage.HwDeviceUsagePlugin */
    /* JADX WARN: Multi-variable type inference failed */
    @Override // com.android.server.security.core.IHwSecurityPlugin
    public IBinder asBinder() {
        return this;
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStart() {
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "HwDeviceUsagePlugin is Start");
        }
        this.mMonitor.start();
        this.mHwDeviceUsageCollection = new HwDeviceUsageCollection(this.mContext);
        if (IS_CHINA_RELEASE_VERSION && IS_PHONE && this.mHwDeviceUsageCollection.isOpenFlagSet()) {
            this.mHwDeviceUsageCollection.onStart();
        }
    }

    @Override // com.android.server.security.core.IHwSecurityPlugin
    public void onStop() {
        if (IS_HW_DEBUG) {
            Slog.d(TAG, "HwDeviceUsagePlugin onStop");
        }
    }

    public long getScreenOnTime() {
        checkPermission(MANAGE_USE_SECURITY);
        HwDeviceUsageCollection hwDeviceUsageCollection = this.mHwDeviceUsageCollection;
        if (hwDeviceUsageCollection == null) {
            return -1;
        }
        return hwDeviceUsageCollection.getScreenOnTime();
    }

    public long getChargeTime() {
        checkPermission(MANAGE_USE_SECURITY);
        HwDeviceUsageCollection hwDeviceUsageCollection = this.mHwDeviceUsageCollection;
        if (hwDeviceUsageCollection == null) {
            return -1;
        }
        return hwDeviceUsageCollection.getChargeTime();
    }

    public long getTalkTime() {
        checkPermission(MANAGE_USE_SECURITY);
        HwDeviceUsageCollection hwDeviceUsageCollection = this.mHwDeviceUsageCollection;
        if (hwDeviceUsageCollection == null) {
            return -1;
        }
        return hwDeviceUsageCollection.getTalkTime();
    }

    public long getFristUseTime() {
        checkPermission(MANAGE_USE_SECURITY);
        HwDeviceUsageCollection hwDeviceUsageCollection = this.mHwDeviceUsageCollection;
        if (hwDeviceUsageCollection == null) {
            return -1;
        }
        return hwDeviceUsageCollection.getFirstUseTime();
    }

    public void setOpenFlag(int flag) {
        checkPermission(MANAGE_USE_SECURITY);
        HwDeviceUsageCollection hwDeviceUsageCollection = this.mHwDeviceUsageCollection;
        if (hwDeviceUsageCollection != null) {
            hwDeviceUsageCollection.setOpenFlag(flag);
        }
    }

    public void setScreenOnTime(long time) {
        checkPermission(MANAGE_USE_SECURITY);
        HwDeviceUsageCollection hwDeviceUsageCollection = this.mHwDeviceUsageCollection;
        if (hwDeviceUsageCollection != null) {
            hwDeviceUsageCollection.setScreenOnTime(time);
        }
    }

    public void setChargeTime(long time) {
        checkPermission(MANAGE_USE_SECURITY);
        HwDeviceUsageCollection hwDeviceUsageCollection = this.mHwDeviceUsageCollection;
        if (hwDeviceUsageCollection != null) {
            hwDeviceUsageCollection.setChargeTime(time);
        }
    }

    public void setTalkTime(long time) {
        checkPermission(MANAGE_USE_SECURITY);
        HwDeviceUsageCollection hwDeviceUsageCollection = this.mHwDeviceUsageCollection;
        if (hwDeviceUsageCollection != null) {
            hwDeviceUsageCollection.setTalkTime(time);
        }
    }

    public void setFristUseTime(long time) {
        checkPermission(MANAGE_USE_SECURITY);
        HwDeviceUsageCollection hwDeviceUsageCollection = this.mHwDeviceUsageCollection;
        if (hwDeviceUsageCollection != null) {
            hwDeviceUsageCollection.setFirstUseTime(time);
        }
    }

    private void checkPermission(String permission) {
        Context context = this.mContext;
        context.enforceCallingOrSelfPermission(permission, "Must have " + permission + " permission.");
    }

    public boolean isDeviceActivated() {
        checkPermission(MANAGE_USE_SECURITY);
        if (this.mMonitor.isActivated()) {
            return true;
        }
        this.mMonitor.restart(ActivationMonitor.ACTIVATION_TIME);
        return false;
    }

    public void resetActivation() {
        checkPermission(MANAGE_USE_SECURITY);
        this.mMonitor.resetActivation();
    }

    public void detectActivationWithDuration(long duration) {
        checkPermission(MANAGE_USE_SECURITY);
        this.mMonitor.restart(duration);
    }
}
