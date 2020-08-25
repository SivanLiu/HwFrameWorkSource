package com.android.server.hidata.appqoe;

import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.SystemProperties;
import android.provider.Settings;
import android.util.Log;
import com.android.server.hidata.HwHidataAppStateInfo;
import com.android.server.hidata.IHwHidataCallback;
import com.android.server.hidata.arbitration.HwArbitrationCommonUtils;
import com.android.server.hidata.arbitration.HwArbitrationFunction;
import com.android.server.hidata.channelqoe.HwChannelQoEManager;
import com.android.server.hidata.channelqoe.IChannelQoECallback;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import com.android.server.wifipro.WifiProCommonUtils;
import huawei.android.net.hwmplink.MpLinkCommonUtils;
import java.util.ArrayList;
import java.util.Iterator;

public class HwAPPQoEManager implements IChannelQoECallback {
    private static final int GAMEQOE_CALLBACKLIST_MAX_SIZE = 10;
    private static final String TAG = "HiData_HwAPPQoEManager";
    private static final int USER_HANDOVER_TO_WIFI = 9;
    private static HwAPPQoEManager mHwAPPQoEManager = null;
    IHwAPPQoECallback mBrainCallback = null;
    private Context mContext;
    private ArrayList<IHwHidataCallback> mGameQoeCallbackList = new ArrayList<>();
    private HwAPPQoEStateMachine mHwAPPQoEStateMachine = null;
    private HwChannelQoEManager mHwChannelQoEManager = null;
    private final Object mLock = new Object();
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
        HwAPPQoEManager hwAPPQoEManager = mHwAPPQoEManager;
        if (hwAPPQoEManager != null) {
            return hwAPPQoEManager;
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

    public void queryNetworkQuality(int uid, int scence, int network, boolean needTputTest) {
        int qci;
        HwArbitrationCommonUtils.logD(TAG, false, "queryNetworkQuality uid = %{public}d scence = %{public}d network = %{public}d needTputTest = %{public}s", Integer.valueOf(uid), Integer.valueOf(scence), Integer.valueOf(network), String.valueOf(needTputTest));
        HwAPPQoEResourceManger manager = HwAPPQoEResourceManger.getInstance();
        if (manager != null) {
            HwAPPQoEAPKConfig conifg = manager.getAPKScenceConfig(scence);
            if (conifg != null) {
                qci = conifg.mQci;
            } else {
                qci = 0;
            }
            this.mHwChannelQoEManager = HwChannelQoEManager.createInstance(this.mContext);
            this.mHwChannelQoEManager.queryChannelQuality(uid, network, qci, needTputTest, this);
        }
    }

    public IHwAPPQoECallback getAPPQoECallback(boolean isBrain) {
        if (isBrain) {
            return this.mBrainCallback;
        }
        return this.mWaveMappingCallback;
    }

    public void startWifiLinkMonitor(int UID, int scence, boolean needTputTest) {
        int qci;
        HwArbitrationCommonUtils.logD(TAG, false, "startWifiLinkMonitor UID = %{public}d scence = %{public}d needTputTest = %{public}s", Integer.valueOf(UID), Integer.valueOf(scence), String.valueOf(needTputTest));
        HwAPPQoEResourceManger manager = HwAPPQoEResourceManger.getInstance();
        if (manager != null) {
            HwAPPQoEAPKConfig conifg = manager.getAPKScenceConfig(scence);
            if (conifg != null) {
                qci = conifg.mQci;
            } else {
                qci = 0;
            }
            this.mHwChannelQoEManager = HwChannelQoEManager.createInstance(this.mContext);
            this.mHwChannelQoEManager.startWifiLinkMonitor(UID, scence, qci, needTputTest, this);
        }
    }

    public void stopWifiLinkMonitor(int UID, boolean stopAll) {
        this.mHwChannelQoEManager = HwChannelQoEManager.createInstance(this.mContext);
        this.mHwChannelQoEManager.stopWifiLinkMonitor(UID, stopAll);
    }

    public void onMplinkStateChange(HwAPPStateInfo appInfo, int mplinkEvent, int failReason) {
        WifiInfo mWifiInfo;
        HwAPPQoEUtils.logD(TAG, false, "Enter onMplinkStateChange mplinkEvent = %{public}d", Integer.valueOf(mplinkEvent));
        if (mplinkEvent == 9 && appInfo != null) {
            HwAPPChrManager.getInstance().updateStatisInfo(appInfo, 8);
            HwAPPQoEUserAction mHwAPPQoEUserAction = HwAPPQoEUserAction.getInstance();
            if (mHwAPPQoEUserAction != null && (mWifiInfo = ((WifiManager) this.mContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE)).getConnectionInfo()) != null && mWifiInfo.getSSID() != null) {
                mHwAPPQoEUserAction.updateUserActionData(1, appInfo.mAppId, mWifiInfo.getSSID());
            }
        }
    }

    public HwAPPStateInfo getCurAPPStateInfo() {
        HwAPPQoEUtils.logD(TAG, false, "Enter getCurAPPStateInfo.", new Object[0]);
        return this.mHwAPPQoEStateMachine.getCurAPPStateInfo();
    }

    public void logE(String info) {
        Log.e(TAG, info);
    }

    @Override // com.android.server.hidata.channelqoe.IChannelQoECallback
    public void onChannelQuality(int UID, int scence, int network, int label) {
        HwAPPQoEUtils.logD(TAG, false, "onChannelQuality UID = %{public}d scence = %{public}d network = %{public}d label = %{public}d", Integer.valueOf(UID), Integer.valueOf(scence), Integer.valueOf(network), Integer.valueOf(label));
        boolean result = false;
        if (this.mBrainCallback != null) {
            if (label == 0) {
                result = true;
            }
            this.mBrainCallback.onNetworkQualityCallBack(UID, scence, network, result);
        }
    }

