package com.android.server.wifi.HwQoE;

import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;

public class HwQoEWiFiOptimization {
    private static HwQoEWiFiOptimization mHwQoEWiFiOptimization = null;
    private Context mContext;
    private boolean mIsVpnConnected;
    private WifiManager mWifiManager = ((WifiManager) this.mContext.getSystemService("wifi"));
    private WifiNative mWifiNative = WifiInjector.getInstance().getWifiNative();

    private HwQoEWiFiOptimization(Context context) {
        this.mContext = context;
    }

    public static synchronized HwQoEWiFiOptimization getInstance(Context context) {
        HwQoEWiFiOptimization hwQoEWiFiOptimization;
        synchronized (HwQoEWiFiOptimization.class) {
            if (mHwQoEWiFiOptimization == null) {
                mHwQoEWiFiOptimization = new HwQoEWiFiOptimization(context);
            }
            hwQoEWiFiOptimization = mHwQoEWiFiOptimization;
        }
        return hwQoEWiFiOptimization;
    }

    public synchronized void updateVNPStateChanged(boolean isVpnConnected) {
        this.mIsVpnConnected = isVpnConnected;
        if (this.mIsVpnConnected) {
            hwQoELimitedSpeed(0, 1);
        }
    }

    public synchronized void hwQoELimitedSpeed(int enable, int mode) {
        HwQoEUtils.logD("HwQoEService: hwQoELimitedSpeed: " + enable + " mode=" + mode + ",VpnConnected:" + this.mIsVpnConnected);
        if (this.mIsVpnConnected && enable == 1) {
            HwQoEUtils.logD("Vpn Connected,can not limit speed!");
            return;
        }
        Bundle args = new Bundle();
        args.putInt("enbale", enable);
        args.putInt("mode", mode);
        CollectData data = new CollectData(ResourceType.getReousrceId(ResourceType.RESOURCE_NET_MANAGE), System.currentTimeMillis(), args);
        long id = Binder.clearCallingIdentity();
        HwSysResManager.getInstance().reportData(data);
        Binder.restoreCallingIdentity(id);
    }

    public synchronized void hwQoEHighPriorityTransmit(int uid, int type, int enable) {
        HwQoEUtils.logD("hwQoEHighPriorityTransmit uid: " + uid + " enable: " + enable);
        HwQoEJNIAdapter.getInstance().setDpiMarkRule(uid, type, enable);
    }

    public synchronized void hwQoESetMode(int mode) {
        HwQoEUtils.logD("hwQoESetMode:  mode: " + mode);
        WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (mWifiInfo != null) {
            this.mWifiNative.gameKOGAdjustSpeed(mWifiInfo.getFrequency(), mode);
        }
    }

    public synchronized void setTXPower(int enable) {
        HwQoEUtils.logD("hwQoESetTXPower:  enable: " + enable);
        if (this.mWifiNative != null) {
            this.mWifiNative.setPwrBoost(enable);
        }
    }
}
