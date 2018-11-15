package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.util.Log;

public class HwWifiServiceFactory {
    private static final String TAG = "HwWifiServiceFactory";
    private static final Object mLock = new Object();
    private static volatile Factory obj = null;

    public interface Factory {
        HwMSSHandlerManager getHwMSSHandlerManager(Context context, WifiNative wifiNative, WifiInfo wifiInfo);

        HwSupplicantHeartBeat getHwSupplicantHeartBeat(WifiStateMachine wifiStateMachine, WifiNative wifiNative);

        HwWifiCHRService getHwWifiCHRService();

        HwWifiDevicePolicy getHwWifiDevicePolicy();

        HwWifiMonitor getHwWifiMonitor();

        HwWifiServiceManager getHwWifiServiceManager();

        void initWifiCHRService(Context context);
    }

    private static Factory getImplObject() {
        if (obj == null) {
            synchronized (mLock) {
                if (obj == null) {
                    try {
                        obj = (Factory) Class.forName("com.android.server.wifi.HwWifiServiceFactoryImpl").newInstance();
                    } catch (Exception e) {
                        String str = TAG;
                        StringBuilder stringBuilder = new StringBuilder();
                        stringBuilder.append(": reflection exception is ");
                        stringBuilder.append(e);
                        Log.e(str, stringBuilder.toString());
                    }
                }
            }
        }
        String str2 = TAG;
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("get AllImpl object = ");
        stringBuilder2.append(obj);
        Log.v(str2, stringBuilder2.toString());
        return obj;
    }

    public static HwWifiServiceManager getHwWifiServiceManager() {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwWifiServiceManager();
        }
        return DummyHwWifiServiceManager.getDefault();
    }

    public static HwWifiCHRService getHwWifiCHRService() {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwWifiCHRService();
        }
        return null;
    }

    public static void initWifiCHRService(Context cxt) {
        Factory obj = getImplObject();
        if (obj != null) {
            obj.initWifiCHRService(cxt);
        }
    }

    public static HwWifiMonitor getHwWifiMonitor() {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwWifiMonitor();
        }
        return null;
    }

    public static HwSupplicantHeartBeat getHwSupplicantHeartBeat(WifiStateMachine wifiStateMachine, WifiNative wifiNative) {
        Log.d(TAG, "getHwSupplicantHeartBeat() is callled");
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwSupplicantHeartBeat(wifiStateMachine, wifiNative);
        }
        return null;
    }

    public static HwWifiDevicePolicy getHwWifiDevicePolicy() {
        Log.d(TAG, "getHwWifiDevicePolicy() is callled");
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwWifiDevicePolicy();
        }
        return null;
    }

    public static HwMSSHandlerManager getHwMSSHandlerManager(Context context, WifiNative wifiNative, WifiInfo wifiInfo) {
        Factory obj = getImplObject();
        if (obj != null) {
            return obj.getHwMSSHandlerManager(context, wifiNative, wifiInfo);
        }
        return null;
    }
}
