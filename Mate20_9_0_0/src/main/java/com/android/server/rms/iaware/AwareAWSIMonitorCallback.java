package com.android.server.rms.iaware;

import android.appwidget.IHwAWSIDAMonitorCallback.Stub;
import android.os.Binder;
import android.os.Bundle;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;

class AwareAWSIMonitorCallback extends Stub {
    private static final String TAG = "AwareAWSIMonitorCallback";

    AwareAWSIMonitorCallback() {
    }

    public void updateWidgetFlushReport(int userId, String packageName) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && resManager.isResourceNeeded(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC))) {
            Bundle args = new Bundle();
            args.putInt("userid", userId);
            args.putString("widget", packageName);
            args.putInt("relationType", 32);
            CollectData data = new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC), System.currentTimeMillis(), args);
            long origId = Binder.clearCallingIdentity();
            resManager.reportData(data);
            Binder.restoreCallingIdentity(origId);
        }
    }
}