    @Override // com.android.server.hidata.channelqoe.IChannelQoECallback
    public void onWifiLinkQuality(int UID, int scence, int label) {
        HwAPPQoEUtils.logD(TAG, false, "onWifiLinkQuality UID = %{public}d scence = %{public}d label = %{public}d", Integer.valueOf(UID), Integer.valueOf(scence), Integer.valueOf(label));
        boolean result = false;
        if (this.mBrainCallback != null) {
            if (label == 0) {
                result = true;
            }
            this.mBrainCallback.onWifiLinkQuality(UID, scence, result);
        }
    }

    @Override // com.android.server.hidata.channelqoe.IChannelQoECallback
    public void onCellPSAvailable(boolean isOK, int reason) {
    }

    private static boolean getSettingsSystemBoolean(ContentResolver cr, String name, boolean def) {
        return Settings.System.getInt(cr, name, def ? 1 : 0) == 1;
    }

    public boolean getHidataState() {
        return getSettingsSystemBoolean(this.mContext.getContentResolver(), "smart_network_switching", false);
    }

    @Override // com.android.server.hidata.channelqoe.IChannelQoECallback
    public void onCurrentRtt(int rtt) {
    }

    public boolean registerHidataMonitor(IHwHidataCallback callback) {
        synchronized (this.mLock) {
            if (callback == null) {
                HwAPPQoEUtils.logE(TAG, false, "Callback null", new Object[0]);
                return false;
            } else if (this.mGameQoeCallbackList.size() >= 10) {
                HwAPPQoEUtils.logE(TAG, false, "mGameQoeCallbackList size full", new Object[0]);
                return false;
            } else if (this.mGameQoeCallbackList.contains(callback)) {
                HwAPPQoEUtils.logE(TAG, false, "Callback has in list, do not register again", new Object[0]);
                return false;
            } else {
                this.mGameQoeCallbackList.add(callback);
                return true;
            }
        }
    }

    public void notifyGameQoeCallback(HwAPPStateInfo appStateInfo, int state) {
        synchronized (this.mLock) {
            if (this.mGameQoeCallbackList.size() == 0) {
                HwAPPQoEUtils.logE(TAG, false, "no notifyGameQoeCallback", new Object[0]);
                return;
            }
            HwAPPQoEUtils.logD(TAG, false, "notifyGameQoeCallback: %{public}d", Integer.valueOf(state));
            HwHidataAppStateInfo gameStateInfo = new HwHidataAppStateInfo();
            gameStateInfo.setAppId(appStateInfo.mAppId);
            gameStateInfo.setCurUid(appStateInfo.mAppUID);
            gameStateInfo.setCurScence(appStateInfo.mScenceId);
            gameStateInfo.setCurRtt(appStateInfo.mAppRTT);
            gameStateInfo.setCurState(state);
            gameStateInfo.setAction(appStateInfo.mAction);
            Iterator<IHwHidataCallback> it = this.mGameQoeCallbackList.iterator();
            while (it.hasNext()) {
                it.next().onAppStateChangeCallBack(gameStateInfo);
            }
        }
    }

    public boolean isNotifyAppQoeMpLinkStateSuccessful(String pkgName, boolean enable) {
        HwAPPStateInfo appStateInfo = getCurAPPStateInfo();
        if (appStateInfo == null) {
            HwAPPQoEUtils.logE(TAG, false, "appStateInfo error", new Object[0]);
            return false;
        }
        int uid = MpLinkCommonUtils.getAppUid(this.mContext, pkgName);
        if (appStateInfo.mAppUID != uid) {
            HwAPPQoEUtils.logE(TAG, false, "not curApp uid : %{public}d", Integer.valueOf(uid));
            return false;
        }
        if (enable) {
            this.mHwAPPQoEStateMachine.sendMessage(112, appStateInfo);
        } else {
            this.mHwAPPQoEStateMachine.sendMessage(111, appStateInfo);
        }
        return true;
    }

    public static boolean isAppStartMonitor(HwAPPStateInfo appStateInfo, Context context) {
        if (appStateInfo == null || context == null) {
            HwAPPQoEUtils.logE(TAG, false, "appStateInfo or context error", new Object[0]);
            return false;
        } else if (WifiProCommonUtils.isWifiProLitePropertyEnabled(context) || !WifiProCommonUtils.isWifiProSwitchOn(context)) {
            HwAPPQoEUtils.logD(TAG, false, "lite version or wifi pro switch off", new Object[0]);
            return false;
        } else if (HwArbitrationCommonUtils.MAINLAND_REGION && appStateInfo.getAppRegion() != 1) {
            return true;
        } else {
            if (appStateInfo.getAppRegion() == 1 && !HwArbitrationFunction.isChina() && HwArbitrationCommonUtils.IS_HIDATA2_ENABLED) {
                return isOverSeaAppInAllowMpLinkArea(appStateInfo);
            }
            HwAPPQoEUtils.logD(TAG, false, "the app and phone region is not situation", new Object[0]);
            return false;
        }
    }

    private static boolean isOverSeaAppInAllowMpLinkArea(HwAPPStateInfo appStateInfo) {
        if (appStateInfo.mScenceId == 100901) {
            return !isNotAllowMplinkRegion();
        }
        return true;
    }

    private static boolean isNotAllowMplinkRegion() {
        String country = SystemProperties.get("ro.hw.country", "");
        return country.contains("meaf") || "jp".equalsIgnoreCase(country) || "la".equalsIgnoreCase(country) || "nla".equalsIgnoreCase(country);
    }
}
