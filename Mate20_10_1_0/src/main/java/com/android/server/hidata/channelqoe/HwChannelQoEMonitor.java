package com.android.server.hidata.channelqoe;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.Message;
import android.os.SystemProperties;
import android.util.wifi.HwHiLog;
import com.android.server.hidata.appqoe.HwAPPQoEUserAction;
import com.android.server.hidata.arbitration.HwArbitrationCommonUtils;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class HwChannelQoEMonitor implements IChannelQoECallback {
    private static final int MESSAGE_MEASURE = 65280;
    private static final int MESSAGE_START = 65281;
    private static final int MESSAGE_STOP = 65282;
    private static final int MESSAGE_STOP_ALL = 65283;
    private static final int START_DELAY = 10000;
    private static final String TAG = "HiDATA_ChannelQoE_Monitor";
    private static final int THRESHOLD_RSSI_JUDGEMENT = 15;
    private static final int WIFI_CHLOAD_THRESHOLD = 200;
    private static final int WIFI_MONITOR_INTERVAL_X = 3000;
    private static final int WIFI_MONITOR_INTERVAL_Y = 60000;
    private static final int WIFI_MONITOR_TIMES = 2;
    private static final int WIFI_QUALITY_BAD = 2;
    private static final int WIFI_QUALITY_GOOD = 1;
    private static final int WIFI_QUALITY_UNKNOW = 3;
    private static final int WIFI_RSSI_THRESHOLD = -65;
    private boolean chipsetWithChload;
    /* access modifiers changed from: private */
    public int judge_chload;
    /* access modifiers changed from: private */
    public int judge_rssi;
    private int lastChload;
    private BroadcastReceiver mBroadcastReceiver = new ChQoeBroadcastReceiver();
    /* access modifiers changed from: private */
    public String mBssid;
    /* access modifiers changed from: private */
    public HwChannelQoEMonitor mChannelQoEMonitor;
    /* access modifiers changed from: private */
    public int mCounter;
    /* access modifiers changed from: private */
    public int mCursor;
    /* access modifiers changed from: private */
    public List<HwChannelQoEAppInfo> mMonitorCallbackList;
    private Context mMonitorContext;
    private Handler mMonitorHandler;
    /* access modifiers changed from: private */
    public int preRssi = 0;
    private int sendDelay;

    public HwChannelQoEMonitor(Context context) {
        this.mMonitorContext = context;
        this.mChannelQoEMonitor = this;
        this.mMonitorHandler = new Handler() {
            /* class com.android.server.hidata.channelqoe.HwChannelQoEMonitor.AnonymousClass1 */

            public void handleMessage(Message msg) {
                HwChannelQoEMonitor.this.logE(false, "handleMessage:%{public}s", String.valueOf(msg.what));
                switch (msg.what) {
                    case HwChannelQoEMonitor.MESSAGE_MEASURE /*{ENCODED_INT: 65280}*/:
                        String currentBssid = HwChannelQoEMonitor.this.getWifiBssid();
                        if (currentBssid == null) {
                            HwChannelQoEMonitor.this.logE(false, "wifi isn't connected, reset good_times and will Re-Measure in %{public}d milliseconds", 3000);
                            HwChannelQoEMonitor.this.resetGoodTimes();
                            String unused = HwChannelQoEMonitor.this.mBssid = null;
                            sendEmptyMessageDelayed(HwChannelQoEMonitor.MESSAGE_MEASURE, 3000);
                            return;
                        }
                        if (HwChannelQoEMonitor.this.mBssid == null) {
                            HwChannelQoEMonitor.this.logE(false, "First time or wifi disconnected.", new Object[0]);
                            String unused2 = HwChannelQoEMonitor.this.mBssid = currentBssid;
                        }
                        if (!currentBssid.equals(HwChannelQoEMonitor.this.mBssid)) {
                            HwChannelQoEMonitor.this.logE(false, "Bssid is changed. reset good times.", new Object[0]);
                            String unused3 = HwChannelQoEMonitor.this.mBssid = currentBssid;
                            HwChannelQoEMonitor.this.resetGoodTimes();
                        }
                        int quality = HwChannelQoEMonitor.this.getWifiQuality();
                        if (1 == quality) {
                            for (HwChannelQoEAppInfo appInfo : HwChannelQoEMonitor.this.mMonitorCallbackList) {
                                boolean needTputTest = false;
                                if (appInfo.goodTimes == 2 && appInfo.needTputTest) {
                                    HwArbitrationCommonUtils.logD(HwChannelQoEMonitor.TAG, false, "Detect wifi good twice, will start throughpput test if the third time is also good.", new Object[0]);
                                    needTputTest = true;
                                }
                                HwChannelQoEManager.getInstance().queryChannelQuality(appInfo.mUID, appInfo.mNetwork, appInfo.mQci, needTputTest, HwChannelQoEMonitor.this.mChannelQoEMonitor);
                            }
                            HwChannelQoEMonitor hwChannelQoEMonitor = HwChannelQoEMonitor.this;
                            int unused4 = hwChannelQoEMonitor.mCounter = hwChannelQoEMonitor.mMonitorCallbackList.size();
                            int unused5 = HwChannelQoEMonitor.this.mCursor = 0;
                            return;
                        } else if (2 == quality) {
                            HwChannelQoEMonitor.this.logE(false, "wifi quality bad. reset good times and will Re-Measure in %{public}s milliseconds", String.valueOf(3000));
                            HwChannelQoEMonitor.this.resetGoodTimes();
                            sendEmptyMessageDelayed(HwChannelQoEMonitor.MESSAGE_MEASURE, 3000);
                            return;
                        } else if (3 == quality) {
                            HwChannelQoEMonitor.this.logE(false, "wifi quality WIFI_QUALITY_UNKNOW, will Re-Measure in %{public}s milliseconds", String.valueOf(3000));
                            sendEmptyMessageDelayed(HwChannelQoEMonitor.MESSAGE_MEASURE, 3000);
                            return;
                        } else {
                            HwChannelQoEMonitor.this.logE(false, "there is no wifi network available.", new Object[0]);
                            return;
                        }
                    case HwChannelQoEMonitor.MESSAGE_START /*{ENCODED_INT: 65281}*/:
                        HwChannelQoEAppInfo info = (HwChannelQoEAppInfo) msg.obj;
                        HwChannelQoEMonitor.this.mMonitorCallbackList.add(info);
                        HwCHQciConfig config = HwCHQciManager.getInstance().getChQciConfig(info.mQci);
                        int unused6 = HwChannelQoEMonitor.this.judge_chload = config.mChload;
                        int unused7 = HwChannelQoEMonitor.this.judge_rssi = config.mRssi;
                        if (hasMessages(HwChannelQoEMonitor.MESSAGE_MEASURE)) {
                            HwChannelQoEMonitor.this.logE(false, "startMonitor already running.", new Object[0]);
                            return;
                        } else {
                            sendEmptyMessage(HwChannelQoEMonitor.MESSAGE_MEASURE);
                            return;
                        }
                    case HwChannelQoEMonitor.MESSAGE_STOP /*{ENCODED_INT: 65282}*/:
                        int UID = msg.arg1;
                        Iterator it = HwChannelQoEMonitor.this.mMonitorCallbackList.iterator();
                        while (true) {
                            if (it.hasNext()) {
                                HwChannelQoEAppInfo appInfo2 = (HwChannelQoEAppInfo) it.next();
                                if (appInfo2.mUID == UID) {
                                    HwChannelQoEMonitor.this.mMonitorCallbackList.remove(appInfo2);
                                }
                            }
                        }
                        if (HwChannelQoEMonitor.this.mMonitorCallbackList.isEmpty()) {
                            HwChannelQoEMonitor.this.stopRunning();
                            return;
                        }
                        return;
                    case HwChannelQoEMonitor.MESSAGE_STOP_ALL /*{ENCODED_INT: 65283}*/:
                        if (!HwChannelQoEMonitor.this.mMonitorCallbackList.isEmpty()) {
                            HwChannelQoEMonitor.this.mMonitorCallbackList.clear();
                        }
                        HwChannelQoEMonitor.this.stopRunning();
                        return;
                    default:
                        HwChannelQoEMonitor.this.logE(false, "unknown message.", new Object[0]);
                        return;
                }
            }
        };
        this.mMonitorCallbackList = new ArrayList();
        this.sendDelay = 60000;
        this.mBssid = null;
        this.lastChload = -1;
        this.judge_chload = 200;
        this.judge_rssi = -65;
        logE(false, "new HwChannelQoEMonitor.", new Object[0]);
        logE(false, "WIFI_CHLOAD_THRESHOLD:%{public}s", String.valueOf(200));
        logE(false, "WIFI_RSSI_THRESHOLD:%{public}s", String.valueOf(-65));
        logE(false, "START_DELAY %{public}s", String.valueOf(10000));
        logE(false, "WIFI_MONITOR_TIMES %{public}s", String.valueOf(2));
        this.chipsetWithChload = isChipHasChload();
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction("com.android.server.hidata.arbitration.HwArbitrationStateMachine");
        this.mMonitorContext.registerReceiver(this.mBroadcastReceiver, intentFilter, "com.huawei.hidata.permission.MPLINK_START_CHECK", null);
    }

    private boolean alreadyExist(int UID) {
        if (this.mMonitorCallbackList.isEmpty()) {
            return false;
        }
        for (HwChannelQoEAppInfo appInfo : this.mMonitorCallbackList) {
            if (appInfo.mUID == UID) {
                return true;
            }
        }
        return false;
    }

    public void startMonitor(HwChannelQoEAppInfo appQoeInfo, boolean needTputTest) {
        if (alreadyExist(appQoeInfo.mUID)) {
            logE(false, "startMonitor alreadyExist: %{public}s", String.valueOf(appQoeInfo.mUID));
            return;
        }
        appQoeInfo.needTputTest = needTputTest;
        Message msg = Message.obtain();
        msg.what = MESSAGE_START;
        msg.obj = appQoeInfo;
        this.mMonitorHandler.sendMessageDelayed(msg, 10000);
        logE(false, "startMonitor will start in %{public}d milliseconds.", 10000);
    }

    public void stopMonitor(int UID) {
        logE(false, "stopMonitor: %{public}s", String.valueOf(UID));
        Message msg = Message.obtain();
        msg.what = MESSAGE_STOP;
        msg.arg1 = UID;
        this.mMonitorHandler.sendMessage(msg);
    }

    public void stopAll() {
        logE(false, "stopAll", new Object[0]);
        this.mMonitorHandler.sendEmptyMessage(MESSAGE_STOP_ALL);
    }

    /* access modifiers changed from: private */
    public void stopRunning() {
        logE(false, "stopRunning", new Object[0]);
        this.lastChload = -1;
        this.judge_chload = 200;
        this.judge_rssi = -65;
        this.mMonitorHandler.removeMessages(MESSAGE_MEASURE);
        this.mMonitorHandler.removeMessages(MESSAGE_START);
    }

    /* access modifiers changed from: private */
    public String getWifiBssid() {
        WifiManager mWManager = (WifiManager) this.mMonitorContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE);
        if (mWManager == null) {
            logE(false, "can't find wifi manager.", new Object[0]);
            return null;
        }
        WifiInfo info = mWManager.getConnectionInfo();
        if (info != null) {
            return info.getBSSID();
        }
        logE(false, "there is no wifi connected.", new Object[0]);
        return null;
    }

    /* access modifiers changed from: private */
    public int getWifiQuality() {
        if (!this.chipsetWithChload) {
            return judgeWithoutChload();
        }
        return judgeWithChload();
    }

    private int judgeWithoutChload() {
        int i;
        WifiInfo wifiInfo = getWifiInfo();
        if (wifiInfo == null) {
            logE(false, "judgeWithoutChload get null Wifi Info.", new Object[0]);
            return -1;
        }
        int currentRssi = wifiInfo.getRssi();
        logE(false, "current rssi %{public}d", Integer.valueOf(currentRssi));
        int i2 = this.preRssi;
        int i3 = this.judge_rssi;
        if ((i2 >= i3 || currentRssi < i3) && ((i = this.preRssi) < this.judge_rssi || currentRssi - i < 15)) {
            logE(false, "judgeWithoutChload return bad.", new Object[0]);
            return 2;
        }
        logE(false, "judgeWithoutChload return good.", new Object[0]);
        return 1;
    }

    private int judgeWithChload() {
        WifiInfo info = getWifiInfo();
        if (info == null) {
            logE(false, "judgeWithChload: there is no wifi connected.", new Object[0]);
            return -1;
        }
        int rssi = info.getRssi();
        int chload = info.getChload();
        logE(false, "judgeWithChload. RSSI is %{public}s", String.valueOf(rssi));
        logE(false, "judgeWithChload. CHLoad is %{public}s", String.valueOf(chload));
        if (-1 == chload) {
            int i = this.lastChload;
            if (i == -1) {
                logE(false, "last time the chload is also -1.", new Object[0]);
                return 3;
            }
            logE(false, "last time the chload is not -1. last is %{public}d", Integer.valueOf(i));
            chload = this.lastChload;
        } else {
            this.lastChload = chload;
        }
        if (rssi < this.judge_rssi || chload > this.judge_chload) {
            return 2;
        }
        return 1;
    }

    /* access modifiers changed from: private */
    public WifiInfo getWifiInfo() {
        WifiManager mWManager = (WifiManager) this.mMonitorContext.getSystemService(DevSchedFeatureRT.WIFI_FEATURE);
        if (mWManager == null) {
            logE(false, "getWifiInfo: can't find wifi manager.", new Object[0]);
            return null;
        }
        WifiInfo info = mWManager.getConnectionInfo();
        if (info != null) {
            return info;
        }
        logE(false, "getWifiInfo: there is no wifi connected.", new Object[0]);
        return null;
    }

    /* access modifiers changed from: private */
    public void logE(boolean isFmtStrPrivate, String info, Object... args) {
        HwHiLog.e(TAG, isFmtStrPrivate, info, args);
    }

    @Override // com.android.server.hidata.channelqoe.IChannelQoECallback
    public void onChannelQuality(int UID, int sense, int network, int label) {
        this.mCursor++;
        logE(false, "onChannelQuality enter. %{public}s", String.valueOf(this.mCursor));
        if (this.mMonitorCallbackList.isEmpty()) {
            logE(false, "onChannelQuality callback list is empty, maybe monitor has been stopped.", new Object[0]);
            return;
        }
        for (HwChannelQoEAppInfo appInfo : this.mMonitorCallbackList) {
            if (appInfo.mUID == UID) {
                if (label == 0) {
                    appInfo.goodTimes++;
                    logE(false, "%{public}s good times is %{public}s", String.valueOf(appInfo.mUID), String.valueOf(appInfo.goodTimes));
                    if (appInfo.goodTimes > 2) {
                        logE(false, "3 times for good measure. will stop monitor for UID:%{public}d", Integer.valueOf(UID));
                        appInfo.callback.onWifiLinkQuality(appInfo.mUID, appInfo.mScence, 0);
                        stopMonitor(UID);
                    }
                    logE(false, "set delay time to 3 sec.", new Object[0]);
                    this.sendDelay = 3000;
                } else {
                    logE(false, "wifi rtt result is bad, anyway set good times to 0.", new Object[0]);
                    appInfo.goodTimes = 0;
                    this.sendDelay = 60000;
                }
            }
        }
        if (this.mCounter == this.mCursor) {
            logE(false, "all done, will Re-Measure in %{public}d milliseconds", Integer.valueOf(this.sendDelay));
            this.mMonitorHandler.sendEmptyMessageDelayed(MESSAGE_MEASURE, (long) this.sendDelay);
            this.sendDelay = 60000;
        }
    }

    @Override // com.android.server.hidata.channelqoe.IChannelQoECallback
    public void onWifiLinkQuality(int UID, int sense, int label) {
    }

    @Override // com.android.server.hidata.channelqoe.IChannelQoECallback
    public void onCellPSAvailable(boolean isOK, int reason) {
    }

    @Override // com.android.server.hidata.channelqoe.IChannelQoECallback
    public void onCurrentRtt(int rtt) {
    }

    /* access modifiers changed from: private */
    public void resetGoodTimes() {
        for (HwChannelQoEAppInfo info : this.mMonitorCallbackList) {
            info.goodTimes = 0;
        }
    }

    private boolean isChipHasChload() {
        String[] chipCodes = {HwAPPQoEUserAction.HISI_CHIP_1102A, HwAPPQoEUserAction.HISI_CHIP_1103, HwAPPQoEUserAction.HISI_CHIP_1105};
        String chipset = SystemProperties.get(HwAPPQoEUserAction.CHIPSET_TYPE_PROP, HwAPPQoEUserAction.DEFAULT_CHIP_TYPE);
        logE(false, "isChipHasChload, chipset = %{public}s", chipset);
        for (String code : chipCodes) {
            if (chipset.contains(code)) {
                return true;
            }
        }
        return false;
    }

    private class ChQoeBroadcastReceiver extends BroadcastReceiver {
        private ChQoeBroadcastReceiver() {
        }

        public void onReceive(Context arg0, Intent intent) {
            if (intent == null) {
                HwChannelQoEMonitor.this.logE(false, "received intent is null, return", new Object[0]);
                return;
            }
            String action = intent.getAction();
            HwChannelQoEMonitor.this.logE(false, "ChQoeBroadcastReceiver receive broadcast: %{public}s", action);
            if (!"com.android.server.hidata.arbitration.HwArbitrationStateMachine".equals(action)) {
                HwChannelQoEMonitor.this.logE(false, "ChQoeBroadcastReceiver receive broadcast: %{public}s", "com.android.server.hidata.arbitration.HwArbitrationStateMachine");
            } else if (801 != intent.getIntExtra("MPLinkSuccessNetworkKey", 802)) {
                HwChannelQoEMonitor.this.logE(false, "network type is not CELL.", new Object[0]);
            } else {
                WifiInfo info = HwChannelQoEMonitor.this.getWifiInfo();
                if (info == null || -127 == info.getRssi()) {
                    int unused = HwChannelQoEMonitor.this.preRssi = -1;
                    HwChannelQoEMonitor.this.logE(false, "there is no wifi or invalid value, set preRssi -1.", new Object[0]);
                    return;
                }
                int unused2 = HwChannelQoEMonitor.this.preRssi = info.getRssi();
                HwChannelQoEMonitor hwChannelQoEMonitor = HwChannelQoEMonitor.this;
                hwChannelQoEMonitor.logE(false, "set preRssi %{public}d", Integer.valueOf(hwChannelQoEMonitor.preRssi));
            }
        }
    }
}
