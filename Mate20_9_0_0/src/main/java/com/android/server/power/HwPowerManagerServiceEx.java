package com.android.server.power;

import android.content.Context;

public final class HwPowerManagerServiceEx implements IHwPowerManagerServiceEx {
    static final String TAG = "HwPowerManagerServiceEx";
    final Context mContext;
    IHwPowerManagerInner mIPowerInner = null;

    public HwPowerManagerServiceEx(IHwPowerManagerInner pms, Context context) {
        this.mIPowerInner = pms;
        this.mContext = context;
    }

    public boolean isAwarePreventScreenOn(String pkgName, String tag) {
        if (pkgName == null || tag == null || this.mIPowerInner == null || this.mIPowerInner.getPowerMonitor() == null) {
            return false;
        }
        return this.mIPowerInner.getPowerMonitor().isAwarePreventScreenOn(pkgName, tag);
    }
}
