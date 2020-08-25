package com.huawei.hwwifiproservice;

import android.content.Context;
import android.net.wifi.ScanResult;
import android.util.wifi.HwHiLog;
import java.util.List;

public class HwIntelligenceWiFiManager {
    private static HwIntelligenceWiFiManager mHwIntelligenceWiFiManager = null;
    private static List<ScanResult> mScanList = null;
    private HwIntelligenceStateMachine mStateMachine;

    private HwIntelligenceWiFiManager(Context context) {
        this.mStateMachine = HwIntelligenceStateMachine.createIntelligenceStateMachine(context);
    }

    public static HwIntelligenceWiFiManager createInstance(Context context) {
        if (mHwIntelligenceWiFiManager == null) {
            mHwIntelligenceWiFiManager = new HwIntelligenceWiFiManager(context);
        }
        return mHwIntelligenceWiFiManager;
    }

    public void start() {
        this.mStateMachine.onStart();
    }

    public void stop() {
        this.mStateMachine.onStop();
    }

    public static synchronized void setWiFiProScanResultList(List<ScanResult> list) {
        synchronized (HwIntelligenceWiFiManager.class) {
            HwHiLog.i(MessageUtil.TAG, false, "setWiFiProScanResultList", new Object[0]);
            mScanList = list;
        }
    }

    public static synchronized List<ScanResult> getWiFiProScanResultList() {
        List<ScanResult> list;
        synchronized (HwIntelligenceWiFiManager.class) {
            list = mScanList;
        }
        return list;
    }
}
