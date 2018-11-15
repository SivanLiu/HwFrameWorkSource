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
import android.util.Log;
import com.android.server.hidata.appqoe.HwAPPQoEUserAction;
import com.android.server.hidata.arbitration.HwArbitrationDEFS;
import com.android.server.rms.iaware.memory.utils.MemoryConstant;
import java.util.ArrayList;
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
    private int judge_chload;
    private int judge_rssi;
    private int lastChload;
    private BroadcastReceiver mBroadcastReceiver = new ChQoeBroadcastReceiver(this, null);
    private String mBssid;
    private HwChannelQoEMonitor mChannelQoEMonitor;
    private int mCounter;
    private int mCursor;
    private List<HwChannelQoEAppInfo> mMonitorCallbackList;
    private Context mMonitorContext;
    private Handler mMonitorHandler;
    private int preRssi = 0;
    private int sendDelay;

    private class ChQoeBroadcastReceiver extends BroadcastReceiver {
        private ChQoeBroadcastReceiver() {
        }

        /* synthetic */ ChQoeBroadcastReceiver(HwChannelQoEMonitor x0, AnonymousClass1 x1) {
            this();
        }

        public void onReceive(Context arg0, Intent intent) {
            String action = intent.getAction();
            HwChannelQoEMonitor hwChannelQoEMonitor = HwChannelQoEMonitor.this;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("ChQoeBroadcastReceiver receive broadcast: ");
            stringBuilder.append(action);
            hwChannelQoEMonitor.logE(stringBuilder.toString());
            if (!"com.android.server.hidata.arbitration.HwArbitrationStateMachine".equals(action)) {
                HwChannelQoEMonitor.this.logE("ChQoeBroadcastReceiver receive broadcast: com.android.server.hidata.arbitration.HwArbitrationStateMachine");
            } else if (801 != intent.getIntExtra("MPLinkSuccessNetworkKey", 802)) {
                HwChannelQoEMonitor.this.logE("network type is not CELL.");
            } else {
                WifiInfo info = HwChannelQoEMonitor.this.getWifiInfo();
                if (info == null || -127 == info.getRssi()) {
                    HwChannelQoEMonitor.this.preRssi = -1;
                    HwChannelQoEMonitor.this.logE("there is no wifi or invalid value, set preRssi -1.");
                } else {
                    HwChannelQoEMonitor.this.preRssi = info.getRssi();
                    HwChannelQoEMonitor hwChannelQoEMonitor2 = HwChannelQoEMonitor.this;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append("set preRssi ");
                    stringBuilder2.append(HwChannelQoEMonitor.this.preRssi);
                    hwChannelQoEMonitor2.logE(stringBuilder2.toString());
                }
            }
        }
    }

    public HwChannelQoEMonitor(Context context) {
        this.mMonitorContext = context;
        this.mChannelQoEMonitor = this;
        this.mMonitorHandler = new Handler() {
            /* JADX WARNING: Removed duplicated region for block: B:16:0x007d  */
            /* Code decompiled incorrectly, please refer to instructions dump. */
            public void handleMessage(Message msg) {
                HwChannelQoEMonitor hwChannelQoEMonitor = HwChannelQoEMonitor.this;
                StringBuilder stringBuilder = new StringBuilder();
                stringBuilder.append("handleMessage:");
                stringBuilder.append(String.valueOf(msg.what));
                hwChannelQoEMonitor.logE(stringBuilder.toString());
                switch (msg.what) {
                    case HwChannelQoEMonitor.MESSAGE_MEASURE /*65280*/:
                        String currentBssid = HwChannelQoEMonitor.this.getWifiBssid();
                        if (currentBssid == null) {
                            HwChannelQoEMonitor.this.logE("wifi isn't connected, reset good_times and will Re-Measure in 3000 milliseconds");
                            HwChannelQoEMonitor.this.resetGoodTimes();
                            HwChannelQoEMonitor.this.mBssid = null;
                            sendEmptyMessageDelayed(HwChannelQoEMonitor.MESSAGE_MEASURE, HwArbitrationDEFS.NotificationMonitorPeriodMillis);
                            return;
                        }
                        if (HwChannelQoEMonitor.this.mBssid == null) {
                            HwChannelQoEMonitor.this.logE("First time or wifi disconnected.");
                            HwChannelQoEMonitor.this.mBssid = currentBssid;
                        }
                        if (!currentBssid.equals(HwChannelQoEMonitor.this.mBssid)) {
                            HwChannelQoEMonitor.this.logE("Bssid is changed. reset good times.");
                            HwChannelQoEMonitor.this.mBssid = currentBssid;
                            HwChannelQoEMonitor.this.resetGoodTimes();
                        }
                        int quality = HwChannelQoEMonitor.this.getWifiQuality();
                        HwChannelQoEMonitor hwChannelQoEMonitor2;
                        StringBuilder stringBuilder2;
                        if (1 == quality) {
                            for (HwChannelQoEAppInfo appInfo : HwChannelQoEMonitor.this.mMonitorCallbackList) {
                                HwChannelQoEManager.getInstance().queryChannelQuality(appInfo.mUID, appInfo.mScence, appInfo.mNetwork, appInfo.mQci, HwChannelQoEMonitor.this.mChannelQoEMonitor);
                            }
                            HwChannelQoEMonitor.this.mCounter = HwChannelQoEMonitor.this.mMonitorCallbackList.size();
                            HwChannelQoEMonitor.this.mCursor = 0;
                            break;
                        } else if (2 == quality) {
                            hwChannelQoEMonitor2 = HwChannelQoEMonitor.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("wifi quality bad. reset good times and will Re-Measure in ");
                            stringBuilder2.append(String.valueOf(3000));
                            stringBuilder2.append(" milliseconds");
                            hwChannelQoEMonitor2.logE(stringBuilder2.toString());
                            HwChannelQoEMonitor.this.resetGoodTimes();
                            sendEmptyMessageDelayed(HwChannelQoEMonitor.MESSAGE_MEASURE, HwArbitrationDEFS.NotificationMonitorPeriodMillis);
                            break;
                        } else if (3 == quality) {
                            hwChannelQoEMonitor2 = HwChannelQoEMonitor.this;
                            stringBuilder2 = new StringBuilder();
                            stringBuilder2.append("wifi quality WIFI_QUALITY_UNKNOW, will Re-Measure in ");
                            stringBuilder2.append(String.valueOf(3000));
                            stringBuilder2.append(" milliseconds");
                            hwChannelQoEMonitor2.logE(stringBuilder2.toString());
                            sendEmptyMessageDelayed(HwChannelQoEMonitor.MESSAGE_MEASURE, HwArbitrationDEFS.NotificationMonitorPeriodMillis);
                            break;
                        } else {
                            HwChannelQoEMonitor.this.logE("there is no wifi network available.");
                            return;
                        }
                    case HwChannelQoEMonitor.MESSAGE_START /*65281*/:
                        HwChannelQoEAppInfo info = msg.obj;
                        HwChannelQoEMonitor.this.mMonitorCallbackList.add(info);
                        HwCHQciConfig config = HwCHQciManager.getInstance().getChQciConfig(info.mQci);
                        HwChannelQoEMonitor.this.judge_chload = config.mChload;
                        HwChannelQoEMonitor.this.judge_rssi = config.mRssi;
                        if (!hasMessages(HwChannelQoEMonitor.MESSAGE_MEASURE)) {
                            sendEmptyMessage(HwChannelQoEMonitor.MESSAGE_MEASURE);
                            break;
                        } else {
                            HwChannelQoEMonitor.this.logE("startMonitor already running.");
                            break;
                        }
                    case HwChannelQoEMonitor.MESSAGE_STOP /*65282*/:
                        int UID = msg.arg1;
                        for (HwChannelQoEAppInfo appInfo2 : HwChannelQoEMonitor.this.mMonitorCallbackList) {
                            if (appInfo2.mUID == UID) {
                                HwChannelQoEMonitor.this.mMonitorCallbackList.remove(appInfo2);
                                if (HwChannelQoEMonitor.this.mMonitorCallbackList.isEmpty()) {
                                    HwChannelQoEMonitor.this.stopRunning();
                                    break;
                                }
                            }
                        }
                        if (HwChannelQoEMonitor.this.mMonitorCallbackList.isEmpty()) {
                        }
                        break;
                    case HwChannelQoEMonitor.MESSAGE_STOP_ALL /*65283*/:
                        if (!HwChannelQoEMonitor.this.mMonitorCallbackList.isEmpty()) {
                            HwChannelQoEMonitor.this.mMonitorCallbackList.clear();
                        }
                        HwChannelQoEMonitor.this.stopRunning();
                        break;
                    default:
                        HwChannelQoEMonitor.this.logE("unknown message.");
                        break;
                }
            }
        };
        this.mMonitorCallbackList = new ArrayList();
        this.sendDelay = 60000;
        this.mBssid = null;
        this.lastChload = -1;
        this.judge_chload = 200;
        this.judge_rssi = -65;
        logE("new HwChannelQoEMonitor.");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("WIFI_CHLOAD_THRESHOLD:");
        stringBuilder.append(String.valueOf(200));
        logE(stringBuilder.toString());
        StringBuilder stringBuilder2 = new StringBuilder();
        stringBuilder2.append("WIFI_RSSI_THRESHOLD:");
        stringBuilder2.append(String.valueOf(-65));
        logE(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("START_DELAY");
        stringBuilder2.append(String.valueOf(10000));
        logE(stringBuilder2.toString());
        stringBuilder2 = new StringBuilder();
        stringBuilder2.append("WIFI_MONITOR_TIMES");
        stringBuilder2.append(String.valueOf(2));
        logE(stringBuilder2.toString());
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

    public void startMonitor(HwChannelQoEAppInfo appQoeInfo) {
        if (alreadyExist(appQoeInfo.mUID)) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("startMonitor alreadyExist: ");
            stringBuilder.append(String.valueOf(appQoeInfo.mUID));
            logE(stringBuilder.toString());
            return;
        }
        Message msg = Message.obtain();
        msg.what = MESSAGE_START;
        msg.obj = appQoeInfo;
        this.mMonitorHandler.sendMessageDelayed(msg, MemoryConstant.MIN_INTERVAL_OP_TIMEOUT);
        logE("startMonitor will start in 10000 milliseconds.");
    }

    public void stopMonitor(int UID) {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("stopMonitor: ");
        stringBuilder.append(String.valueOf(UID));
        logE(stringBuilder.toString());
        Message msg = Message.obtain();
        msg.what = MESSAGE_STOP;
        msg.arg1 = UID;
        this.mMonitorHandler.sendMessage(msg);
    }

    public void stopAll() {
        logE("stopAll");
        this.mMonitorHandler.sendEmptyMessage(MESSAGE_STOP_ALL);
    }

    private void stopRunning() {
        logE("stopRunning");
        this.lastChload = -1;
        this.judge_chload = 200;
        this.judge_rssi = -65;
        this.mMonitorHandler.removeMessages(MESSAGE_MEASURE);
        this.mMonitorHandler.removeMessages(MESSAGE_START);
    }

    private String getWifiBssid() {
        WifiManager mWManager = (WifiManager) this.mMonitorContext.getSystemService("wifi");
        if (mWManager == null) {
            logE("can't find wifi manager.");
            return null;
        }
        WifiInfo info = mWManager.getConnectionInfo();
        if (info != null) {
            return info.getBSSID();
        }
        logE("there is no wifi connected.");
        return null;
    }

    private int getWifiQuality() {
        if (this.chipsetWithChload) {
            return judgeWithChload();
        }
        return judgeWithoutChload();
    }

    private int judgeWithoutChload() {
        WifiInfo wifiInfo = getWifiInfo();
        if (wifiInfo == null) {
            logE("judgeWithoutChload get null Wifi Info.");
            return -1;
        }
        int currentRssi = wifiInfo.getRssi();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("current rssi ");
        stringBuilder.append(currentRssi);
        logE(stringBuilder.toString());
        if ((this.preRssi >= this.judge_rssi || currentRssi < this.judge_rssi) && (this.preRssi < this.judge_rssi || currentRssi - this.preRssi < 15)) {
            logE("judgeWithoutChload return bad.");
            return 2;
        }
        logE("judgeWithoutChload return good.");
        return 1;
    }

    private int judgeWithChload() {
        WifiInfo info = getWifiInfo();
        if (info == null) {
            logE("judgeWithChload: there is no wifi connected.");
            return -1;
        }
        int rssi = info.getRssi();
        int chload = info.getChload();
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("judgeWithChload. RSSI is ");
        stringBuilder.append(String.valueOf(rssi));
        logE(stringBuilder.toString());
        stringBuilder = new StringBuilder();
        stringBuilder.append("judgeWithChload. CHLoad is ");
        stringBuilder.append(String.valueOf(chload));
        logE(stringBuilder.toString());
        if (-1 != chload) {
            this.lastChload = chload;
        } else if (this.lastChload == -1) {
            logE("last time the chload is also -1.");
            return 3;
        } else {
            StringBuilder stringBuilder2 = new StringBuilder();
            stringBuilder2.append("last time the chload is not -1. last is ");
            stringBuilder2.append(this.lastChload);
            logE(stringBuilder2.toString());
            chload = this.lastChload;
        }
        if (rssi < this.judge_rssi || chload > this.judge_chload) {
            return 2;
        }
        return 1;
    }

    private WifiInfo getWifiInfo() {
        WifiManager mWManager = (WifiManager) this.mMonitorContext.getSystemService("wifi");
        if (mWManager == null) {
            logE("getWifiInfo: can't find wifi manager.");
            return null;
        }
        WifiInfo info = mWManager.getConnectionInfo();
        if (info != null) {
            return info;
        }
        logE("getWifiInfo: there is no wifi connected.");
        return null;
    }

    private void logE(String info) {
        Log.e(TAG, info);
    }

    public void onChannelQuality(int UID, int sense, int network, int label) {
        this.mCursor++;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("onChannelQuality enter. ");
        stringBuilder.append(String.valueOf(this.mCursor));
        logE(stringBuilder.toString());
        for (HwChannelQoEAppInfo appInfo : this.mMonitorCallbackList) {
            if (appInfo.mUID == UID) {
                if (label == 0) {
                    appInfo.good_times++;
                    StringBuilder stringBuilder2 = new StringBuilder();
                    stringBuilder2.append(String.valueOf(appInfo.mUID));
                    stringBuilder2.append(" good times is ");
                    stringBuilder2.append(String.valueOf(appInfo.good_times));
                    logE(stringBuilder2.toString());
                    if (appInfo.good_times > 2) {
                        stringBuilder2 = new StringBuilder();
                        stringBuilder2.append("3 times for good measure. will stop monitor for UID:");
                        stringBuilder2.append(UID);
                        logE(stringBuilder2.toString());
                        appInfo.callback.onWifiLinkQuality(appInfo.mUID, appInfo.mScence, 0);
                        stopMonitor(UID);
                    }
                    logE("set delay time to 3 sec.");
                    this.sendDelay = 3000;
                } else {
                    logE("wifi rtt result is bad, anyway set good times to 0.");
                    appInfo.good_times = 0;
                    this.sendDelay = 60000;
                }
            }
        }
        if (this.mCounter == this.mCursor) {
            stringBuilder = new StringBuilder();
            stringBuilder.append("all done, will Re-Measure in ");
            stringBuilder.append(this.sendDelay);
            stringBuilder.append(" milliseconds");
            logE(stringBuilder.toString());
            this.mMonitorHandler.sendEmptyMessageDelayed(MESSAGE_MEASURE, (long) this.sendDelay);
            this.sendDelay = 60000;
        }
    }

    public void onWifiLinkQuality(int UID, int sense, int label) {
    }

    public void onCellPSAvailable(boolean isOK, int reason) {
    }

    public void onCurrentRtt(int rtt) {
    }

    private void resetGoodTimes() {
        for (HwChannelQoEAppInfo info : this.mMonitorCallbackList) {
            info.good_times = 0;
        }
    }

    private boolean isChipHasChload() {
        String chipset = SystemProperties.get(HwAPPQoEUserAction.CHIPSET_TYPE_PROP, "none");
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isChipHasChload, chipset = ");
        stringBuilder.append(chipset);
        logE(stringBuilder.toString());
        if (chipset == null || !chipset.contains("1103")) {
            return false;
        }
        return true;
    }
}
