package com.huawei.hwwifiproservice;

import android.content.Context;
import android.os.Bundle;
import com.android.server.wifi.wifipro.HwWifiProServiceProxy;

public class WifiManagerEx {
    private static boolean isInitialized = false;
    private static Context mContext = null;
    private static HwWifiProServiceProxy mHwWifiProServiceProxy;

    public static synchronized void init(Context context) {
        synchronized (WifiManagerEx.class) {
            if (context != null) {
                if (!isInitialized) {
                    mContext = context;
                    mHwWifiProServiceProxy = HwWifiProServiceProxy.createHwWifiProServiceProxy(mContext);
                    isInitialized = true;
                }
            }
        }
    }

    public static synchronized Bundle ctrlHwWifiNetwork(String pkgName, int interfaceId, Bundle data) {
        synchronized (WifiManagerEx.class) {
            if (!isInitialized) {
                return null;
            }
            return mHwWifiProServiceProxy.ctrlHwWifiNetwork(pkgName, interfaceId, data);
        }
    }
}
