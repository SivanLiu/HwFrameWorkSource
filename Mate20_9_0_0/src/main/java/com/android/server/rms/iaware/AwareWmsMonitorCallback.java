package com.android.server.rms.iaware;

import android.os.Binder;
import android.os.Bundle;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import com.huawei.android.view.IHwWMDAMonitorCallback.Stub;

class AwareWmsMonitorCallback extends Stub {
    private static final String TAG = "AwareWmsMonitorCallback";

    AwareWmsMonitorCallback() {
    }

    public boolean isResourceNeeded(String resourceid) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager == null) {
            return false;
        }
        return resManager.isResourceNeeded(getReousrceId(resourceid));
    }

    private int getReousrceId(String resourceid) {
        if (resourceid == null) {
            return ResourceType.getReousrceId(ResourceType.RESOURCE_INVALIDE_TYPE);
        }
        if (resourceid.equals("RESOURCE_APPASSOC")) {
            return ResourceType.getReousrceId(ResourceType.RESOURCE_APPASSOC);
        }
        if (resourceid.equals("RESOURCE_SYSLOAD")) {
            return ResourceType.getReousrceId(ResourceType.RESOURCE_SYSLOAD);
        }
        return ResourceType.getReousrceId(ResourceType.RESOURCE_INVALIDE_TYPE);
    }

    public void reportData(String resourceid, long timestamp, Bundle args) {
        HwSysResManager resManager = HwSysResManager.getInstance();
        if (resManager != null && args != null) {
            CollectData data = new CollectData(getReousrceId(resourceid), timestamp, args);
            long id = Binder.clearCallingIdentity();
            resManager.reportData(data);
            Binder.restoreCallingIdentity(id);
        }
    }
}
