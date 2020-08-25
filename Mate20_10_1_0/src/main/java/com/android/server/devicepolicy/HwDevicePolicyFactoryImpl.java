package com.android.server.devicepolicy;

import android.content.Context;
import com.android.server.devicepolicy.HwDevicePolicyFactory;

public class HwDevicePolicyFactoryImpl implements HwDevicePolicyFactory.Factory {
    private static final String TAG = "HwDevicePolicyFactoryImpl";

    public HwDevicePolicyFactory.IHwDevicePolicyManagerService getHuaweiDevicePolicyManagerService() {
        return new HwDevicePolicyManagerServiceImpl();
    }

    public static class HwDevicePolicyManagerServiceImpl implements HwDevicePolicyFactory.IHwDevicePolicyManagerService {
        public DevicePolicyManagerService getInstance(Context context) {
            return new HwDevicePolicyManagerService(context);
        }
    }
}
