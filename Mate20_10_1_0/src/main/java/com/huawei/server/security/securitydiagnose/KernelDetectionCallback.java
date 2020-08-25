package com.huawei.server.security.securitydiagnose;

import android.common.HwFrameworkFactory;
import android.util.Log;
import vendor.huawei.hardware.hwstp.V1_1.IHwStpKernelDetectionCallback;

class KernelDetectionCallback extends IHwStpKernelDetectionCallback.Stub {
    private static final int KERNEL_DESTRUCTION = 6;
    private static final String TAG = "Module Kernel Detection";

    KernelDetectionCallback() {
    }

    @Override // vendor.huawei.hardware.hwstp.V1_1.IHwStpKernelDetectionCallback
    public void onEvent(int uid, int pid, int isMalApp) {
        HwFrameworkFactory.getHwBehaviorCollectManager().sendEvent(6, uid, isMalApp, (String) null, (String) null);
        Log.i(TAG, "sendEvent succeed");
    }
}
