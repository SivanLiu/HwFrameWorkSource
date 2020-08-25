package com.huawei.hwwifiproservice;

import android.common.HwFrameworkFactory;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.TrafficStats;
import android.net.Uri;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import android.text.TextUtils;
import android.util.Log;
import android.util.wifi.HwHiLog;
import com.android.server.hidata.appqoe.HwAPPQoEAPKConfig;
import com.android.server.hidata.appqoe.HwAPPQoEResourceManger;
import com.android.server.hidata.appqoe.HwAPPQoEUtils;
import com.android.server.hidata.appqoe.ai.HwApkQoeAiCalc;
import com.android.server.wifi.hwcoex.HiCoexUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

public class HwQoeQualityMonitor {
    private static final int AFTER_GOOD_THRESHOLD = 525;
    private static final int APP_CACHE_MAX_COUNT = 500;
    private static final int APP_NOT_FOUND_IN_CACHE = -1;
    private static final int BYTE_TO_BIT = 8;
    private static final int CHLOAD_TH_2G = 600;
    private static final int CHLOAD_TH_5G = 600;
    private static final int CMD_QUERY_HIDATA_INFO = 23;
    private static final int DEFAULT_BAD_QUALITY_NUM_THRESHOLD = 5;
    private static final int DEFAULT_CONTUNUES_BAD_QUALITY_THRESHOLD = 4;
    private static final int DEFAULT_DETECTION_PERIOD = 8;
    private static final int DEFAULT_GOOD_QUALITY_NUM_THRESHOLD = 2;
    private static final String DNS_SEPORATOR = "/";
    private static final int EMPTY_PACKET = 0;
    private static final int HIDATA_TCP_INFO_MIN_LENGTH = 10;
    private static final int HIDATA_WHITELIST = 0;
    private static final String IFACE = "wlan0";
    private static final int INIT_THRESHOLD = 510;
    private static final int INVALID_UID = -1;
    private static final String KEY_QOE_ARG = "arg";
    private static final String KEY_QOE_CMD = "cmd";
    private static final String KEY_QOE_RESULT_CMD = "resultCmd";
    private static final int LINKSPEED_RX_HIGH_TH_2G = 54;
    private static final int LINKSPEED_RX_HIGH_TH_5G = 54;
    private static final int LINKSPEED_RX_LOW_TH_2G = 27;
    private static final int LINKSPEED_RX_LOW_TH_5G = 40;
    private static final int LINKSPEED_TH_2G = 27;
    private static final int LINKSPEED_TH_5G = 40;
    private static final int LINKSPEED_TX_HIGH_TH_2G = 13;
    private static final int LINKSPEED_TX_HIGH_TH_5G = 27;
    private static final int LINKSPEED_TX_LOW_TH_2G = 6;
    private static final int LINKSPEED_TX_LOW_TH_5G = 13;
    private static final int MB_IN_BITS = 1048576;
    private static final int MIN_PERIOD_TIME = 1000;
    private static final float MIN_RX_PKTS = 1.0f;
    private static final float MIN_TX_PKTS = 2.0f;
    private static final int MONITOR_INTERVAL_THRESHOLD = 8;
    private static final float MORE_PKTS_HIGH_BAD_RATE = 0.5f;
    private static final float MORE_PKTS_LOW_BAD_RATE = 0.2f;
    private static final float MORE_PKTS_MIDDLE_BAD_RATE = 0.3f;
    private static final float MORE_RX_PKTS = 10.0f;
    private static final float MORE_TX_PKTS = 20.0f;
    public static final int MSG_APP_STATE_BAD = 1;
    public static final int MSG_APP_STATE_GOOD = 2;
    public static final int MSG_APP_STATE_UNKNOW = 0;
    private static final int NOISE_TH_2G = -70;
    private static final int NOISE_TH_5G = -80;
    private static final float OTA_RATE_BAD_TH = 0.01f;
    private static final int QUALIT_AI_INFO_DNS_FAIL = 2;
    private static final int QUALIT_AI_INFO_FREQUENCY = 0;
    private static final int QUALIT_AI_INFO_MAX_NUMBER = 14;
    private static final int QUALIT_AI_INFO_OTA_RATE = 12;
    private static final int QUALIT_AI_INFO_RESEND_RATE = 13;
    private static final int QUALIT_AI_INFO_RSSI = 1;
    private static final int QUALIT_AI_INFO_RS_PACKET = 7;
    private static final int QUALIT_AI_INFO_RTT = 10;
    private static final int QUALIT_AI_INFO_RTT_PACKET = 11;
    private static final int QUALIT_AI_INFO_RX_BYTE = 9;
    private static final int QUALIT_AI_INFO_RX_PACKET = 6;
    private static final int QUALIT_AI_INFO_TX_BAD = 4;
    private static final int QUALIT_AI_INFO_TX_BYTE = 8;
    private static final int QUALIT_AI_INFO_TX_GOOD = 3;
    private static final int QUALIT_AI_INFO_TX_PACKET = 5;
    private static final int QUALIT_AI_MIN_BAD_RTT = 500;
    private static final int QUALIT_AI_RSSI_FOUR_LEVEL = 4;
    private static final int QUALIT_AI_RSSI_TWO_LEVEL = 2;
    private static final String RSSI_PACKET_COUNT_INFO = "rssiPacketCountInfo";
    private static final int STRATEGY_MPLINK = 2;
    private static final int STRATEGY_NO_SWITCH = 0;
    private static final int STRATEGY_SWITCH_TO_CELL = 1;
    private static final int SYSTEM_UID = 1000;
    private static final String TAG = "HwQoeQualityMonitor";
    private static final float TCP_DEFAULT = 0.0f;
    private static final int TCP_INFO_INDEX_RESEND_PACKET = 8;
    private static final int TCP_INFO_INDEX_RTT_PACKET_NUMBER = 1;
    private static final int TCP_INFO_INDEX_RTT_TIME = 0;
    private static final int TCP_INFO_INDEX_RX_PACKET = 7;
    private static final int TCP_INFO_INDEX_TX_PACKET = 6;
    private static final float TCP_RTT_MAYBE_BAD = 1000.0f;
    private static final float TX_BAD_MIN_TH = 1.0f;
    private static final float TX_GOOD_MIN_TH = 20.0f;
    private static final int TX_QUEUE_BUFFER_TIME_MAX = 60000;
    private static final int TX_QUEUE_BUFFER_TIME_MIN = 800;
    private static final String UID_COUNT_SEPORATOR = "-";
    private static final int WLAN_PLUS_WHITELIST = 1;
    private static HwQoeQualityMonitor sHwQoeQualityMonitor = null;
    private HwAPPQoEAPKConfig mApkConfig;
    private List<HwAPPQoEAPKConfig> mApkConfigList;
    private List<String> mCachedInternetApps = new ArrayList();
    /* access modifiers changed from: private */
    public Context mContext = null;
    /* access modifiers changed from: private */
    public HwApkTcpInfo mCurrentHwApkTcpInfo;
    /* access modifiers changed from: private */
    public int mCurrentUid = -1;
    private final HandlerThread mHandlerThread;
    private HwApkQoeAiCalc mHwApkQoeAiCalc;
    private HwAPPQoEResourceManger mHwAppQoeResourceManager;
    private HwAutoConnectManager mHwAutoConnectManager;
    private IntentFilter mIntentFilter = new IntentFilter();
    /* access modifiers changed from: private */
    public boolean mIsCurrentAppMonitoring = false;
    /* access modifiers changed from: private */
    public AtomicBoolean mIsMonitorStarted = new AtomicBoolean(false);
    /* access modifiers changed from: private */
    public boolean mIsMpLinkState = false;
    private boolean mIsRegister = false;
    /* access modifiers changed from: private */
    public HwApkTcpInfo mLastHwApkTcpInfo;
    private int mLastTopUid = 0;
    private int mLastUidDnsFailedCnt = -1;
    private final Object mLock = new Object();
    private IntentFilter mPackageRemovedFilter = new IntentFilter();
    /* access modifiers changed from: private */
    public int mPeriodTime = 8000;
    /* access modifiers changed from: private */
    public QoeMonitorHandler mQoeMonitorHandler;
    /* access modifiers changed from: private */
    public TcpQualitySecondRecords mTcpQualitySecondRecords = null;
    private WifiManager mWifiManager;
    private BroadcastReceiver mWifiQoeBroadcastReceiver = new WifiQoeBroadcastReceiver();

