package com.android.server.wifi;

import android.content.Context;
import android.net.wifi.WifiInfo;
import com.android.server.wifi.HwWifiServiceFactory.Factory;
import com.android.server.wifi.MSS.HwMSSHandler;

public class HwWifiServiceFactoryImpl implements Factory {
    private static final String TAG = "HwWifiServiceFactoryImpl";

    public HwWifiServiceManager getHwWifiServiceManager() {
        return HwWifiServiceManagerImpl.getDefault();
    }

    public HwWifiCHRService getHwWifiCHRService() {
        return HwWifiCHRServiceImpl.getInstance();
    }

    public void initWifiCHRService(Context cxt) {
        HwWifiCHRServiceImpl.init(cxt);
    }

    public HwWifiMonitor getHwWifiMonitor() {
        return HwWifiMonitorImpl.getDefault();
    }

    public HwSupplicantHeartBeat getHwSupplicantHeartBeat(WifiStateMachine wifiStateMachine, WifiNative wifiNative) {
        return SupplicantHeartBeat.createHwSupplicantHeartBeat(wifiStateMachine, wifiNative);
    }

    public HwWifiDevicePolicy getHwWifiDevicePolicy() {
        return HwWifiDevicePolicyImpl.getDefault();
    }

    public HwMSSHandlerManager getHwMSSHandlerManager(Context context, WifiNative wifiNative, WifiInfo wifiInfo) {
        return HwMSSHandler.getDefault(context, wifiNative, wifiInfo);
    }
}
