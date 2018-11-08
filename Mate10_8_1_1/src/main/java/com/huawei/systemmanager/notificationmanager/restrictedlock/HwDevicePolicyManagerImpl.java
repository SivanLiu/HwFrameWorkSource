package com.huawei.systemmanager.notificationmanager.restrictedlock;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import java.util.List;

public class HwDevicePolicyManagerImpl implements IDevicePolicyManager {
    private static volatile HwDevicePolicyManagerImpl mInstance = null;

    public static synchronized IDevicePolicyManager getInstance() {
        IDevicePolicyManager iDevicePolicyManager;
        synchronized (HwDevicePolicyManagerImpl.class) {
            if (mInstance == null) {
                mInstance = new HwDevicePolicyManagerImpl();
            }
            iDevicePolicyManager = mInstance;
        }
        return iDevicePolicyManager;
    }

    public List<ComponentName> getActiveAdminsAsUser(Context context, int userid) {
        return ((DevicePolicyManager) context.getSystemService("device_policy")).getActiveAdminsAsUser(userid);
    }
}
