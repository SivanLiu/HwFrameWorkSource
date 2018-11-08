package com.android.server.wifi;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiScanner;
import android.net.wifi.WifiScanner.ChannelSpec;
import android.net.wifi.WifiScanner.ScanSettings;
import android.os.Looper;
import android.os.RemoteException;
import android.util.LocalLog;
import android.util.Log;
import com.android.server.wifi.hotspot2.PasspointNetworkEvaluator;
import com.android.server.wifi.wifipro.wifiscangenie.WifiScanGenieController;
import com.huawei.pgmng.plug.PGSdk;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class HwWifiConnectivityManager extends WifiConnectivityManager {
    private static final int DEFAULT_SCAN_PERIOD_SKIP_COUNTER = 4;
    private static final int HW_MAX_PERIODIC_SCAN_INTERVAL_MS = 60000;
    private static final int HW_MAX_STATIONARY_PERIODIC_SCAN_INTERVAL_MS = 300000;
    private static final int HW_MID_PERIODIC_SCAN_INTERVAL_MS = 30000;
    private static final int HW_MIX_PERIODIC_SCAN_INTERVAL_MS = 10000;
    private static final int HW_NAVIGATION_PERIODIC_SCAN_INTERVAL_MS = 120000;
    private static final String PG_AR_STATE_ACTION = "com.huawei.intent.action.PG_AR_STATE_ACTION";
    private static final String PG_RECEIVER_PERMISSION = "com.huawei.powergenie.receiverPermission";
    private static final int SCAN_COUNT_CHANGE_REASON_ADD = 0;
    private static final int SCAN_COUNT_CHANGE_REASON_MINUS = 1;
    private static final int SCAN_COUNT_CHANGE_REASON_RESET = 2;
    private static final int STATE_GPS = 3;
    private static final String TAG = "HwWifiConnectivityManager";
    private boolean bExtendWifiScanPeriodForP2p = false;
    private int iScanPeriodSkipTimes = 4;
    private Context mContext;
    private int mExponentialPeriodicSingleScanInterval;
    private int mHwSingleScanCounter;
    private boolean mIsStationary = false;
    private PGSdk mPGSdk = null;
    private int mPeriodicSingleScanInterval;
    private WifiScanGenieController mWifiScanGenieController;
    private int mWifiScanPeriodCounter = 0;

    public HwWifiConnectivityManager(Context context, WifiStateMachine stateMachine, WifiScanner scanner, WifiConfigManager configManager, WifiInfo wifiInfo, WifiNetworkSelector networkSelector, WifiConnectivityHelper connectivityHelper, WifiLastResortWatchdog wifiLastResortWatchdog, OpenNetworkNotifier openNetworkNotifier, WifiMetrics wifiMetrics, Looper looper, Clock clock, LocalLog localLog, boolean enable, FrameworkFacade frameworkFacade, SavedNetworkEvaluator savedNetworkEvaluator, ScoredNetworkEvaluator scoredNetworkEvaluator, PasspointNetworkEvaluator passpointNetworkEvaluator) {
        super(context, stateMachine, scanner, configManager, wifiInfo, networkSelector, connectivityHelper, wifiLastResortWatchdog, openNetworkNotifier, wifiMetrics, looper, clock, localLog, enable, frameworkFacade, savedNetworkEvaluator, scoredNetworkEvaluator, passpointNetworkEvaluator);
        this.mContext = context;
        this.mWifiScanGenieController = WifiScanGenieController.createWifiScanGenieControllerImpl(this.mContext);
        if (PreconfiguredNetworkManager.IS_R1) {
            this.mNetworkSelector.registerNetworkEvaluator(new PreconfiguredNetworkEvaluator(context, configManager), 4);
        }
        this.mContext.registerReceiver(new BroadcastReceiver() {
            public void onReceive(Context context, Intent intent) {
                if (intent != null) {
                    boolean stationary = intent.getBooleanExtra("stationary", false);
                    Log.d(HwWifiConnectivityManager.TAG, "Current stationary = " + HwWifiConnectivityManager.this.mIsStationary + ", new stationary = " + stationary);
                    if (stationary != HwWifiConnectivityManager.this.mIsStationary) {
                        HwWifiConnectivityManager.this.mIsStationary = stationary;
                        HwWifiConnectivityManager.this.startConnectivityScan(false, false);
                    }
                }
            }
        }, new IntentFilter(PG_AR_STATE_ACTION), PG_RECEIVER_PERMISSION, null);
        log("HwWifiConnectivityManager init!");
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public String unselectDhcpFailedBssid(String targetBssid, String scanResultBssid, WifiConfiguration candidate) {
        if (HwSelfCureEngine.getInstance() == null || candidate == null || scanResultBssid == null || !HwSelfCureEngine.getInstance().isDhcpFailedConfigKey(candidate.configKey())) {
            return targetBssid;
        }
        return scanResultBssid;
    }

    protected void extendWifiScanPeriodForP2p(boolean bExtend, int iTimes) {
        this.mWifiScanPeriodCounter = 0;
        this.bExtendWifiScanPeriodForP2p = bExtend;
        if (!bExtend || iTimes <= 0) {
            this.iScanPeriodSkipTimes = 4;
        } else {
            this.iScanPeriodSkipTimes = iTimes;
        }
        log("extendWifiScanPeriodForP2p: " + this.bExtendWifiScanPeriodForP2p + ", Times = " + this.iScanPeriodSkipTimes);
    }

    protected boolean isScanThisPeriod(boolean isP2pConn) {
        if (!isP2pConn) {
            if (this.mWifiScanPeriodCounter > 0) {
                this.bExtendWifiScanPeriodForP2p = false;
                this.mWifiScanPeriodCounter = 0;
                this.iScanPeriodSkipTimes = 4;
            }
            return true;
        } else if (!this.bExtendWifiScanPeriodForP2p) {
            return true;
        } else {
            this.mWifiScanPeriodCounter++;
            if (this.mWifiScanPeriodCounter % this.iScanPeriodSkipTimes == 0) {
                return true;
            }
            log("isScanThisPeriod: false");
            return false;
        }
    }

    private int getScanGeniePeriodicSingleScanInterval() {
        if (this.mHwSingleScanCounter < 4) {
            this.mPeriodicSingleScanInterval = 10000;
        } else if (this.mHwSingleScanCounter < 7) {
            this.mPeriodicSingleScanInterval = 30000;
        } else {
            if (checkNavigationMode()) {
                this.mPeriodicSingleScanInterval = 120000;
            } else {
                this.mPeriodicSingleScanInterval = 60000;
            }
            if (this.mIsStationary) {
                this.mPeriodicSingleScanInterval = 300000;
            }
        }
        log("HwSingleScanCounter: " + this.mHwSingleScanCounter + ", mPeriodicSingleScanInterval : " + this.mPeriodicSingleScanInterval + " ms");
        return this.mPeriodicSingleScanInterval;
    }

    private boolean checkNavigationMode() {
        ArrayList<String> navigationPackages = new ArrayList(Arrays.asList(new String[]{"com.autonavi.minimap", "com.baidu.BaiduMap", "com.autonavi.xmgd.navigator", "com.tencent.map", "com.ovital.ovitalMap", "com.google.android.apps.maps", "com.baidu.navi", "cld.navi.mainframe", "com.sogou.map.android.maps", "com.uu.uunavi", "com.sunboxsoft.oilforgdandroid", "com.pdager", "com.itotem.traffic.broadcasts", "com.mapbar.android.mapbarmap", "com.autonavi.xmgd.navigator.toc", "cn.com.tiros.android.navidog", "com.autonavi.minimap.custom", "com.autonavi.cmccmap", "com.baidu.BaiduMap.pad", "com.baidu.carlife", "com.tigerknows", "com.erlinyou.worldlist", "com.uu.uueeye", "com.mapbar.android.trybuynavi", "com.zhituo.gpslocation", "com.waze", "ru.yandex.yandexnavi"}));
        boolean state = false;
        this.mPGSdk = PGSdk.getInstance();
        if (this.mPGSdk == null) {
            return false;
        }
        for (int k = 0; k < navigationPackages.size(); k++) {
            try {
                state = this.mPGSdk.checkStateByPkg(this.mContext, (String) navigationPackages.get(k), 3);
            } catch (RemoteException e) {
                log("checkStateByPkg occur exception.");
            }
            if (state) {
                return true;
            }
        }
        return false;
    }

    protected int getPeriodicSingleScanInterval() {
        if (!isSupportWifiScanGenie() || this.mWifiState == 1) {
            log("****isSupportWifiScanGenie :  fasle: ");
            this.mExponentialPeriodicSingleScanInterval *= 2;
            int maxInterval = 160000;
            if (this.mIsStationary) {
                maxInterval = 300000;
            }
            if (this.mExponentialPeriodicSingleScanInterval > maxInterval) {
                this.mExponentialPeriodicSingleScanInterval = maxInterval;
            }
            return this.mExponentialPeriodicSingleScanInterval;
        }
        log("****isSupportWifiScanGenie :  true: ");
        return getScanGeniePeriodicSingleScanInterval();
    }

    protected void resetPeriodicSingleScanInterval() {
        this.mExponentialPeriodicSingleScanInterval = 20000;
    }

    protected void handleSingleScanFailure(int reason) {
        log("handleSingleScanFailure reason " + reason);
        handleScanCountChanged(1);
    }

    protected void handleSingleScanSuccess() {
    }

    protected void handleScanCountChanged(int reason) {
        if (reason == 0) {
            this.mHwSingleScanCounter++;
        } else if (1 == reason) {
            if (this.mHwSingleScanCounter > 0) {
                this.mHwSingleScanCounter--;
            }
        } else if (2 == reason) {
            this.mHwSingleScanCounter = 0;
        }
        log("handleScanCounterChanged,reason: " + reason + ", mHwSingleScanCounter: " + this.mHwSingleScanCounter);
    }

    protected boolean isSupportWifiScanGenie() {
        return true;
    }

    protected boolean isWifiScanSpecialChannels() {
        return isSupportWifiScanGenie() && this.mHwSingleScanCounter <= 1;
    }

    protected ScanSettings getScanGenieSettings() {
        return getHwScanSettings();
    }

    protected boolean handleForceScan() {
        return false;
    }

    private ScanSettings getHwScanSettings() {
        List<Integer> fusefrequencyList = this.mWifiScanGenieController.getScanfrequencys();
        if (fusefrequencyList == null || fusefrequencyList.size() == 0) {
            log("getHwScanSettings,fusefrequencyList is null:");
            return null;
        }
        ScanSettings settings = new ScanSettings();
        settings.band = 0;
        settings.reportEvents = 3;
        settings.numBssidsPerScan = 0;
        ChannelSpec[] channels = new ChannelSpec[fusefrequencyList.size()];
        for (int i = 0; i < fusefrequencyList.size(); i++) {
            channels[i] = new ChannelSpec(((Integer) fusefrequencyList.get(i)).intValue());
        }
        settings.channels = channels;
        return settings;
    }

    private void log(String msg) {
        Log.d(TAG, msg);
    }
}
