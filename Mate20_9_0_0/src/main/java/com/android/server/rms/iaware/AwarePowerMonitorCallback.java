package com.android.server.rms.iaware;

import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import com.huawei.android.os.IHwPowerDAMonitorCallback.Stub;

class AwarePowerMonitorCallback extends Stub {
    private static final String TAG = "AwarePowerDAMonitorCallback";

    AwarePowerMonitorCallback() {
    }

    public boolean isAwarePreventScreenOn(String pkgName, String tag) {
        return DevSchedFeatureRT.isAwarePreventWakelockScreenOn(pkgName, tag);
    }
}
