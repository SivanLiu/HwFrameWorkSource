package com.android.server.wifi.ABS;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.NetworkInfo;
import android.net.NetworkInfo.State;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import com.android.server.wifi.HwWifiCHRService;
import com.android.server.wifi.HwWifiCHRServiceImpl;
import java.util.List;

public class HwABSCHRManager {
    private static final int UPLOAD_TIME_INTERVAL = 86400000;
    private static final int WIFI_CONNECTED_MESSAGE = 1;
    public static final int WIFI_HANDOVER_TYPE_ANTENER_SCREEN_OFF_STATE = 5;
    public static final int WIFI_HANDOVER_TYPE_ANTENER_SCREEN_ON_STATE = 4;
    public static final int WIFI_HANDOVER_TYPE_IN_CALL = 6;
    public static final int WIFI_HANDOVER_TYPE_LONG_CONNECT_STATE = 1;
    public static final int WIFI_HANDOVER_TYPE_NONE = 0;
    public static final int WIFI_HANDOVER_TYPE_OUT_CALL = 7;
    public static final int WIFI_HANDOVER_TYPE_SEARCH_STATE = 3;
    public static final int WIFI_HANDOVER_TYPE_SHORT_CONNECT_STATE = 2;
    public static final int WIFI_HANDOVER_TYPE_SISO_TO_MIMO = 8;
    private static HwABSCHRManager mHwABSCHRManager = null;
    private IntentFilter intentFilter = new IntentFilter();
    private boolean isBootComplete = false;
    private HwABSCHRBlackListEvent mBlackListEvent = null;
    private BroadcastReceiver mBroadcastReceiver = new WifiBroadcastReceiver(this, null);
    private Context mContext;
    private HwABSCHRExEvent mCurrentApInfo;
    private Handler mHandler = new Handler() {
        public void handleMessage(Message msg) {
            if (msg.what == 1) {
                HwABSCHRManager.this.uploadStatistis();
            }
        }
    };
    private HwABSDataBaseManager mHwABSDataBaseManager;
    private boolean mIsNeedReset = false;
    private WifiManager mWifiManager;

    private class WifiBroadcastReceiver extends BroadcastReceiver {
        private WifiBroadcastReceiver() {
        }

        /* synthetic */ WifiBroadcastReceiver(HwABSCHRManager x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if ("android.net.wifi.STATE_CHANGE".equals(action)) {
                NetworkInfo netInfo = (NetworkInfo) intent.getParcelableExtra("networkInfo");
                if (netInfo != null && netInfo.getState() == State.CONNECTED) {
                    HwABSCHRManager.this.mHandler.sendEmptyMessage(1);
                }
            } else if ("android.intent.action.BOOT_COMPLETED".equals(action)) {
                HwABSCHRManager.this.isBootComplete = true;
            }
        }
    }

    private HwABSCHRManager(Context context) {
        this.mContext = context;
        this.mHwABSDataBaseManager = HwABSDataBaseManager.getInstance(context);
        this.mWifiManager = (WifiManager) this.mContext.getSystemService("wifi");
        registerBroadcastReceiver();
        if (this.mHwABSDataBaseManager.getCHRStatistics() == null) {
            this.mHwABSDataBaseManager.inlineAddCHRInfo(new HwABSCHRStatistics());
        }
    }

    public static HwABSCHRManager getInstance(Context context) {
        if (mHwABSCHRManager == null) {
            mHwABSCHRManager = new HwABSCHRManager(context);
        }
        return mHwABSCHRManager;
    }

    private void registerBroadcastReceiver() {
        this.intentFilter.addAction("android.net.wifi.STATE_CHANGE");
        this.intentFilter.addAction("android.intent.action.BOOT_COMPLETED");
        this.mContext.registerReceiver(this.mBroadcastReceiver, this.intentFilter);
    }

    private void uploadStatistis() {
        if (isNeedToUpload(this.mHwABSDataBaseManager.getCHRStatistics())) {
            HwWifiCHRServiceImpl.getInstance().updateWifiException(216, "ABS_STATISICS");
            this.mIsNeedReset = true;
        }
    }

