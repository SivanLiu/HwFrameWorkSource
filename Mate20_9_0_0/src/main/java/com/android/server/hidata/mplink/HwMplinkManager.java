package com.android.server.hidata.mplink;

import android.content.Context;
import android.net.NetworkInfo;
import android.net.wifi.WifiInfo;
import com.android.server.hidata.arbitration.IHiDataCHRCallBack;
import huawei.android.net.hwmplink.MpLinkCommonUtils;

public class HwMplinkManager {
    private static final String TAG = "HiData_MplinkManager";
    private static HwMplinkManager mMplinkManager;
    private Context mContext;
    private HwMpLinkServiceImpl mHwMpLinkServiceImpl = HwMpLinkServiceImpl.getInstance(this.mContext);

    public static HwMplinkManager createInstance(Context context) {
        if (mMplinkManager == null) {
            mMplinkManager = new HwMplinkManager(context);
        }
        return mMplinkManager;
    }

    public static HwMplinkManager getInstance() {
        return mMplinkManager;
    }

    private HwMplinkManager(Context context) {
        this.mContext = context;
        MpLinkCommonUtils.logI(TAG, "init MplinkManager completed!");
    }

    public synchronized boolean isMpLinkConditionSatisfy() {
        MpLinkCommonUtils.logD(TAG, "isMpLinkConditionSatisfy");
        return this.mHwMpLinkServiceImpl.isMpLinkConditionSatisfy();
    }

    public synchronized void notifyIpConfigCompleted() {
        MpLinkCommonUtils.logD(TAG, "notifyIpConfigCompleted!");
        this.mHwMpLinkServiceImpl.notifyIpConfigCompleted();
    }

    public synchronized void registMpLinkCallback(IMpLinkCallback callback) {
        this.mHwMpLinkServiceImpl.registMpLinkCallback(callback);
    }

    public synchronized void registCHRCallback(IHiDataCHRCallBack callback) {
        this.mHwMpLinkServiceImpl.registMpLinkCHRCallback(callback);
    }

    @Deprecated
    public synchronized void requestBindProcessToNetwork(int netid, int uid, int type) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestBindProcessToNetwork, uid : ");
        stringBuilder.append(uid);
        stringBuilder.append(", netid : ");
        stringBuilder.append(netid);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        this.mHwMpLinkServiceImpl.requestBindProcessToNetwork(netid, uid, type);
    }

    public synchronized void requestBindProcessToNetwork(int netid, int uid, MpLinkQuickSwitchConfiguration configuration) {
        this.mHwMpLinkServiceImpl.requestBindProcessToNetwork(netid, uid, configuration);
    }

    public synchronized void requestClearBindProcessToNetwork(int netid, int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestClearBindProcessToNetwork, uid : ");
        stringBuilder.append(uid);
        stringBuilder.append(", netid : ");
        stringBuilder.append(netid);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        this.mHwMpLinkServiceImpl.requestClearBindProcessToNetwork(netid, uid);
    }

    public synchronized void foregroundAppChanged(int uid) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("foregroundAppChanged, uid : ");
        stringBuilder.append(uid);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        this.mHwMpLinkServiceImpl.foregroundAppChanged(uid);
    }

    public synchronized void requestWiFiAndCellCoexist(boolean coexist) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("requestWiFiAndCellCoexist, coexist : ");
        stringBuilder.append(coexist);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        this.mHwMpLinkServiceImpl.requestWiFiAndCellCoexist(coexist);
    }

    public synchronized void updateMplinkAiDevicesList(int type, String packageWhiteList) {
        MpLinkCommonUtils.logD(TAG, "updateMplinkAiDevicesList,");
    }

    public static boolean isKeepCurrMplinkConnected(WifiInfo wifiInfo) {
        if (wifiInfo == null || !MpLinkCommonUtils.isSupportMpLink()) {
            return false;
        }
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("wifi score:");
        stringBuilder.append(wifiInfo.score);
        MpLinkCommonUtils.logD(str, stringBuilder.toString());
        if (wifiInfo.score != 35) {
            return false;
        }
        MpLinkCommonUtils.logD(TAG, "isKeepCurrMplinkConnected");
        return true;
    }

    public synchronized void registRFInterferenceCallback(IRFInterferenceCallback callback) {
        this.mHwMpLinkServiceImpl.registRFInterferenceCallback(callback);
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
