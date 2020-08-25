package com.android.server.hidata.mplink;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import com.android.server.hidata.arbitration.IHiDataCHRCallBack;
import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class HwMpLinkManager {
    private static final String TAG = "HiData_MpLinkManager";
    private static HwMpLinkManager mMpLinkManager;
    private Context mContext;
    private HwMpLinkServiceImpl mHwMpLinkServiceImpl = HwMpLinkServiceImpl.getInstance(this.mContext);

    private HwMpLinkManager(Context context) {
        this.mContext = context;
        MpLinkCommonUtils.logI(TAG, false, "init MpLinkManager completed!", new Object[0]);
    }

    public static HwMpLinkManager createInstance(Context context) {
        if (mMpLinkManager == null) {
            mMpLinkManager = new HwMpLinkManager(context);
        }
        return mMpLinkManager;
    }

    public static HwMpLinkManager getInstance() {
        return mMpLinkManager;
    }

    public synchronized boolean isMpLinkConditionSatisfy() {
        MpLinkCommonUtils.logD(TAG, false, "isMpLinkConditionSatisfy", new Object[0]);
        return this.mHwMpLinkServiceImpl.isMpLinkConditionSatisfy();
    }

    public synchronized void notifyIpConfigCompleted() {
        MpLinkCommonUtils.logD(TAG, false, "notifyIpConfigCompleted!", new Object[0]);
        this.mHwMpLinkServiceImpl.notifyIpConfigCompleted();
    }

    public synchronized void registerMpLinkCallback(IMpLinkCallback callback) {
        this.mHwMpLinkServiceImpl.registerMpLinkCallback(callback);
    }

    public synchronized void registerChrCallback(IHiDataCHRCallBack callback) {
        this.mHwMpLinkServiceImpl.registerMpLinkChrCallback(callback);
    }

    public synchronized void requestBindProcessToNetwork(int netId, int uid, MpLinkQuickSwitchConfiguration configuration) {
        this.mHwMpLinkServiceImpl.requestBindProcessToNetwork(netId, uid, configuration);
    }

    public synchronized void requestClearBindProcessToNetwork(int netId, int uid) {
        MpLinkCommonUtils.logD(TAG, false, "requestClearBindProcessToNetwork, uid : %{public}d, netid : %{public}d", new Object[]{Integer.valueOf(uid), Integer.valueOf(netId)});
        this.mHwMpLinkServiceImpl.requestClearBindProcessToNetwork(netId, uid);
    }

    public synchronized void foregroundAppChanged(int uid) {
        MpLinkCommonUtils.logD(TAG, false, "foregroundAppChanged, uid : %{public}d", new Object[]{Integer.valueOf(uid)});
        this.mHwMpLinkServiceImpl.foregroundAppChanged(uid);
    }

    public synchronized void requestWiFiAndCellCoexist(boolean isCoexisted) {
        MpLinkCommonUtils.logD(TAG, false, "requestWiFiAndCellCoexist, coexist : %{public}s", new Object[]{String.valueOf(isCoexisted)});
        this.mHwMpLinkServiceImpl.requestWiFiAndCellCoexist(isCoexisted);
    }

    public synchronized void updateMpLinkAiDevicesList(int type, String packageWhiteList) {
        MpLinkCommonUtils.logD(TAG, false, "updateMpLinkAiDevicesList,", new Object[0]);
    }

    public static boolean isKeepCurrMpLinkConnected(WifiInfo wifiInfo) {
        if (wifiInfo == null || !MpLinkCommonUtils.isSupportMpLink()) {
            return false;
        }
        MpLinkCommonUtils.logD(TAG, false, "wifi score:%{public}d", new Object[]{Integer.valueOf(wifiInfo.score)});
        return false;
    }

    public synchronized void registerRfInterferenceCallback(IRFInterferenceCallback callback) {
        this.mHwMpLinkServiceImpl.registerRfInterferenceCallback(callback);
    }

    public synchronized boolean isAppBindedNetwork() {
        if (this.mHwMpLinkServiceImpl == null) {
            return false;
        }
        return this.mHwMpLinkServiceImpl.isAppBindedNetwork();
    }

    public synchronized NetworkInfo getMpLinkNetworkInfo(NetworkInfo info, int uid) {
        if (this.mHwMpLinkServiceImpl == null) {
            return info;
        }
        return this.mHwMpLinkServiceImpl.getMpLinkNetworkInfo(info, uid);
    }
}