    private boolean isNeedToUpload(HwABSCHRStatistics record) {
        if (record == null || !this.isBootComplete) {
            HwABSUtils.logD("isNeedToUpload record == null");
            return false;
        }
        long currTime = System.currentTimeMillis();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isNeedToUpload record.last_upload_time = ");
        stringBuilder.append(record.last_upload_time);
        stringBuilder.append("   System.currentTimeMillis() = ");
        stringBuilder.append(System.currentTimeMillis());
        HwABSUtils.logD(stringBuilder.toString());
        if (currTime - record.last_upload_time > 86400000) {
            HwABSUtils.logD("isNeedToUpload return true");
            return true;
        }
        HwABSUtils.logD("isNeedToUpload return false");
        return false;
    }

    private WifiConfiguration getCurrentConfig(int networkId) {
        WifiConfiguration result = null;
        List<WifiConfiguration> configNetworks = this.mWifiManager.getConfiguredNetworks();
        if (configNetworks == null || configNetworks.size() == 0) {
            return null;
        }
        for (WifiConfiguration nextConfig : configNetworks) {
            if (networkId == nextConfig.networkId) {
                result = nextConfig;
                break;
            }
        }
        return result;
    }

    private boolean isValid(WifiConfiguration config) {
        boolean z = false;
        if (config == null) {
            return false;
        }
        if (config.allowedKeyManagement.cardinality() <= 1) {
            z = true;
        }
        return z;
    }