    private HwQoeQualityMonitor(Context context) {
        this.mContext = context;
        this.mTcpQualitySecondRecords = new TcpQualitySecondRecords();
        this.mHandlerThread = new HandlerThread(TAG);
        this.mHandlerThread.start();
        this.mQoeMonitorHandler = new QoeMonitorHandler(this.mHandlerThread.getLooper());
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mHwApkQoeAiCalc = new HwApkQoeAiCalc();
        this.mHwAppQoeResourceManager = HwAPPQoEResourceManger.getInstance();
    }

    public static HwQoeQualityMonitor createInstance(Context context) {
        if (sHwQoeQualityMonitor == null) {
            sHwQoeQualityMonitor = new HwQoeQualityMonitor(context);
        }
        return sHwQoeQualityMonitor;
    }

    public static HwQoeQualityMonitor getInstance() {
        return sHwQoeQualityMonitor;
    }

    private void setDefaultApkConfigInfo(String packageName) {
        HwAPPQoEAPKConfig hwAPPQoEAPKConfig = this.mApkConfig;
        hwAPPQoEAPKConfig.packageName = packageName;
        hwAPPQoEAPKConfig.setWlanPlus(1);
        this.mApkConfig.setSwitchType(1);
        this.mApkConfig.setRtt(500.0f);
        this.mApkConfig.setThreshlod((int) AFTER_GOOD_THRESHOLD);
        this.mApkConfig.setTcpResendRate(0.5f);
        this.mApkConfig.setDetectCycle(8);
        this.mApkConfig.setBadCount(5);
        this.mApkConfig.setGoodCount(2);
        this.mApkConfig.setBadContinuousCnt(4);
        this.mApkConfig.setNoise2gTh(-70);
        this.mApkConfig.setNoise5gTh((int) NOISE_TH_5G);
        this.mApkConfig.setLinkSpeed2gTh(27);
        this.mApkConfig.setLinkSpeed5gTh(40);
        this.mApkConfig.setChLoad2gTh((int) WifiProCommonUtils.RESP_CODE_TIMEOUT);
        this.mApkConfig.setChLoad5gTh((int) WifiProCommonUtils.RESP_CODE_TIMEOUT);
        this.mApkConfig.setOtaRateTh((float) OTA_RATE_BAD_TH);
        this.mApkConfig.setTxGoodTh(20.0f);
        this.mApkConfig.setTxBadTh(1.0f);
        this.mApkConfig.setTcpRttTh((float) TCP_RTT_MAYBE_BAD);
    }

    private void getAppConfigInfo(String packageName) {
        HwAPPQoEAPKConfig config = null;
        this.mApkConfigList = this.mHwAppQoeResourceManager.getAPKConfigList();
        List<HwAPPQoEAPKConfig> list = this.mApkConfigList;
        if (list == null) {
            Log.e(TAG, "APKConfigList is null");
            return;
        }
        Iterator<HwAPPQoEAPKConfig> it = list.iterator();
        while (true) {
            if (!it.hasNext()) {
                break;
            }
            HwAPPQoEAPKConfig apkConfig = it.next();
            if (packageName != null && packageName.equals(apkConfig.packageName)) {
                config = apkConfig;
                Log.i(TAG, "find the APP Config by packageName." + config.packageName);
                break;
            }
        }
        if (config == null) {
            Log.i(TAG, "can not find the app " + packageName + ", use the default value.");
            this.mApkConfig = new HwAPPQoEAPKConfig();
            setDefaultApkConfigInfo(packageName);
            return;
        }
        this.mApkConfig = config;
        if (!isParaLegal(this.mApkConfig)) {
            setDefaultApkConfigInfo(packageName);
        }
    }

    private boolean isParaLegal(HwAPPQoEAPKConfig config) {
        if (config == null) {
            return false;
        }
        if (config.getWlanPlus() == 1 && config.getSwitchType() == 0) {
            Log.i(TAG, "app: " + config.packageName + " in blacklist, do not monitor.");
            return true;
        } else if (config.getSwitchType() != 1 && config.getSwitchType() != 2) {
            Log.i(TAG, "Strategy Para is illegal." + config.packageName);
            return false;
        } else if (!isAlgorithmParaLegal(config)) {
            Log.i(TAG, "Alg Para is illegal." + config.packageName);
            return false;
        } else if (isWifiOtaParaLegal(config)) {
            return false;
        } else {
            Log.i(TAG, "Ota Para is illegal." + config.packageName);
            return false;
        }
    }

    private boolean isAlgorithmParaLegal(HwAPPQoEAPKConfig config) {
        if (config == null || config.getRtt() < TCP_DEFAULT || config.getTcpResendRate() < TCP_DEFAULT || config.getThreshold() < 0 || config.getDetectCycle() < 0 || config.getBadCount() < 0 || config.getGoodCount() < 0 || config.getBadCount() < config.getBadContinuousCnt() || config.getDetectCycle() < config.getBadContinuousCnt() || config.getDetectCycle() < config.getBadCount() + config.getGoodCount()) {
            return false;
        }
        Log.i(TAG, "Alg Para is legal." + config.packageName);
        return true;
    }

    private boolean isWifiOtaParaLegal(HwAPPQoEAPKConfig config) {
        if (config == null || config.getNoise2gTh() >= 0 || config.getNoise5gTh() >= 0 || config.getLinkSpeed2gTh() <= 0 || config.getLinkSpeed5gTh() <= 0 || config.getChLoad2gTh() <= 0 || config.getChLoad5gTh() <= 0 || config.getOtaRateTh() <= TCP_DEFAULT || config.getTxGoodTh() <= TCP_DEFAULT || config.getTxBadTh() <= TCP_DEFAULT || config.getTcpRttTh() <= TCP_DEFAULT) {
            return false;
        }
        Log.i(TAG, "Ota Para is legal." + config.packageName);
        return true;
    }

    public void checkAndStartQoeMonitor() {
        if (isAllowedToStartQoeMonitor()) {
            HwAutoConnectManager autoConnectManager = HwAutoConnectManager.getInstance();
            if (autoConnectManager == null) {
                Log.w(TAG, "checkAndNotifyMonitor autoConnectManager is null");
                return;
            }
            HwUidTcpMonitor hwUidTcpMonitor = HwUidTcpMonitor.getInstance(this.mContext);
            if (hwUidTcpMonitor == null) {
                Log.w(TAG, "checkAndStartQoeMonitor hwUidTcpMonitor is null");
                return;
            }
            String pkgName = autoConnectManager.getCurrentPackageName();
            HwAPPQoEAPKConfig hwAPPQoEAPKConfig = this.mApkConfig;
            if (hwAPPQoEAPKConfig == null || !hwAPPQoEAPKConfig.packageName.equals(pkgName)) {
                getAppConfigInfo(pkgName);
                HwAPPQoEAPKConfig hwAPPQoEAPKConfig2 = this.mApkConfig;
                if (hwAPPQoEAPKConfig2 == null) {
                    Log.e(TAG, "mAPKConfig is null !!");
                    this.mTcpQualitySecondRecords.resetNetworkBlockDetectionCfg();
                    return;
                }
                HwAPPQoEUtils.logD(TAG, false, "ApkConfig Config Info:%{public}s", new Object[]{hwAPPQoEAPKConfig2.toString()});
                this.mTcpQualitySecondRecords.setNetworkBlockDetectionCfg(this.mApkConfig.getDetectCycle(), this.mApkConfig.getBadCount(), this.mApkConfig.getGoodCount(), this.mApkConfig.getBadContinuousCnt());
            }
            if (this.mApkConfig.getWlanPlus() == 1 && this.mApkConfig.getSwitchType() == 0) {
                Log.i(TAG, "app: " + pkgName + " in blacklist, do not monitor.");
                return;
            }
            int topUid = autoConnectManager.getCurrentTopUid();
            if (isCachedInternetApp(pkgName)) {
                Log.i(TAG, "start to evaluate network quality for app : " + pkgName + " already in internet lists");
                startQoeMonitor(topUid);
            } else if (hwUidTcpMonitor.isAppAccessInternet(topUid)) {
                Log.i(TAG, "start to evaluate network quality for app : " + pkgName + " and add it to internet lists");
                addInternetAppToCache(pkgName);
                startQoeMonitor(topUid);
            } else {
                Log.i(TAG, "continue check internet for app : " + pkgName);
            }
        }
    }

