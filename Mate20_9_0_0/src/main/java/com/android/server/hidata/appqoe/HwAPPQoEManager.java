package com.android.server.hidata.appqoe;

import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.provider.Settings.System;
import android.util.Log;
import com.android.server.hidata.channelqoe.HwChannelQoEManager;
import com.android.server.hidata.channelqoe.IChannelQoECallback;

public class HwAPPQoEManager implements IChannelQoECallback {
    private static final String TAG = "HiData_HwAPPQoEManager";
    private static final int USER_HANDOVER_TO_WIFI = 9;
    private static HwAPPQoEManager mHwAPPQoEManager = null;
    IHwAPPQoECallback mBrainCallback = null;
    private Context mContext;
    private HwAPPQoEStateMachine mHwAPPQoEStateMachine = null;
    private HwChannelQoEManager mHwChannelQoEManager = null;
    IHwAPPQoECallback mWaveMappingCallback = null;

    private HwAPPQoEManager(Context context) {
        this.mContext = context;
        this.mHwAPPQoEStateMachine = HwAPPQoEStateMachine.createHwAPPQoEStateMachine(context);
    }

    public static HwAPPQoEManager createHwAPPQoEManager(Context context) {
        if (mHwAPPQoEManager == null) {
            mHwAPPQoEManager = new HwAPPQoEManager(context);
        }
        return mHwAPPQoEManager;
    }

    public static HwAPPQoEManager getInstance() {
        if (mHwAPPQoEManager != null) {
            return mHwAPPQoEManager;
        }
        return null;
    }

    public void registerAppQoECallback(IHwAPPQoECallback callback, boolean isBrain) {
        if (isBrain) {
            this.mBrainCallback = callback;
        } else {
            this.mWaveMappingCallback = callback;
        }
    }

    public void queryNetworkQuality(int UID, int scence, int network, boolean needRtt) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("queryNetworkQuality UID = ");
        stringBuilder.append(UID);
        stringBuilder.append(" scence = ");
        stringBuilder.append(scence);
        stringBuilder.append(" network = ");
        stringBuilder.append(network);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        HwAPPQoEResourceManger manager = HwAPPQoEResourceManger.getInstance();
        if (manager != null) {
            int qci;
            HwAPPQoEAPKConfig conifg = manager.getAPKScenceConfig(scence);
            if (conifg != null) {
                qci = conifg.mQci;
            } else {
                qci = 0;
            }
            this.mHwChannelQoEManager = HwChannelQoEManager.createInstance(this.mContext);
            this.mHwChannelQoEManager.queryChannelQuality(UID, scence, network, qci, this);
        }
    }

    public IHwAPPQoECallback getAPPQoECallback(boolean isBrain) {
        if (isBrain) {
            return this.mBrainCallback;
        }
        return this.mWaveMappingCallback;
    }

    public void startWifiLinkMonitor(int UID, int scence) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("startWifiLinkMonitor UID = ");
        stringBuilder.append(UID);
        stringBuilder.append(" scence = ");
        stringBuilder.append(scence);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        HwAPPQoEResourceManger manager = HwAPPQoEResourceManger.getInstance();
        if (manager != null) {
            int qci;
            HwAPPQoEAPKConfig conifg = manager.getAPKScenceConfig(scence);
            if (conifg != null) {
                qci = conifg.mQci;
            } else {
                qci = 0;
            }
            this.mHwChannelQoEManager = HwChannelQoEManager.createInstance(this.mContext);
            this.mHwChannelQoEManager.startWifiLinkMonitor(UID, scence, qci, this);
        }
    }

    public void stopWifiLinkMonitor(int UID, boolean stopAll) {
        this.mHwChannelQoEManager = HwChannelQoEManager.createInstance(this.mContext);
        this.mHwChannelQoEManager.stopWifiLinkMonitor(UID, stopAll);
    }

    public void onMplinkStateChange(HwAPPStateInfo appInfo, int mplinkEvent, int failReason) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Enter onMplinkStateChange mplinkEvent = ");
        stringBuilder.append(mplinkEvent);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        if (mplinkEvent == 9 && appInfo != null) {
            HwAPPChrManager.getInstance().updateStatisInfo(appInfo, 8);
            HwAPPQoEUserAction mHwAPPQoEUserAction = HwAPPQoEUserAction.getInstance();
            if (mHwAPPQoEUserAction != null) {
                Context context = this.mContext;
                Context context2 = this.mContext;
                WifiInfo mWifiInfo = ((WifiManager) context.getSystemService("wifi")).getConnectionInfo();
                if (mWifiInfo != null && mWifiInfo.getSSID() != null) {
                    mHwAPPQoEUserAction.updateUserActionData(1, appInfo.mAppId, mWifiInfo.getSSID());
                }
            }
        }
    }

    public HwAPPStateInfo getCurAPPStateInfo() {
        HwAPPQoEUtils.logD(TAG, "Enter getCurAPPStateInfo.");
        return this.mHwAPPQoEStateMachine.getCurAPPStateInfo();
    }

    public void logE(String info) {
        Log.e(TAG, info);
    }

    public void onChannelQuality(int UID, int scence, int network, int label) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onChannelQuality UID = ");
        stringBuilder.append(UID);
        stringBuilder.append(" scence =  network = ");
        stringBuilder.append(network);
        stringBuilder.append(" label = ");
        stringBuilder.append(label);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        boolean result = false;
        if (this.mBrainCallback != null) {
            if (label == 0) {
                result = true;
            }
            this.mBrainCallback.onNetworkQualityCallBack(UID, scence, network, result);
        }
    }

    public void onWifiLinkQuality(int UID, int scence, int label) {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onWifiLinkQuality UID = ");
        stringBuilder.append(UID);
        stringBuilder.append(" scence = ");
        stringBuilder.append(scence);
        stringBuilder.append(" label = ");
        stringBuilder.append(label);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        boolean result = false;
        if (this.mBrainCallback != null) {
            if (label == 0) {
                result = true;
            }
            this.mBrainCallback.onWifiLinkQuality(UID, scence, result);
        }
    }

    public void onCellPSAvailable(boolean isOK, int reason) {
    }

    private static boolean getSettingsSystemBoolean(ContentResolver cr, String name, boolean def) {
        return System.getInt(cr, name, def) == 1;
    }

    public boolean getHidataState() {
        return getSettingsSystemBoolean(this.mContext.getContentResolver(), "smart_network_switching", false);
    }

    public void onCurrentRtt(int rtt) {
    }
}