    public synchronized void increaseEventStatistics(int type) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("increaseEventStatistics type = ");
        stringBuilder.append(type);
        HwABSUtils.logD(stringBuilder.toString());
        HwABSCHRStatistics statistics = this.mHwABSDataBaseManager.getCHRStatistics();
        if (statistics != null) {
            switch (type) {
                case 1:
                    statistics.long_connect_event++;
                    break;
                case 2:
                    statistics.short_connect_event++;
                    break;
                case 3:
                    statistics.search_event++;
                    break;
                case 4:
                    statistics.antenna_preempted_screen_on_event++;
                    break;
                case 5:
                    statistics.antenna_preempted_screen_off_event++;
                    break;
                case 6:
                    statistics.mo_mt_call_event++;
                    break;
                case 7:
                    statistics.siso_to_mimo_event++;
                    break;
                case 8:
                    statistics.ping_pong_times++;
                    break;
            }
            updateRssiLevel(statistics);
            this.mHwABSDataBaseManager.inlineUpdateCHRInfo(statistics);
        }
    }

    private void updateRssiLevel(HwABSCHRStatistics statistics) {
        WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
        switch (HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(mWifiInfo.getFrequency(), mWifiInfo.getRssi())) {
            case 0:
                statistics.mRssiL0++;
                return;
            case 1:
                statistics.mRssiL1++;
                return;
            case 2:
                statistics.mRssiL2++;
                return;
            case 3:
                statistics.mRssiL3++;
                return;
            case 4:
                statistics.mRssiL4++;
                return;
            default:
                return;
        }
    }

    public static String getAPSSID(WifiInfo mWifiInfo) {
        String ssid = "";
        String strAp_Ssid = "";
        if (mWifiInfo.getSSID() == null) {
            return strAp_Ssid;
        }
        String oriSsid = mWifiInfo.getSSID();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("getAPSSID oriSsid = ");
        stringBuilder.append(oriSsid);
        HwABSUtils.logD(stringBuilder.toString());
        if (!oriSsid.equals("") && oriSsid.length() >= 4) {
            ssid = oriSsid.substring(1, oriSsid.length() - 1);
        }
        if (ssid.length() >= 32) {
            return ssid.substring(0, 31);
        }
        return ssid;
    }

    public synchronized void initABSHandoverException(int type) {
        WifiInfo mWifiInfo = this.mWifiManager.getConnectionInfo();
        if (mWifiInfo != null) {
            WifiConfiguration config = getCurrentConfig(mWifiInfo.getNetworkId());
            if (isValid(config)) {
                this.mCurrentApInfo = new HwABSCHRExEvent();
                this.mCurrentApInfo.mABSApSsid = getAPSSID(mWifiInfo);
                this.mCurrentApInfo.mABSApBssid = mWifiInfo.getBSSID();
                this.mCurrentApInfo.mABSApChannel = mWifiInfo.getFrequency();
                this.mCurrentApInfo.mABSApRSSI = mWifiInfo.getRssi();
                this.mCurrentApInfo.mABSApAuthType = config.getAuthType();
                this.mCurrentApInfo.mSwitchType = type;
            }
        }
    }

    public synchronized HwABSCHRExEvent getExceptionInfo() {
        return this.mCurrentApInfo;
    }

    public HwABSCHRStatistics getStatisticsInfo() {
        HwABSCHRStatistics statistics = this.mHwABSDataBaseManager.getCHRStatistics();
        if (this.mIsNeedReset) {
            HwABSCHRStatistics resetStatistics = new HwABSCHRStatistics();
            resetStatistics.last_upload_time = System.currentTimeMillis();
            this.mHwABSDataBaseManager.inlineUpdateCHRInfo(resetStatistics);
            this.mIsNeedReset = false;
        }
        return statistics;
    }

    public void updateCHRInfo(HwABSCHRStatistics statistics) {
        this.mHwABSDataBaseManager.inlineUpdateCHRInfo(statistics);
    }

    public synchronized void uploadABSReassociateExeption() {
        HwABSUtils.logD("uploadABSReassociateExeption");
        if (this.mCurrentApInfo != null) {
            HwWifiCHRServiceImpl.getInstance().updateWifiException(215, "ABS_Reassociate_Exeption");
        }
    }

    public void updateABSTime(String ssid, long mimoTime, long sisoTime, long mimoScreenOnTime, long sisoScreenOnTime) {
        String str = ssid;
        long j = mimoTime;
        long j2 = sisoTime;
        long j3 = mimoScreenOnTime;
        long j4 = sisoScreenOnTime;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateScreenOffTime mimoTime = ");
        stringBuilder.append(j);
        stringBuilder.append(" sisoTime = ");
        stringBuilder.append(j2);
        stringBuilder.append(" mimoScreenOnTime = ");
        stringBuilder.append(j3);
        stringBuilder.append(" sisoScreenOnTime = ");
        stringBuilder.append(j4);
        HwABSUtils.logE(stringBuilder.toString());
        HwABSCHRStatistics statistics = this.mHwABSDataBaseManager.getCHRStatistics();
        if (str == null) {
        } else if (statistics == null) {
            HwABSCHRStatistics hwABSCHRStatistics = statistics;
        } else {
            statistics.mimo_time += j;
            statistics.siso_time += j2;
            statistics.mimo_screen_on_time += j3;
            statistics.siso_screen_on_time += j4;
            this.mHwABSDataBaseManager.inlineUpdateCHRInfo(statistics);
            HwWifiCHRService mHwWifiCHRService = HwWifiCHRServiceImpl.getInstance();
            stringBuilder = new StringBuilder();
            stringBuilder.append("updateABSTime ssid == ");
            stringBuilder.append(str);
            HwABSUtils.logE(stringBuilder.toString());
            mHwWifiCHRService.updateABSTime(str, 0, 0, j, j2, mimoScreenOnTime, sisoScreenOnTime);
            return;
        }
        HwABSUtils.logE("updateABSTime ssid == null");
    }

    public void updateCHRAssociateTimes(String ssid, int associateTimes, int associateFailedTimes) {
        String str = ssid;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("updateCHRAssociateTimes  associateTimes = ");
        int i = associateTimes;
        stringBuilder.append(i);
        stringBuilder.append(" associateFailedTimes = ");
        int i2 = associateFailedTimes;
        stringBuilder.append(i2);
        HwABSUtils.logE(stringBuilder.toString());
        if (str == null) {
            HwABSUtils.logE("updateCHRAssociateTimes ssid == null");
            return;
        }
        HwWifiCHRService mHwWifiCHRService = HwWifiCHRServiceImpl.getInstance();
        stringBuilder = new StringBuilder();
        stringBuilder.append("updateCHRAssociateTimes ssid == ");
        stringBuilder.append(str);
        HwABSUtils.logE(stringBuilder.toString());
        mHwWifiCHRService.updateABSTime(str, i, i2, 0, 0, 0, 0);
    }

    public HwABSCHRBlackListEvent getABSBlackListException() {
        return this.mBlackListEvent;
    }

    public void uploadBlackListException(HwABSCHRBlackListEvent event) {
        HwABSUtils.logD("uploadABSBlackListExeption");
        if (event == null) {
            HwABSUtils.logD("uploadABSBlackListExeption erro null");
            return;
        }
        this.mBlackListEvent = event;
        HwWifiCHRServiceImpl.getInstance().updateWifiException(217, "ABS_BlackList_Exeption");
    }
}