    public void startMonitor() {
        Log.i(TAG, "startMonitor");
        this.mCurrentUid = -1;
        this.mIsMonitorStarted.set(true);
        registerBroadcastReceiver();
    }

    public void stopMonitor() {
        Log.i(TAG, "stopMonitor");
        this.mIsMonitorStarted.set(false);
        this.mIsMpLinkState = false;
        unRegisterBroadcastReceiver();
        stopQoeMonitor();
    }

    public void notifyAppChanged() {
        Log.i(TAG, "notifyAppChanged mIsMonitorStarted = " + this.mIsMonitorStarted.get());
        if (this.mIsMonitorStarted.get()) {
            this.mIsMpLinkState = false;
            stopQoeMonitor();
            checkAndStartQoeMonitor();
        }
    }

    private void startQoeMonitor(int topUid) {
        this.mCurrentUid = topUid;
        this.mQoeMonitorHandler.sendEmptyMessage(1);
    }

    /* access modifiers changed from: private */
    public void stopQoeMonitor() {
        Log.i(TAG, "stopQoeMonitor");
        this.mIsCurrentAppMonitoring = false;
        if (this.mQoeMonitorHandler.hasMessages(1)) {
            this.mQoeMonitorHandler.removeMessages(1);
        }
        if (this.mQoeMonitorHandler.hasMessages(2)) {
            this.mQoeMonitorHandler.removeMessages(2);
        }
        this.mCurrentUid = -1;
    }

    private boolean isAllowedToStartQoeMonitor() {
        if (this.mIsMonitorStarted.get() && isScreenOn(this.mContext) && !this.mIsCurrentAppMonitoring && !this.mIsMpLinkState) {
            return true;
        }
        Log.i(TAG, "checkAndStartQoeMonitor mIsCurrentAppMonitoring = " + this.mIsCurrentAppMonitoring + ", mIsMpLinkState = " + this.mIsMpLinkState + ", isScreenOn = " + isScreenOn(this.mContext) + ", mIsMonitorStarted = " + this.mIsMonitorStarted.get());
        return false;
    }

