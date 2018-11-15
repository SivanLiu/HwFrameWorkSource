package com.android.server.wifi.HwQoE;

import android.content.Context;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Binder;
import android.os.Bundle;
import android.rms.HwSysResManager;
import android.rms.iaware.AwareConstant.ResourceType;
import android.rms.iaware.CollectData;
import com.android.server.hidata.IHidataCallback;
import com.android.server.hidata.hiradio.HwWifiBoost;
import com.android.server.wifi.ABS.HwABSDetectorService;
import com.android.server.wifi.ABS.HwABSUtils;
import com.android.server.wifi.WifiInjector;
import com.android.server.wifi.WifiNative;

public class HwQoEWiFiOptimization implements IHidataCallback {
    private static HwQoEWiFiOptimization mHwQoEWiFiOptimization = null;
    private Context mContext;
    private HwWifiBoost mHwWifiBoost = HwWifiBoost.getInstance(this.mContext);
    private boolean mIsVpnConnected;
    private WifiManager mWifiManager = ((WifiManager) this.mContext.getSystemService("wifi"));
    private WifiNative mWifiNative = WifiInjector.getInstance().getWifiNative();

    private HwQoEWiFiOptimization(Context context) {
        this.mContext = context;
        this.mHwWifiBoost.registWifiBoostCallback(this);
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("HwQoEService: hwQoELimitedSpeed: ");
        stringBuilder.append(enable);
        stringBuilder.append(" mode=");
        stringBuilder.append(mode);
        stringBuilder.append(",VpnConnected:");
        stringBuilder.append(this.mIsVpnConnected);
        HwQoEUtils.logD(stringBuilder.toString());
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
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hwQoEHighPriorityTransmit uid: ");
        stringBuilder.append(uid);
        stringBuilder.append(" enable: ");
        stringBuilder.append(enable);
        stringBuilder.append("type:");
        stringBuilder.append(type);
        HwQoEUtils.logD(stringBuilder.toString());
    }

    public synchronized void hwQoESetMode(int mode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hwQoESetMode:  mode: ");
        stringBuilder.append(mode);
        HwQoEUtils.logD(stringBuilder.toString());
        WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (mWifiInfo != null) {
            this.mWifiNative.gameKOGAdjustSpeed(mWifiInfo.getFrequency(), mode);
        }
    }

    public synchronized void setTXPower(int enable) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("hwQoESetTXPower:  enable: ");
        stringBuilder.append(enable);
        HwQoEUtils.logD(stringBuilder.toString());
        if (this.mWifiNative != null) {
            this.mWifiNative.setPwrBoost(enable);
        }
    }

    public void onSetPMMode(int mode) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSetPMMode:  mode: ");
        stringBuilder.append(mode);
        HwQoEUtils.logD(stringBuilder.toString());
        if ((6 == mode || 7 == mode) && !HwQoEService.getInstance().isPermitUpdateWifiPowerMode(mode)) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("onSetPMMode: Not allow set low power mode:");
            stringBuilder.append(mode);
            HwQoEUtils.logD(stringBuilder.toString());
            return;
        }
        hwQoESetMode(mode);
    }

    public void onSetTXPower(int enable) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onSetTXPower:  enable: ");
        stringBuilder.append(enable);
        HwQoEUtils.logD(stringBuilder.toString());
        setTXPower(enable);
    }

    public void onPauseABSHandover() {
        HwQoEUtils.logD("onPauseABSHandover");
        if (HwABSUtils.getABSEnable()) {
            HwABSDetectorService mHwABSDetectorService = HwABSDetectorService.getInstance();
            if (mHwABSDetectorService != null) {
                mHwABSDetectorService.puaseABSHandover();
            }
        }
    }

    public void onRestartABSHandover() {
        HwQoEUtils.logD("onRestartABSHandover");
        if (HwABSUtils.getABSEnable()) {
            HwABSDetectorService mHwABSDetectorService = HwABSDetectorService.getInstance();
            if (mHwABSDetectorService != null) {
                mHwABSDetectorService.restartABSHandover();
            }
        }
    }

    public RssiPacketCountInfo onGetOTAInfo() {
        return HiDataUtilsManager.getInstance(this.mContext).getOTAInfo();
    }
}