    /* access modifiers changed from: private */
    public RssiPacketCountInfo getRssiPacketCountInfo() {
        Bundle result = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 80, null);
        RssiPacketCountInfo rssiPacketCountInfo = new RssiPacketCountInfo();
        if (result != null) {
            return result.getParcelable(RSSI_PACKET_COUNT_INFO);
        }
        return rssiPacketCountInfo;
    }

    /* access modifiers changed from: private */
    public int[] sendQoeCmd(int cmd, int uid) {
        Bundle data = new Bundle();
        data.putInt(KEY_QOE_CMD, cmd);
        data.putInt(KEY_QOE_ARG, uid);
        Bundle resultCmd = WifiManagerEx.ctrlHwWifiNetwork(HwWifiProService.SERVICE_NAME, 8, data);
        int[] results = new int[0];
        if (resultCmd != null) {
            return resultCmd.getIntArray(KEY_QOE_RESULT_CMD);
        }
        return results;
    }

    /* access modifiers changed from: private */
    public class QoeMonitorHandler extends Handler {
        public static final int MSG_DATA_READY = 8;
        public static final int MSG_ENTER_MPLINK = 3;
        public static final int MSG_EXIT_MPLINK = 4;
        public static final int MSG_GET_TCP_INFO = 2;
        public static final int MSG_INIT_TCP_INFO = 1;
        public static final int MSG_REMOVE_APP = 7;
        public static final int MSG_SCREEN_OFF = 6;
        public static final int MSG_SCREEN_ON = 5;
        int mNoDataTimes = 0;
        List<HwApkTcpSecInfo> mPeriodInfoLists = new ArrayList();
        int mThreshold = HwQoeQualityMonitor.INIT_THRESHOLD;

        public QoeMonitorHandler(Looper looper) {
            super(looper);
        }

        public void handleMessage(Message msg) {
            if (msg != null) {
                switch (msg.what) {
                    case 1:
                        handleInitTcpMessage();
                        break;
                    case 2:
                        handleGetTcpMessage();
                        break;
                    case 3:
                        handleEnterMpLinkState();
                        break;
                    case 4:
                        handleExitMpLinkState();
                        break;
                    case 5:
                        if (HwQoeQualityMonitor.this.mIsMonitorStarted.get()) {
                            Log.i(HwQoeQualityMonitor.TAG, "handleMessage MSG_SCREEN_ON");
                            HwQoeQualityMonitor.this.checkAndStartQoeMonitor();
                            break;
                        }
                        break;
                    case 6:
                        if (HwQoeQualityMonitor.this.mIsMonitorStarted.get()) {
                            Log.i(HwQoeQualityMonitor.TAG, "handleMessage MSG_SCREEN_OFF");
                            HwQoeQualityMonitor.this.stopQoeMonitor();
                            break;
                        }
                        break;
                    case 7:
                        handleRemoveAppMessage(msg);
                        break;
                    case 8:
                        handleDataReadyMessage(msg);
                        break;
                }
                super.handleMessage(msg);
            }
        }

        private void handleExitMpLinkState() {
            if (HwQoeQualityMonitor.this.mIsMonitorStarted.get()) {
                Log.i(HwQoeQualityMonitor.TAG, "handleMessage MSG_EXIT_MPLINK");
                boolean unused = HwQoeQualityMonitor.this.mIsMpLinkState = false;
                HwQoeQualityMonitor.this.checkAndStartQoeMonitor();
            }
        }

        private void handleEnterMpLinkState() {
            if (HwQoeQualityMonitor.this.mIsMonitorStarted.get()) {
                Log.i(HwQoeQualityMonitor.TAG, "handleMessage MSG_ENTER_MPLINK");
                boolean unused = HwQoeQualityMonitor.this.mIsMpLinkState = true;
                HwQoeQualityMonitor.this.stopQoeMonitor();
            }
        }

        private void handleInitTcpMessage() {
            HwQoeQualityMonitor hwQoeQualityMonitor = HwQoeQualityMonitor.this;
            if (!hwQoeQualityMonitor.isScreenOn(hwQoeQualityMonitor.mContext)) {
                Log.i(HwQoeQualityMonitor.TAG, "handleInitTcpMessage block monitor for screen off");
                return;
            }
            boolean unused = HwQoeQualityMonitor.this.mIsCurrentAppMonitoring = true;
            HwQoeQualityMonitor hwQoeQualityMonitor2 = HwQoeQualityMonitor.this;
            int[] results = hwQoeQualityMonitor2.sendQoeCmd(23, hwQoeQualityMonitor2.mCurrentUid);
            RssiPacketCountInfo rssiPacketCountInfo = HwQoeQualityMonitor.this.getRssiPacketCountInfo();
            if (results == null || results.length < 10) {
                Log.i(HwQoeQualityMonitor.TAG, "MSG_INIT_TCP_INFO query tcp info invalid!");
                sendEmptyMessageDelayed(1, 1000);
                return;
            }
            HwQoeQualityMonitor hwQoeQualityMonitor3 = HwQoeQualityMonitor.this;
            HwApkTcpInfo unused2 = hwQoeQualityMonitor3.mLastHwApkTcpInfo = new HwApkTcpInfo(hwQoeQualityMonitor3.mCurrentUid, results, rssiPacketCountInfo);
            sendEmptyMessageDelayed(2, 1000);
            this.mNoDataTimes = 0;
            this.mPeriodInfoLists.clear();
            HwQoeQualityMonitor.this.mTcpQualitySecondRecords.resetTcpQualityRecord();
        }

        private void handleGetTcpMessage() {
            HwQoeQualityMonitor hwQoeQualityMonitor = HwQoeQualityMonitor.this;
            if (!hwQoeQualityMonitor.isScreenOn(hwQoeQualityMonitor.mContext)) {
                Log.i(HwQoeQualityMonitor.TAG, "handleGetTcpMessage block monitor for screen off");
                return;
            }
            HwQoeQualityMonitor hwQoeQualityMonitor2 = HwQoeQualityMonitor.this;
            int[] results = hwQoeQualityMonitor2.sendQoeCmd(23, hwQoeQualityMonitor2.mCurrentUid);
            RssiPacketCountInfo rssiPacketCountInfo = HwQoeQualityMonitor.this.getRssiPacketCountInfo();
            if (results == null || results.length < 10) {
                Log.i(HwQoeQualityMonitor.TAG, "MSG_GET_TCP_INFO query tcp info invalid!");
            } else {
                HwQoeQualityMonitor hwQoeQualityMonitor3 = HwQoeQualityMonitor.this;
                HwApkTcpInfo unused = hwQoeQualityMonitor3.mCurrentHwApkTcpInfo = new HwApkTcpInfo(hwQoeQualityMonitor3.mCurrentUid, results, rssiPacketCountInfo);
                HwQoeQualityMonitor hwQoeQualityMonitor4 = HwQoeQualityMonitor.this;
                HwApkTcpSecInfo secInfo = new HwApkTcpSecInfo(hwQoeQualityMonitor4.mLastHwApkTcpInfo, HwQoeQualityMonitor.this.mCurrentHwApkTcpInfo);
                int periodTime = HwQoeQualityMonitor.this.mPeriodTime / 1000;
                if (secInfo.getPeriodTcpTxPacket() == HwQoeQualityMonitor.TCP_DEFAULT && secInfo.getPeriodTcpRxPacket() == HwQoeQualityMonitor.TCP_DEFAULT) {
                    Log.i(HwQoeQualityMonitor.TAG, "MSG_GET_TCP_INFO no data times = " + this.mNoDataTimes);
                    this.mNoDataTimes = this.mNoDataTimes + 1;
                    HwQoeQualityMonitor.this.mTcpQualitySecondRecords.updateCurrentPeriodQuality(0);
                    if (this.mNoDataTimes >= periodTime) {
                        this.mPeriodInfoLists.clear();
                    }
                    sendEmptyMessageDelayed(2, 1000);
                    return;
                }
                this.mNoDataTimes = 0;
                this.mPeriodInfoLists = HwQoeQualityMonitor.this.updatePeriodInfoList(this.mPeriodInfoLists, secInfo);
                analyseApkQuality(HwQoeQualityMonitor.this.getApkQualityOfCurrentPeriod(this.mPeriodInfoLists));
                HwQoeQualityMonitor hwQoeQualityMonitor5 = HwQoeQualityMonitor.this;
                HwApkTcpInfo unused2 = hwQoeQualityMonitor5.mLastHwApkTcpInfo = hwQoeQualityMonitor5.mCurrentHwApkTcpInfo;
            }
            sendEmptyMessageDelayed(2, 1000);
        }

        private void handleRemoveAppMessage(Message msg) {
            if (msg != null) {
                Log.i(HwQoeQualityMonitor.TAG, "handleMessage MSG_REMOVE_APP");
                if (msg.obj instanceof String) {
                    HwQoeQualityMonitor.this.removeInternetAppFromCache((String) msg.obj);
                    return;
                }
                Log.e(HwQoeQualityMonitor.TAG, "handleMessage MSG_REMOVE_APP:Class is not match");
            }
        }

        private void handleDataReadyMessage(Message msg) {
            if (HwQoeQualityMonitor.this.mIsMonitorStarted.get() && msg != null) {
                Log.i(HwQoeQualityMonitor.TAG, "handleMessage MSG_DATA_READY");
                HwWifiConnectivityMonitor hwWifiConnectivityMonitor = HwWifiConnectivityMonitor.getInstance();
                if (hwWifiConnectivityMonitor == null) {
                    Log.w(HwQoeQualityMonitor.TAG, "handleDataReadyMessage hwWifiConnectivityMonitor is null");
                    return;
                }
                Log.i(HwQoeQualityMonitor.TAG, "handleDataReadyMessage notifyTopUidBad");
                HwQoeQualityMonitor.this.mTcpQualitySecondRecords.resetTcpQualityRecord();
                if (HwQoeQualityMonitor.this.isAllowWiFi2Cell(this.mPeriodInfoLists)) {
                    hwWifiConnectivityMonitor.notifyTopUidBad(HwQoeQualityMonitor.this.mCurrentUid);
                }
            }
        }

        private void analyseApkQuality(int quality) {
            if (quality == 1) {
                Log.i(HwQoeQualityMonitor.TAG, "MSG_APP_STATE_BAD");
                HwQoeQualityMonitor.this.mTcpQualitySecondRecords.updateCurrentPeriodQuality(1);
            } else if (quality == 2) {
                Log.i(HwQoeQualityMonitor.TAG, "MSG_APP_STATE_GOOD");
                HwQoeQualityMonitor.this.mTcpQualitySecondRecords.updateCurrentPeriodQuality(2);
            } else {
                Log.i(HwQoeQualityMonitor.TAG, "MSG_APP_STATE_UNKNOW");
                HwQoeQualityMonitor.this.mTcpQualitySecondRecords.updateCurrentPeriodQuality(0);
            }
        }
    }

    /* access modifiers changed from: private */
    public boolean isScreenOn(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService("power");
        if (powerManager == null || !powerManager.isScreenOn()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public List<HwApkTcpSecInfo> updatePeriodInfoList(List<HwApkTcpSecInfo> periodInfoLists, HwApkTcpSecInfo secInfo) {
        if (periodInfoLists.size() >= this.mPeriodTime / 1000 && periodInfoLists.size() > 0) {
            periodInfoLists.remove(0);
        }
        if (secInfo.getPeriodRttPacket() == TCP_DEFAULT && periodInfoLists.size() > 0) {
            HwApkTcpSecInfo lastInfo = periodInfoLists.get(periodInfoLists.size() - 1);
            secInfo.setPeriodRttPacket(lastInfo.getPeriodRttPacket());
            secInfo.setPeriodRtt(lastInfo.getPeriodRtt());
            Log.i(TAG, "RttPacket is 0 ,update secInfo with last");
        }
        periodInfoLists.add(secInfo);
        return periodInfoLists;
    }

    /* access modifiers changed from: private */
    public int getApkQualityOfCurrentPeriod(List<HwApkTcpSecInfo> secInfoList) {
        WifiInfo info = this.mWifiManager.getConnectionInfo();
        if (info == null || info.getBSSID() == null) {
            Log.i(TAG, "getApkQualityOfCurrentPeriod wifi info invalid");
            return 0;
        }
        int rssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(info.getFrequency(), info.getRssi());
        HwApkTcpSecInfo avgInfo = getAvgInfoByTcpSecInfoList(secInfoList);
        float[] aiInfos = {(float) info.getFrequency(), (float) info.getRssi(), 0.0f, avgInfo.getPeriodTxGood(), avgInfo.getPeriodTxBad(), avgInfo.getPeriodTcpTxPacket(), avgInfo.getPeriodTcpRxPacket(), avgInfo.getPeriodTcpRsPacket(), avgInfo.getPeriodTxByte(), avgInfo.getPeriodRxByte(), avgInfo.getPeriodRtt(), avgInfo.getPeriodRttPacket(), avgInfo.getOtaRate(), avgInfo.getTcpResendRate()};
        trackApkQualityDetail(aiInfos);
        if (this.mHwApkQoeAiCalc.judgeQualitByAi(aiInfos, this.mApkConfig.getThreshold())) {
            return updateResultIfHasGoodRssi(avgInfo, 1, rssiLevel);
        }
        return updateResultIfBadRttAndReTx(avgInfo, 2);
    }

    private void trackApkQualityDetail(float[] aiInfos) {
        HwHiLog.d(TAG, false, "trackApkQualityDetail tx = %{public}s, rx = %{public}s, reTx = %{public}s, rtt = %{public}s, rttPkts = %{public}s, tr = %{public}s, wlan0Tx = %{public}s, wlan0Rx = %{public}s, txGood = %{public}s, txBad = %{public}s, otaRate = %{public}s, freq = %{public}s, rssi = %{public}s, dnsFail = %{public}s", new Object[]{String.valueOf(aiInfos[5]), String.valueOf(aiInfos[6]), String.valueOf(aiInfos[7]), String.valueOf(aiInfos[10]), String.valueOf(aiInfos[11]), String.valueOf(aiInfos[13]), String.valueOf(aiInfos[8]), String.valueOf(aiInfos[9]), String.valueOf(aiInfos[3]), String.valueOf(aiInfos[4]), String.valueOf(aiInfos[12]), String.valueOf(aiInfos[0]), String.valueOf(aiInfos[1]), String.valueOf(aiInfos[2])});
    }

    private int updateResultIfHasGoodRssi(HwApkTcpSecInfo avgInfo, int result, int rssiLevel) {
        if (avgInfo == null) {
            Log.e(TAG, "updateResultIfHasGoodRssi invalid parameter");
            return 0;
        } else if (avgInfo.getTcpResendRate() != this.mApkConfig.getTcpResendRate() || avgInfo.getOtaRate() != TCP_DEFAULT || rssiLevel < 4 || avgInfo.getPeriodRtt() >= this.mApkConfig.getRtt()) {
            return result;
        } else {
            Log.i(TAG, "updateResultIfHasGoodRssi has good quality");
            return 2;
        }
    }

    private int updateResultIfBadRttAndReTx(HwApkTcpSecInfo avgInfo, int result) {
        if (avgInfo == null) {
            Log.e(TAG, "updateResultIfBadRttAndReTx invalid parameter");
            return 0;
        } else if (avgInfo.getPeriodRtt() == this.mApkConfig.getRtt() && avgInfo.getPeriodRttPacket() == TCP_DEFAULT) {
            float periodTcpTxPacket = avgInfo.getPeriodTcpTxPacket();
            float tcpResendRate = avgInfo.getTcpResendRate();
            float periodTcpRxPacket = avgInfo.getPeriodTcpRxPacket();
            if (tcpResendRate >= this.mApkConfig.getTcpResendRate() && periodTcpRxPacket > 1.0f) {
                return 1;
            }
            if (tcpResendRate >= this.mApkConfig.getTcpResendRate() - MORE_PKTS_LOW_BAD_RATE && periodTcpRxPacket > 1.0f) {
                return 1;
            }
            if (tcpResendRate >= this.mApkConfig.getTcpResendRate() - 0.3f && periodTcpTxPacket >= MIN_TX_PKTS && periodTcpTxPacket < 20.0f && periodTcpRxPacket > 1.0f && periodTcpRxPacket < MORE_RX_PKTS) {
                return 1;
            }
            Log.i(TAG, "updateResultIfBadRttAndReTx result unknown for rtt is zero");
            return 0;
        } else if (avgInfo.getPeriodTcpRxPacket() > 1.0f) {
            return 2;
        } else {
            Log.i(TAG, "updateResultIfBadRttAndReTx result unknown for both rtt is not zero and rx less than one");
            return 0;
        }
    }

    private HwApkTcpSecInfo getAvgInfoByTcpSecInfoList(List<HwApkTcpSecInfo> secInfoList) {
        HwApkTcpSecInfo avgInfo = new HwApkTcpSecInfo();
        int size = secInfoList.size();
        if (size == 0) {
            Log.i(TAG, "secInfoList empty");
            return avgInfo;
        }
        float periodTotalRtt = TCP_DEFAULT;
        float periodTotalRttPacket = TCP_DEFAULT;
        float periodTotalTcpTxPacket = TCP_DEFAULT;
        float periodTotalTcpRxPacket = TCP_DEFAULT;
        float periodTotalTcpRsPacket = TCP_DEFAULT;
        float periodTotalTxGood = TCP_DEFAULT;
        float periodTotalTxBad = TCP_DEFAULT;
        float periodTotalTxByte = TCP_DEFAULT;
        float periodTotalRxByte = TCP_DEFAULT;
        for (HwApkTcpSecInfo tempSecInfo : secInfoList) {
            periodTotalRtt += tempSecInfo.getPeriodRtt();
            periodTotalRttPacket += tempSecInfo.getPeriodRttPacket();
            periodTotalTcpTxPacket += tempSecInfo.getPeriodTcpTxPacket();
            periodTotalTcpRxPacket += tempSecInfo.getPeriodTcpRxPacket();
            periodTotalTcpRsPacket += tempSecInfo.getPeriodTcpRsPacket();
            periodTotalTxGood += tempSecInfo.getPeriodTxGood();
            periodTotalTxBad += tempSecInfo.getPeriodTxBad();
            periodTotalTxByte += tempSecInfo.getPeriodTxByte();
            periodTotalRxByte += tempSecInfo.getPeriodRxByte();
        }
        float avgOtaRate = TCP_DEFAULT;
        avgInfo.setPeriodRtt(periodTotalRttPacket > TCP_DEFAULT ? periodTotalRtt / periodTotalRttPacket : 0.0f);
        avgInfo.setTcpResendRate(periodTotalTcpTxPacket > TCP_DEFAULT ? periodTotalTcpRsPacket / periodTotalTcpTxPacket : 0.0f);
        if (periodTotalTxGood + periodTotalTxBad > TCP_DEFAULT) {
            avgOtaRate = periodTotalTxBad / (periodTotalTxGood + periodTotalTxBad);
        }
        avgInfo.setOtaRate(avgOtaRate);
        avgInfo.setPeriodRttPacket(periodTotalRttPacket / ((float) size));
        avgInfo.setPeriodTcpTxPacket(periodTotalTcpTxPacket / ((float) size));
        avgInfo.setPeriodTcpRxPacket(periodTotalTcpRxPacket / ((float) size));
        avgInfo.setPeriodTcpRsPacket(periodTotalTcpRsPacket / ((float) size));
        avgInfo.setPeriodTxGood(periodTotalTxGood / ((float) size));
        avgInfo.setPeriodTxBad(periodTotalTxBad / ((float) size));
        avgInfo.setPeriodTxByte(periodTotalTxByte / ((float) size));
        avgInfo.setPeriodRxByte(periodTotalRxByte / ((float) size));
        return avgInfo;
    }

    /* access modifiers changed from: private */
    public class HwApkTcpInfo {
        private int rttPacketNum;
        private int rttTime;
        private long rxByte;
        private int tcpReSendPacket;
        private int tcpRxPacket;
        private int tcpTxPacket;
        private int txBad;
        private long txByte;
        private int txGood;

        public int getRttTime() {
            return this.rttTime;
        }

        public int getRttPacketNum() {
            return this.rttPacketNum;
        }

        public int getTcpTxPacket() {
            return this.tcpTxPacket;
        }

        public int getTcpRxPacket() {
            return this.tcpRxPacket;
        }

        public int getTcpReSendPacket() {
            return this.tcpReSendPacket;
        }

        public int getTxGood() {
            return this.txGood;
        }

        public int getTxBad() {
            return this.txBad;
        }

        public long getTxByte() {
            return this.txByte;
        }

        public long getRxByte() {
            return this.rxByte;
        }

        public HwApkTcpInfo() {
            this.rttTime = 0;
            this.rttPacketNum = 0;
            this.tcpTxPacket = 0;
            this.tcpRxPacket = 0;
            this.tcpReSendPacket = 0;
            this.txByte = 0;
            this.rxByte = 0;
        }

        public HwApkTcpInfo(int uid, int[] info, RssiPacketCountInfo rssiPacketCountInfo) {
            this.rttTime = info[0];
            this.rttPacketNum = info[1];
            this.tcpTxPacket = info[6];
            this.tcpRxPacket = info[7];
            this.tcpReSendPacket = info[8];
            Log.i(HwQoeQualityMonitor.TAG, "rttTime=" + this.rttTime + ", rttPacketNum=" + this.rttPacketNum + ", tcpTxPacket=" + this.tcpTxPacket + ", tcpRxPacket=" + this.tcpRxPacket + ", tcpReSendPacket=" + this.tcpReSendPacket);
            this.txGood = rssiPacketCountInfo.txgood;
            this.txBad = rssiPacketCountInfo.txbad;
            this.txByte = TrafficStats.getTxBytes(HwQoeQualityMonitor.IFACE);
            this.rxByte = TrafficStats.getRxBytes(HwQoeQualityMonitor.IFACE);
        }
    }

    /* access modifiers changed from: private */
    public class HwApkTcpSecInfo {
        private float otaRate;
        private float periodRtt;
        private float periodRttAvg;
        private float periodRttPacket;
        private float periodRxByte;
        private float periodTcpRsPacket;
        private float periodTcpRxPacket;
        private float periodTcpTxPacket;
        private float periodTxBad;
        private float periodTxByte;
        private float periodTxGood;
        private float tcpResendRate;

        public HwApkTcpSecInfo() {
            this.periodRtt = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodRttPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTcpTxPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTcpRxPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTcpRsPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTxByte = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodRxByte = HwQoeQualityMonitor.TCP_DEFAULT;
            this.tcpResendRate = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTxGood = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTxBad = HwQoeQualityMonitor.TCP_DEFAULT;
            this.otaRate = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodRttAvg = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodRtt = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodRttPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTcpTxPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTcpRxPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTcpRsPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTxByte = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodRxByte = HwQoeQualityMonitor.TCP_DEFAULT;
            this.tcpResendRate = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTxGood = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTxBad = HwQoeQualityMonitor.TCP_DEFAULT;
            this.otaRate = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodRttAvg = HwQoeQualityMonitor.TCP_DEFAULT;
        }

        public HwApkTcpSecInfo(HwApkTcpInfo initInfo, HwApkTcpInfo curInfo) {
            this.periodRtt = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodRttPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTcpTxPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTcpRxPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTcpRsPacket = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTxByte = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodRxByte = HwQoeQualityMonitor.TCP_DEFAULT;
            this.tcpResendRate = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTxGood = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodTxBad = HwQoeQualityMonitor.TCP_DEFAULT;
            this.otaRate = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodRttAvg = HwQoeQualityMonitor.TCP_DEFAULT;
            this.periodRttPacket = (float) (curInfo.getRttPacketNum() - initInfo.getRttPacketNum());
            this.periodRtt = (float) (curInfo.getRttTime() - initInfo.getRttTime());
            this.periodTcpTxPacket = (float) (curInfo.getTcpTxPacket() - initInfo.getTcpTxPacket());
            this.periodTcpRxPacket = (float) (curInfo.getTcpRxPacket() - initInfo.getTcpRxPacket());
            this.periodTcpRsPacket = (float) (curInfo.getTcpReSendPacket() - initInfo.getTcpReSendPacket());
            this.periodTxGood = (float) (curInfo.getTxGood() - initInfo.getTxGood());
            this.periodTxBad = (float) (curInfo.getTxBad() - initInfo.getTxBad());
            this.periodTxByte = (float) (curInfo.getTxByte() - initInfo.getTxByte());
            this.periodRxByte = (float) (curInfo.getRxByte() - initInfo.getRxByte());
            float f = this.periodRttPacket;
            if (f != HwQoeQualityMonitor.TCP_DEFAULT) {
                this.periodRttAvg = this.periodRtt / f;
            }
            float f2 = this.periodTxGood;
            float f3 = this.periodTxBad;
            if (f2 + f3 != HwQoeQualityMonitor.TCP_DEFAULT) {
                this.otaRate = f3 / (f2 + f3);
            }
            float f4 = this.periodTcpTxPacket;
            if (f4 != HwQoeQualityMonitor.TCP_DEFAULT) {
                this.tcpResendRate = this.periodTcpRsPacket / f4;
            }
        }

        public float getPeriodRtt() {
            return this.periodRtt;
        }

        public void setPeriodRtt(float periodRtt2) {
            this.periodRtt = periodRtt2;
        }

        public float getPeriodRttPacket() {
            return this.periodRttPacket;
        }

        public void setPeriodRttPacket(float periodRttPacket2) {
            this.periodRttPacket = periodRttPacket2;
        }

        public float getPeriodTcpTxPacket() {
            return this.periodTcpTxPacket;
        }

        public void setPeriodTcpTxPacket(float periodTcpTxPacket2) {
            this.periodTcpTxPacket = periodTcpTxPacket2;
        }

        public float getPeriodTcpRxPacket() {
            return this.periodTcpRxPacket;
        }

        public void setPeriodTcpRxPacket(float periodTcpRxPacket2) {
            this.periodTcpRxPacket = periodTcpRxPacket2;
        }

        public float getPeriodTcpRsPacket() {
            return this.periodTcpRsPacket;
        }

        public void setPeriodTcpRsPacket(float periodTcpRsPacket2) {
            this.periodTcpRsPacket = periodTcpRsPacket2;
        }

        public float getPeriodTxByte() {
            return this.periodTxByte;
        }

        public void setPeriodTxByte(float periodTxByte2) {
            this.periodTxByte = periodTxByte2;
        }

        public float getPeriodRxByte() {
            return this.periodRxByte;
        }

        public void setPeriodRxByte(float periodRxByte2) {
            this.periodRxByte = periodRxByte2;
        }

        public float getTcpResendRate() {
            return this.tcpResendRate;
        }

        public void setTcpResendRate(float tcpResendRate2) {
            this.tcpResendRate = tcpResendRate2;
        }

        public float getPeriodTxGood() {
            return this.periodTxGood;
        }

        public void setPeriodTxGood(float periodTxGood2) {
            this.periodTxGood = periodTxGood2;
        }

        public float getPeriodTxBad() {
            return this.periodTxBad;
        }

        public void setPeriodTxBad(float periodTxBad2) {
            this.periodTxBad = periodTxBad2;
        }

        public float getOtaRate() {
            return this.otaRate;
        }

        public void setOtaRate(float otaRate2) {
            this.otaRate = otaRate2;
        }

        public float getPeriodRttAvg() {
            return this.periodRttAvg;
        }

        public void setPeriodRttAvg(float periodRttAvg2) {
            this.periodRttAvg = periodRttAvg2;
        }
    }

    /* access modifiers changed from: private */
    public class TcpQualitySecondRecords {
        private static final int MAX_QUALITY_QUEUE_SIZE = 12;
        private static final int MIN_QUALITY_QUEUE_SIZE = 4;
        public static final int TCP_BAD = 1;
        public static final int TCP_GOOD = 2;
        public static final int TCP_INITIAL = -1;
        public static final int TCP_UNKNOWN = 0;
        private int mBadQualityNumThreshold = 5;
        private int mBadQualityRecordCounter = 0;
        private int mContinueBadQualityCounter = 0;
        private int mContinuesBadQualityNumThreshold = 4;
        private int mDetectionPeriod = 8;
        private int mGoodQualityNumThreshold = 2;
        private int mGoodQualityRecordCounter = 0;
        private int[] mTcpPeriodQualityQueue = new int[12];

        public TcpQualitySecondRecords() {
            resetNetworkBlockDetectionCfg();
        }

        public boolean setNetworkBlockDetectionCfg(int detectionPeriod, int badQualityNumThreshold, int goodQualityNumThreshold, int continuesBadQualityNumThreshold) {
            if (detectionPeriod > 12 || detectionPeriod < 4 || badQualityNumThreshold > detectionPeriod || goodQualityNumThreshold > detectionPeriod || continuesBadQualityNumThreshold > detectionPeriod) {
                Log.e(HwQoeQualityMonitor.TAG, "setNetworkBlockDetectionCfg fail invalid param ");
                resetNetworkBlockDetectionCfg();
                return false;
            }
            this.mDetectionPeriod = detectionPeriod;
            this.mBadQualityNumThreshold = badQualityNumThreshold;
            this.mGoodQualityNumThreshold = goodQualityNumThreshold;
            this.mContinuesBadQualityNumThreshold = continuesBadQualityNumThreshold;
            resetTcpQualityRecord();
            Log.i(HwQoeQualityMonitor.TAG, "setNetworkBlockDetectionCfg success, detectionPeriod is " + detectionPeriod + ", badQualityNumThreshold is " + badQualityNumThreshold + ", goodQualityNumThreshold is " + goodQualityNumThreshold + ", continuesbadQualityNumThreshold is " + continuesBadQualityNumThreshold);
            return true;
        }

        public void resetTcpQualityRecord() {
            Log.i(HwQoeQualityMonitor.TAG, "resetTcpQualityRecord");
            Arrays.fill(this.mTcpPeriodQualityQueue, -1);
            this.mContinueBadQualityCounter = 0;
            this.mGoodQualityRecordCounter = 0;
            this.mBadQualityRecordCounter = 0;
        }

        public void resetNetworkBlockDetectionCfg() {
            Log.i(HwQoeQualityMonitor.TAG, "resetNetworkBlockDetectionCfg");
            this.mDetectionPeriod = 8;
            this.mBadQualityNumThreshold = 5;
            this.mGoodQualityNumThreshold = 2;
            this.mContinuesBadQualityNumThreshold = 4;
            resetTcpQualityRecord();
        }

        public void updateCurrentPeriodQuality(int quality) {
            addNewQualityRecord(quality);
            if (isNetworkBlock(quality)) {
                triggerNetworkBlockNotification();
            }
        }

        private boolean isNetworkBlock(int currentQuality) {
            if (currentQuality == 2) {
                return false;
            }
            if (this.mContinueBadQualityCounter == this.mContinuesBadQualityNumThreshold) {
                Log.i(HwQoeQualityMonitor.TAG, "isNetworkBlock, continues bad quality detected, network is block");
                return true;
            } else if (this.mTcpPeriodQualityQueue[this.mDetectionPeriod - 1] != 1) {
                return false;
            } else {
                if (currentQuality != 1 || this.mGoodQualityRecordCounter > this.mGoodQualityNumThreshold || this.mBadQualityRecordCounter < this.mBadQualityNumThreshold) {
                    if (currentQuality == 0 && this.mGoodQualityRecordCounter <= this.mGoodQualityNumThreshold && this.mBadQualityRecordCounter >= this.mBadQualityNumThreshold - 1) {
                        int i = 0;
                        while (i < this.mDetectionPeriod) {
                            int i2 = this.mTcpPeriodQualityQueue[i];
                            if (i2 == 1) {
                                Log.i(HwQoeQualityMonitor.TAG, "isNetworkBlock, current quality is unknown, but it tend to be bad from the histroy record, network is block");
                                return true;
                            } else if (i2 == 2) {
                                return false;
                            } else {
                                i++;
                            }
                        }
                    }
                    return false;
                }
                Log.i(HwQoeQualityMonitor.TAG, "isNetworkBlock, bad quality frequently detected during the user maximum tolerable period, network is block");
                return true;
            }
        }

        private void addNewQualityRecord(int quality) {
            if (this.mTcpPeriodQualityQueue[this.mDetectionPeriod - 1] == 1) {
                this.mBadQualityRecordCounter--;
            }
            if (this.mTcpPeriodQualityQueue[this.mDetectionPeriod - 1] == 2) {
                this.mGoodQualityRecordCounter--;
            }
            for (int i = this.mDetectionPeriod - 1; i >= 1; i--) {
                int[] iArr = this.mTcpPeriodQualityQueue;
                iArr[i] = iArr[i - 1];
            }
            this.mTcpPeriodQualityQueue[0] = quality;
            if (quality == 1) {
                this.mBadQualityRecordCounter++;
                this.mContinueBadQualityCounter++;
            } else if (quality != 2) {
                int i2 = this.mContinueBadQualityCounter;
                if (i2 > 0) {
                    this.mContinueBadQualityCounter = i2 + 1;
                }
            } else {
                this.mGoodQualityRecordCounter++;
                this.mContinueBadQualityCounter = 0;
            }
        }

        private void triggerNetworkBlockNotification() {
            Log.i(HwQoeQualityMonitor.TAG, "triggerNetworkBlockNotification enter(), network block detected");
            HwQoeQualityMonitor.this.mQoeMonitorHandler.sendEmptyMessage(8);
            resetTcpQualityRecord();
        }
    }

    private void registerBroadcastReceiver() {
        if (!this.mIsRegister) {
            this.mIntentFilter.addAction("com.android.server.hidata.arbitration.HwArbitrationStateMachine");
            this.mIntentFilter.addAction("android.intent.action.SCREEN_ON");
            this.mIntentFilter.addAction("android.intent.action.SCREEN_OFF");
            this.mContext.registerReceiver(this.mWifiQoeBroadcastReceiver, this.mIntentFilter, "com.huawei.hidata.permission.MPLINK_START_CHECK", null);
            this.mPackageRemovedFilter.addAction("android.intent.action.PACKAGE_REMOVED");
            this.mPackageRemovedFilter.addDataScheme("package");
            this.mContext.registerReceiver(this.mWifiQoeBroadcastReceiver, this.mPackageRemovedFilter);
            this.mIsRegister = true;
        }
    }

    private void unRegisterBroadcastReceiver() {
        if (this.mIsRegister) {
            this.mContext.unregisterReceiver(this.mWifiQoeBroadcastReceiver);
            this.mIsRegister = false;
        }
    }

    /* access modifiers changed from: private */
    public void handleMpLinkStateChange(int network) {
        if (network == 801) {
            this.mQoeMonitorHandler.sendEmptyMessage(3);
        } else if (network == 800) {
            this.mQoeMonitorHandler.sendEmptyMessage(4);
        } else {
            Log.i(TAG, "handleMpLinkStateChange unknown network : " + network);
        }
    }

    private void addInternetAppToCache(String packageName) {
        if (this.mCachedInternetApps == null || TextUtils.isEmpty(packageName)) {
            Log.w(TAG, "invalid parameter");
        } else if (this.mCachedInternetApps.size() >= 500) {
            Log.w(TAG, "reach cache list max count");
        } else {
            synchronized (this.mLock) {
                if (this.mCachedInternetApps.indexOf(packageName) == -1) {
                    this.mCachedInternetApps.add(packageName);
                }
            }
        }
    }

    /* access modifiers changed from: private */
    public void removeInternetAppFromCache(String packageName) {
        if (this.mCachedInternetApps == null || TextUtils.isEmpty(packageName)) {
            Log.w(TAG, "invalid parameter");
            return;
        }
        synchronized (this.mLock) {
            if (this.mCachedInternetApps.indexOf(packageName) != -1) {
                this.mCachedInternetApps.remove(packageName);
                Log.i(TAG, "remove app : " + packageName + " success");
            }
        }
    }

    private boolean isCachedInternetApp(String packageName) {
        if (this.mCachedInternetApps == null || TextUtils.isEmpty(packageName)) {
            Log.w(TAG, "invalid parameter");
            return false;
        }
        synchronized (this.mLock) {
            if (this.mCachedInternetApps.indexOf(packageName) != -1) {
                return true;
            }
            return false;
        }
    }

    private class WifiQoeBroadcastReceiver extends BroadcastReceiver {
        private WifiQoeBroadcastReceiver() {
        }

        public void onReceive(Context context, Intent intent) {
            if (intent == null || intent.getAction() == null) {
                Log.w(HwQoeQualityMonitor.TAG, "WifiQoeBroadcastReceiver onReceive invalid parameter");
                return;
            }
            String action = intent.getAction();
            Log.i(HwQoeQualityMonitor.TAG, "onReceive action:" + action);
            if ("com.android.server.hidata.arbitration.HwArbitrationStateMachine".equals(action)) {
                HwQoeQualityMonitor.this.handleMpLinkStateChange(intent.getIntExtra("MPLinkSuccessNetworkKey", HiCoexUtils.NETWORK_UNKNOWN));
            } else if ("android.intent.action.SCREEN_ON".equals(action)) {
                HwQoeQualityMonitor.this.mQoeMonitorHandler.sendEmptyMessage(5);
            } else if ("android.intent.action.SCREEN_OFF".equals(action)) {
                HwQoeQualityMonitor.this.mQoeMonitorHandler.sendEmptyMessage(6);
            } else if ("android.intent.action.PACKAGE_REMOVED".equals(action)) {
                Uri uri = intent.getData();
                if (uri == null) {
                    Log.w(HwQoeQualityMonitor.TAG, "WifiQoeBroadcastReceiver ACTION_PACKAGE_REMOVED uri is null");
                } else {
                    HwQoeQualityMonitor.this.mQoeMonitorHandler.sendMessage(HwQoeQualityMonitor.this.mQoeMonitorHandler.obtainMessage(7, uri.getSchemeSpecificPart()));
                }
            } else {
                Log.w(HwQoeQualityMonitor.TAG, "unknown action:" + action);
            }
        }
    }

    private boolean isTxRxRateQualityGood(List<HwApkTcpSecInfo> secInfoList) {
        HwApkTcpSecInfo avgInfo = getAvgInfoByTcpSecInfoList(secInfoList);
        float avgToatalTxRate = avgInfo.getPeriodTxByte();
        float avgToatalRxRate = avgInfo.getPeriodRxByte();
        Log.i(TAG, "avgToatalTxRate: " + avgToatalTxRate + ", avgToatalRxRate: " + avgToatalRxRate);
        if (avgToatalTxRate + avgToatalRxRate >= 262144.0f) {
            return true;
        }
        return false;
    }

    private boolean isWifiUpLinkQualityGood(WifiInfo info, HwApkTcpSecInfo avgInfo) {
        float periodRtt = avgInfo.getPeriodRtt();
        float otaRate = avgInfo.getOtaRate();
        float txGood = avgInfo.getPeriodTxGood();
        float txBad = avgInfo.getPeriodTxBad();
        int txRate = info.getTxLinkSpeedMbps();
        int chLoad = info.getChload();
        int bufferTime = info.getUlDelay();
        info.getSSID();
        Log.i(TAG, "WifiUpLink periodRtt = " + periodRtt + ", otaRate = " + otaRate + ", txGood = " + txGood + ", txBad = " + txBad + ", txRate = " + txRate + ", chLoad = " + chLoad + " buffer time = " + bufferTime);
        if (bufferTime >= 800 && bufferTime <= 60000) {
            return false;
        }
        if ((info.is24GHz() && txRate <= 6) || (info.is5GHz() && txRate <= 13)) {
            return false;
        }
        if (info.is24GHz() && periodRtt >= this.mApkConfig.getTcpRttTh() && chLoad >= this.mApkConfig.getChLoad2gTh() && WifiProCommonUtils.getEnterpriseCount(this.mWifiManager) > 5) {
            return false;
        }
        if (info.is5GHz() && periodRtt >= this.mApkConfig.getTcpRttTh() && chLoad >= this.mApkConfig.getChLoad5gTh() && WifiProCommonUtils.getEnterpriseCount(this.mWifiManager) > 5) {
            return false;
        }
        if (otaRate < this.mApkConfig.getOtaRateTh() || txGood < this.mApkConfig.getTxGoodTh() || txBad < this.mApkConfig.getTxBadTh() || periodRtt < this.mApkConfig.getTcpRttTh()) {
            return true;
        }
        return false;
    }

    private boolean isWifiDownLinkQualityGood(WifiInfo info, HwApkTcpSecInfo avgInfo) {
        float periodRtt = avgInfo.getPeriodRtt();
        int noise = info.getNoise();
        int chLoad = info.getChload();
        int rxRate = info.getRxLinkSpeedMbps();
        int txRate = info.getTxLinkSpeedMbps();
        Log.i(TAG, "WifiDownLink periodRtt = " + periodRtt + ", noise = " + noise + ", chLoad = " + chLoad + ", rxRate = " + rxRate + ", txRate = " + txRate);
        if ((info.is24GHz() && (rxRate <= 27 || (rxRate <= 54 && txRate <= 13))) || (info.is5GHz() && (rxRate <= 40 || (rxRate <= 54 && txRate <= 27)))) {
            return false;
        }
        if (info.is24GHz() && noise >= this.mApkConfig.getNoise2gTh() && chLoad >= this.mApkConfig.getChLoad2gTh() && periodRtt >= this.mApkConfig.getTcpRttTh() && WifiProCommonUtils.getEnterpriseCount(this.mWifiManager) > 5) {
            return false;
        }
        if (!info.is5GHz() || noise < this.mApkConfig.getNoise5gTh() || chLoad < this.mApkConfig.getChLoad5gTh() || periodRtt < this.mApkConfig.getTcpRttTh() || WifiProCommonUtils.getEnterpriseCount(this.mWifiManager) <= 5) {
            return true;
        }
        return false;
    }

    private boolean isWifiOtaQualityGood(List<HwApkTcpSecInfo> secInfoList) {
        HwApkTcpSecInfo avgInfo = getAvgInfoByTcpSecInfoList(secInfoList);
        WifiInfo info = this.mWifiManager.getConnectionInfo();
        if (info == null || info.getBSSID() == null) {
            return true;
        }
        if (isWifiUpLinkQualityGood(info, avgInfo) && isWifiDownLinkQualityGood(info, avgInfo)) {
            return true;
        }
        return false;
    }

    /* access modifiers changed from: private */
    public boolean isAllowWiFi2Cell(List<HwApkTcpSecInfo> secInfoList) {
        int rssiLevel = WifiProCommonUtils.getCurrenSignalLevel(this.mWifiManager.getConnectionInfo());
        Log.i(TAG, "rssiLevel = " + rssiLevel);
        if (WifiProCommonUtils.isHi1105Chip()) {
            if (rssiLevel >= 2) {
                return !isTxRxRateQualityGood(secInfoList) && !isWifiOtaQualityGood(secInfoList);
            }
            Log.i(TAG, "1105 and signal level <= 1, try switch.");
            return true;
        } else if (rssiLevel <= 2) {
            Log.i(TAG, "not 1105 and signal level <= 2, try switch.");
            return true;
        } else {
            Log.i(TAG, "not 1105 and signal level > 2, remain on Wifi.");
            return false;
        }
    }
}
