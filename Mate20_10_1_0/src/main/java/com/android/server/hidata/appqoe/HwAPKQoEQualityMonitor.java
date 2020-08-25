package com.android.server.hidata.appqoe;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.TrafficStats;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.PowerManager;
import com.android.server.hidata.HwHidataJniAdapter;
import com.android.server.hidata.appqoe.ai.HwApkQoeAiCalc;
import com.android.server.hidata.hiradio.HwWifiBoost;
import com.android.server.rms.iaware.feature.DevSchedFeatureRT;
import java.util.ArrayList;
import java.util.List;

public class HwAPKQoEQualityMonitor {
    private static final int AFTER_GOOD_THRESHOLD = 525;
    private static final int BAD_AVE_RTT = 800;
    private static final int CMD_QUERY_HIDATA_INFO = 23;
    private static final int CMD_QUERY_PKTS = 15;
    private static final int HIDATA_TCP_INFO_MIN_LENGTH = 10;
    private static final String IFACE = "wlan0";
    private static final int INIT_THRESHOLD = 510;
    private static final float LESS_PKTS_BAD_RATE = 0.3f;
    private static final float LESS_PKTS_VERY_BAD_RATE = 0.4f;
    private static final int MIN_PERIOD_TIME = 1000;
    private static final int MIN_RX_PKTS = 100;
    private static final int MIN_TX_PKTS = 3;
    private static final float MORE_PKTS_BAD_RATE = 0.2f;
    private static final float MORE_PKTS_VERY_BAD_RATE = 0.3f;
    private static final int MORE_TX_PKTS = 20;
    private static final int MSG_GET_TCP_INFO = 2;
    private static final int MSG_INIT_TCP_INFO = 1;
    private static final int QUALIT_AI_COMMON_USER_NO_RX_TIMES = 6;
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
    private static final int QUALIT_AI_RADICAL_USER_NO_RX_TIMES = 3;
    private static final int QUALIT_AI_RSSI_LEVEL_2 = 2;
    private static final int QUALIT_AI_RSSI_LEVEL_4 = 4;
    private static final int STRONG_RSSI = 2;
    private static final String TAG = "HiData_HwAPKQoEQualityMonitor";
    private static final float TX_GOOD_RATE = 0.2f;
    private static final int VERY_BAD_AVE_RTT = 1500;
    public boolean isMonitoring = false;
    private HwAPPQoEAPKConfig mAPKConfig;
    private HwAPPChrExcpInfo mAppQoeChrInfo = new HwAPPChrExcpInfo();
    private Context mContext = null;
    /* access modifiers changed from: private */
    public HwAPKTcpInfo mCurrentHwAPKTcpInfo;
    private HwAPPStateInfo mCurrentInfo;
    /* access modifiers changed from: private */
    public int mCurrentUID = 0;
    private Handler mHandler;
    private HwAPPQoEResourceManger mHwAPPQoEResourceManger;
    private HwApkQoeAiCalc mHwApkQoeAiCalc;
    /* access modifiers changed from: private */
    public HwHidataJniAdapter mHwHidataJniAdapter;
    private HwWifiBoost mHwWifiBoost;
    private int mIsNoRxTime = 0;
    /* access modifiers changed from: private */
    public HwAPKTcpInfo mLastHwAPKTcpInfo;
    private HwAPPQoEUserLearning mLearningManager = null;
    private Handler mLocalHandler;
    private final Object mLock = new Object();
    /* access modifiers changed from: private */
    public int mPeriodTime = 0;
    private int mScenceId = 0;
    private int mSystemTotalRxpackte = 0;
    private WifiManager mWifiManager;

    public HwAPKQoEQualityMonitor(Handler handler, Context context) {
        this.mHandler = handler;
        this.mHwAPPQoEResourceManger = HwAPPQoEResourceManger.getInstance();
        this.mHwHidataJniAdapter = HwHidataJniAdapter.getInstance();
        this.mHwWifiBoost = HwWifiBoost.getInstance(context);
        this.mWifiManager = (WifiManager) context.getSystemService(DevSchedFeatureRT.WIFI_FEATURE);
        this.mLearningManager = HwAPPQoEUserLearning.getInstance();
        this.mHwApkQoeAiCalc = new HwApkQoeAiCalc();
        this.mContext = context;
    }

    public void startMonitor(HwAPPStateInfo info) {
        int uid = info.mAppUID;
        int scenceId = info.mScenceId;
        int appId = info.mAppId;
        if (this.isMonitoring) {
            HwAPPQoEUtils.logD(TAG, false, " uid = %{public}d mCurrentUID = %{public}d scenceId = %{public}d mScenceId = %{public}d", Integer.valueOf(uid), Integer.valueOf(this.mCurrentUID), Integer.valueOf(scenceId), Integer.valueOf(this.mScenceId));
            if (uid == this.mCurrentUID && scenceId != this.mScenceId) {
                this.mLocalHandler.removeMessages(1);
                this.mLocalHandler.removeMessages(2);
            } else {
                return;
            }
        } else {
            HwAPPQoEUtils.logD(TAG, false, "uid = %{public}d appId = %{public}d scenceId = %{public}d", Integer.valueOf(uid), Integer.valueOf(appId), Integer.valueOf(scenceId));
            initHandlerThread();
        }
        this.mCurrentInfo = info;
        this.mAPKConfig = this.mHwAPPQoEResourceManger.getAPKScenceConfig(scenceId);
        if (this.mAPKConfig == null) {
            HwAPPQoEUtils.logD(TAG, false, "mAPKConfig == null", new Object[0]);
            stopMonitor();
            return;
        }
        this.isMonitoring = true;
        this.mScenceId = scenceId;
        this.mCurrentUID = uid;
        HwAPPQoEUserLearning hwAPPQoEUserLearning = this.mLearningManager;
        if (hwAPPQoEUserLearning != null) {
            if (hwAPPQoEUserLearning.getUserTypeByAppId(info.mAppId) == 1) {
                HwAPPQoEUtils.logD(TAG, false, " HwAPKQoEQualityMonitor is a COMMON user", new Object[0]);
                this.mPeriodTime = this.mAPKConfig.mAppPeriod * 1000 * 2;
            } else {
                this.mPeriodTime = this.mAPKConfig.mAppPeriod * 1000;
                HwAPPQoEUtils.logD(TAG, false, " HwAPKQoEQualityMonitor is a USER_TYPE_RADICAL user", new Object[0]);
            }
        }
        this.mLocalHandler.sendEmptyMessage(1);
    }

    public void stopMonitor() {
        HwAPPQoEUtils.logD(TAG, false, " HwAPKQoEQualityMonitor stop monitor isMonitoring = %{public}s", String.valueOf(this.isMonitoring));
        if (this.isMonitoring) {
            this.isMonitoring = false;
            this.mIsNoRxTime = 0;
            this.mLocalHandler.removeMessages(1);
            this.mLocalHandler.removeMessages(2);
            release();
        }
    }

    private void initHandlerThread() {
        HandlerThread handlerThread = new HandlerThread("HwAPKQoEQualityMonitor Thread");
        handlerThread.start();
        this.mLocalHandler = new Handler(handlerThread.getLooper()) {
            /* class com.android.server.hidata.appqoe.HwAPKQoEQualityMonitor.AnonymousClass1 */
            int badNumTimes = 0;
            int noDataTimes = 0;
            List<HwApkTcpSecInfo> periodInfoList = new ArrayList();
            int threshold = HwAPKQoEQualityMonitor.INIT_THRESHOLD;

            public void handleMessage(Message msg) {
                int i = msg.what;
                if (i == 1) {
                    this.threshold = HwAPKQoEQualityMonitor.INIT_THRESHOLD;
                    int[] result = HwAPKQoEQualityMonitor.this.mHwHidataJniAdapter.sendQoECmd(23, HwAPKQoEQualityMonitor.this.mCurrentUID);
                    RssiPacketCountInfo otaInfo = HwAPKQoEQualityMonitor.this.getOTAInfo();
                    if (result == null || result.length < 10) {
                        HwAPPQoEUtils.logE(HwAPKQoEQualityMonitor.TAG, false, "MSG_INIT_TCP_INFO error", new Object[0]);
                        sendEmptyMessageDelayed(1, 1000);
                    } else {
                        HwAPKTcpInfo unused = HwAPKQoEQualityMonitor.this.mLastHwAPKTcpInfo = new HwAPKTcpInfo(result, otaInfo);
                        sendEmptyMessageDelayed(2, 1000);
                        this.badNumTimes = 0;
                        this.periodInfoList.clear();
                    }
                } else if (i == 2) {
                    if (!HwAPKQoEQualityMonitor.this.isScreenOn()) {
                        sendEmptyMessageDelayed(2, 1000);
                    } else {
                        int periodTime = HwAPKQoEQualityMonitor.this.mPeriodTime / 1000;
                        int[] result2 = HwAPKQoEQualityMonitor.this.mHwHidataJniAdapter.sendQoECmd(23, HwAPKQoEQualityMonitor.this.mCurrentUID);
                        RssiPacketCountInfo otaInfo2 = HwAPKQoEQualityMonitor.this.getOTAInfo();
                        if (result2 == null || result2.length < 10) {
                            HwAPPQoEUtils.logE(HwAPKQoEQualityMonitor.TAG, false, "get messsage error", new Object[0]);
                        } else {
                            HwAPKTcpInfo unused2 = HwAPKQoEQualityMonitor.this.mCurrentHwAPKTcpInfo = new HwAPKTcpInfo(result2, otaInfo2);
                            HwAPKQoEQualityMonitor hwAPKQoEQualityMonitor = HwAPKQoEQualityMonitor.this;
                            HwApkTcpSecInfo secInfo = new HwApkTcpSecInfo(hwAPKQoEQualityMonitor.mLastHwAPKTcpInfo, HwAPKQoEQualityMonitor.this.mCurrentHwAPKTcpInfo);
                            if (secInfo.getPeriodTcpTxPacket() == 0.0f && secInfo.getPeriodTcpRxPacket() == 0.0f) {
                                HwAPPQoEUtils.logD(HwAPKQoEQualityMonitor.TAG, false, "data is zero mNoDataTimes = " + this.noDataTimes, new Object[0]);
                                this.noDataTimes = this.noDataTimes + 1;
                                if (this.noDataTimes >= periodTime) {
                                    this.periodInfoList.clear();
                                }
                                sendEmptyMessageDelayed(2, 1000);
                            } else {
                                this.noDataTimes = 0;
                                this.periodInfoList = HwAPKQoEQualityMonitor.this.updatePeriodInfoList(this.periodInfoList, secInfo);
                                int quality = HwAPKQoEQualityMonitor.this.getApkQualityOfCurrentPeriod(this.periodInfoList, this.threshold);
                                if (quality == 107) {
                                    this.badNumTimes++;
                                    if (this.badNumTimes >= periodTime) {
                                        HwAPKQoEQualityMonitor.this.sendMessageToSTM(107);
                                        this.badNumTimes = 0;
                                    }
                                } else if (quality == 106) {
                                    this.threshold = HwAPKQoEQualityMonitor.AFTER_GOOD_THRESHOLD;
                                    this.badNumTimes = 0;
                                } else if (quality == 110) {
                                    HwAPKQoEQualityMonitor.this.sendMessageToSTM(107);
                                } else {
                                    HwAPPQoEUtils.logD(HwAPKQoEQualityMonitor.TAG, false, "result not care", new Object[0]);
                                }
                                HwAPKQoEQualityMonitor hwAPKQoEQualityMonitor2 = HwAPKQoEQualityMonitor.this;
                                HwAPKTcpInfo unused3 = hwAPKQoEQualityMonitor2.mLastHwAPKTcpInfo = hwAPKQoEQualityMonitor2.mCurrentHwAPKTcpInfo;
                            }
                        }
                        sendEmptyMessageDelayed(2, 1000);
                    }
                }
                super.handleMessage(msg);
            }
        };
    }

    /* access modifiers changed from: private */
    public boolean isScreenOn() {
        PowerManager powerManager;
        Context context = this.mContext;
        if (context == null || (powerManager = (PowerManager) context.getSystemService("power")) == null || !powerManager.isScreenOn()) {
            return false;
        }
        return true;
    }

    /* access modifiers changed from: private */
    public List<HwApkTcpSecInfo> updatePeriodInfoList(List<HwApkTcpSecInfo> periodInfoList, HwApkTcpSecInfo secInfo) {
        if (periodInfoList.size() >= this.mPeriodTime / 1000 && periodInfoList.size() > 0) {
            periodInfoList.remove(0);
        }
        if (secInfo.getPeriodRttPacket() == 0.0f && periodInfoList.size() > 0) {
            HwApkTcpSecInfo lastInfo = periodInfoList.get(periodInfoList.size() - 1);
            secInfo.setPeriodRttPacket(lastInfo.getPeriodRttPacket());
            secInfo.setPeriodRtt(lastInfo.getPeriodRtt());
            HwAPPQoEUtils.logD(TAG, false, "RttPacket is 0 ,update secInfo with last", new Object[0]);
        }
        periodInfoList.add(secInfo);
        return periodInfoList;
    }

    private void release() {
        Looper looper;
        Handler handler = this.mLocalHandler;
        if (handler != null && (looper = handler.getLooper()) != null && looper != Looper.getMainLooper()) {
            looper.quitSafely();
        }
    }

    /* access modifiers changed from: private */
    public int getApkQualityOfCurrentPeriod(List<HwApkTcpSecInfo> secInfoList, int threshold) {
        int result;
        WifiInfo info = this.mWifiManager.getConnectionInfo();
        if (info != null) {
            int rssiLevel = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(info.getFrequency(), info.getRssi());
            HwAPPQoEUtils.logD(TAG, false, "rssi:%{public}d, level:%{public}d", Integer.valueOf(info.getRssi()), Integer.valueOf(rssiLevel));
            HwApkTcpSecInfo avgInfo = getAvgInfoByTcpSecInfoList(secInfoList);
            avgInfo.toPrintfString();
            if (this.mHwApkQoeAiCalc.judgeQualitByAi(new float[]{(float) info.getFrequency(), (float) info.getRssi(), 0.0f, avgInfo.getPeriodTxGood(), avgInfo.getPeriodTxBad(), avgInfo.getPeriodTcpTxPacket(), avgInfo.getPeriodTcpRxPacket(), avgInfo.getPeriodTcpRsPacket(), avgInfo.getPeriodTxByte(), avgInfo.getPeriodRxByte(), avgInfo.getPeriodRtt(), avgInfo.getPeriodRttPacket(), avgInfo.getOtaRate(), avgInfo.getTcpResendRate()}, threshold)) {
                result = updateResultIfHasGoodRssi(avgInfo, 107, rssiLevel);
            } else {
                result = 106;
            }
            int result2 = updateResultIfHasNoRxPacket(avgInfo, result, rssiLevel);
            updateAppQoeChrInfo(avgInfo, this.mHwApkQoeAiCalc.getCurrentAiScore(), rssiLevel, info.getRssi());
            return result2;
        }
        HwAPPQoEUtils.logE(TAG, false, "Wifi info error", new Object[0]);
        return HwAPPQoEUtils.MSG_APP_STATE_UNKNOW;
    }

    private void updateAppQoeChrInfo(HwApkTcpSecInfo avgInfo, int aiScore, int rssiLevel, int rssi) {
        synchronized (this.mLock) {
            this.mAppQoeChrInfo.netType = 800;
            this.mAppQoeChrInfo.rtt = (int) avgInfo.getPeriodRtt();
            this.mAppQoeChrInfo.txPacket = (int) avgInfo.getPeriodTcpTxPacket();
            this.mAppQoeChrInfo.txByte = (int) avgInfo.getPeriodTxByte();
            this.mAppQoeChrInfo.rxPacket = (int) avgInfo.getPeriodTcpRxPacket();
            this.mAppQoeChrInfo.rxByte = (int) avgInfo.getPeriodRxByte();
            this.mAppQoeChrInfo.rsPacket = (int) avgInfo.getPeriodTcpRsPacket();
            this.mAppQoeChrInfo.para1 = (int) avgInfo.getPeriodTxGood();
            this.mAppQoeChrInfo.para2 = (int) avgInfo.getPeriodTxBad();
            this.mAppQoeChrInfo.para3 = rssi;
            this.mAppQoeChrInfo.para4 = aiScore;
            this.mAppQoeChrInfo.rssi = rssiLevel;
        }
    }

    private int updateResultIfHasGoodRssi(HwApkTcpSecInfo avgInfo, int result, int rssiLevel) {
        if (avgInfo.getTcpResendRate() != 0.0f || avgInfo.getOtaRate() != 0.0f || rssiLevel < 4 || avgInfo.getPeriodRtt() >= 500.0f) {
            return result;
        }
        HwAPPQoEUtils.logD(TAG, false, "has good qualit", new Object[0]);
        return 106;
    }

    private int updateResultIfHasNoRxPacket(HwApkTcpSecInfo avgInfo, int result, int rssiLevel) {
        int times;
        int periodTime = this.mPeriodTime / 1000;
        if (avgInfo.getPeriodTcpTxPacket() > 0.0f && avgInfo.getPeriodTcpRxPacket() == 0.0f && result != 107) {
            if (this.mCurrentInfo.mScenceId == 100104 || this.mCurrentInfo.mScenceId == 100201) {
                HwAPPQoEUtils.logD(TAG, false, "is a pay scence", new Object[0]);
                times = periodTime;
            } else if (getUserTypeByAppId(this.mCurrentInfo.mAppId) == 2) {
                times = 3;
            } else {
                times = 6;
            }
            if (this.mIsNoRxTime >= times) {
                HwAPPQoEUtils.logD(TAG, false, "no rx apk bad", new Object[0]);
                this.mIsNoRxTime = 0;
                return 110;
            } else if (this.mCurrentInfo.mScenceId == 100104 || this.mCurrentInfo.mScenceId == 100201) {
                this.mIsNoRxTime++;
                HwAPPQoEUtils.logD(TAG, false, "no need check system rx", new Object[0]);
                return result;
            } else if (this.mIsNoRxTime == 0) {
                isSystemTcpDataNoRx(true);
                this.mIsNoRxTime++;
                return result;
            } else if (!isSystemTcpDataNoRx(false)) {
                return result;
            } else {
                HwAPPQoEUtils.logD(TAG, false, "system data no rx too", new Object[0]);
                this.mIsNoRxTime++;
                return result;
            }
        } else if (avgInfo.getPeriodTcpRxPacket() <= 0.0f) {
            return result;
        } else {
            this.mSystemTotalRxpackte = 0;
            this.mIsNoRxTime = 0;
            return result;
        }
    }

    private int getUserTypeByAppId(int appId) {
        HwAPPQoEUserLearning hwAPPQoEUserLearning = this.mLearningManager;
        if (hwAPPQoEUserLearning != null) {
            return hwAPPQoEUserLearning.getUserTypeByAppId(appId);
        }
        return 0;
    }

    private HwApkTcpSecInfo getAvgInfoByTcpSecInfoList(List<HwApkTcpSecInfo> secInfoList) {
        HwApkTcpSecInfo avgInfo = new HwApkTcpSecInfo();
        int size = secInfoList.size();
        if (size == 0) {
            HwAPPQoEUtils.logE(TAG, false, "secInfoList Error", new Object[0]);
            return avgInfo;
        }
        float periodTotalRtt = 0.0f;
        float periodTotalRttPacket = 0.0f;
        float periodTotalTcpTxPacket = 0.0f;
        float periodTotalTcpRxPacket = 0.0f;
        float periodTotalTcpRsPacket = 0.0f;
        float periodTotalTxGood = 0.0f;
        float periodTotalTxBad = 0.0f;
        float periodTotalTxByte = 0.0f;
        float periodTotalRxByte = 0.0f;
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
        float avgOtaRate = 0.0f;
        avgInfo.setPeriodRtt(periodTotalRttPacket > 0.0f ? periodTotalRtt / periodTotalRttPacket : 0.0f);
        avgInfo.setTcpResendRate(periodTotalTcpTxPacket > 0.0f ? periodTotalTcpRsPacket / periodTotalTcpTxPacket : 0.0f);
        if (periodTotalTxGood > 0.0f) {
            avgOtaRate = periodTotalTxBad / periodTotalTxGood;
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
    public void sendMessageToSTM(int action) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(action, this.mCurrentInfo));
    }

    private boolean isSystemTcpDataNoRx(boolean isInit) {
        int[] result = this.mHwHidataJniAdapter.sendQoECmd(15, 0);
        if (result == null || result.length < 10) {
            return false;
        }
        if (isInit) {
            HwAPPQoEUtils.logD(TAG, false, "isSystemTcpDataNoRx init", new Object[0]);
            this.mSystemTotalRxpackte = result[7];
            return false;
        }
        int rxPackets = result[7] - this.mSystemTotalRxpackte;
        HwAPPQoEUtils.logD(TAG, false, "isSystemTcpDataNoRx rxPackets = %{public}d result[7] = %{public}d mSystemTotalRxpackte = %{public}d", Integer.valueOf(rxPackets), Integer.valueOf(result[7]), Integer.valueOf(this.mSystemTotalRxpackte));
        this.mSystemTotalRxpackte = result[7];
        if (rxPackets > 0) {
            return false;
        }
        return true;
    }

    public RssiPacketCountInfo getOTAInfo() {
        return this.mHwWifiBoost.getOTAInfo();
    }

    public HwAPPChrExcpInfo getAPPQoEInfo() {
        HwAPPChrExcpInfo hwAPPChrExcpInfo;
        synchronized (this.mLock) {
            HwAPPQoEUtils.logD(TAG, false, "curAppQoeInfo:%{public}s", this.mAppQoeChrInfo.toString());
            hwAPPChrExcpInfo = this.mAppQoeChrInfo;
        }
        return hwAPPChrExcpInfo;
    }

    /* access modifiers changed from: private */
    public static class HwAPKTcpInfo {
        public int rttPackteNum;
        public int rttTime;
        private long rxByte;
        public int tcpReSendPackte;
        public int tcpRxPackte;
        public int tcpTxPackte;
        public int txBad;
        private long txByte;
        public int txGood;

        public long getTxByte() {
            return this.txByte;
        }

        public void setTxByte(long txByte2) {
            this.txByte = txByte2;
        }

        public long getRxByte() {
            return this.rxByte;
        }

        public void setRxByte(long rxByte2) {
            this.rxByte = rxByte2;
        }

        public HwAPKTcpInfo() {
            this.rttTime = 0;
            this.rttPackteNum = 0;
            this.tcpTxPackte = 0;
            this.tcpRxPackte = 0;
            this.tcpReSendPackte = 0;
            this.txByte = 0;
            this.rxByte = 0;
        }

        public HwAPKTcpInfo(int[] info, RssiPacketCountInfo otaInfo) {
            this.rttTime = info[0];
            this.rttPackteNum = info[1];
            this.tcpTxPackte = info[6];
            this.tcpRxPackte = info[7];
            this.tcpReSendPackte = info[8];
            this.txGood = otaInfo.txgood;
            this.txBad = otaInfo.txbad;
            this.txByte = TrafficStats.getTxBytes(HwAPKQoEQualityMonitor.IFACE);
            this.rxByte = TrafficStats.getRxBytes(HwAPKQoEQualityMonitor.IFACE);
        }
    }

    /* access modifiers changed from: private */
    public class HwApkTcpSecInfo {
        private float otaRate = 0.0f;
        private float periodRtt = 0.0f;
        private float periodRttAvg = 0.0f;
        private float periodRttPacket = 0.0f;
        private float periodRxByte = 0.0f;
        private float periodTcpRsPacket = 0.0f;
        private float periodTcpRxPacket = 0.0f;
        private float periodTcpTxPacket = 0.0f;
        private float periodTxBad = 0.0f;
        private float periodTxByte = 0.0f;
        private float periodTxGood = 0.0f;
        private float tcpResendRate = 0.0f;

        public HwApkTcpSecInfo() {
        }

        public HwApkTcpSecInfo(HwAPKTcpInfo initInfo, HwAPKTcpInfo curInfo) {
            this.periodRttPacket = (float) (curInfo.rttPackteNum - initInfo.rttPackteNum);
            this.periodRtt = (float) (curInfo.rttTime - initInfo.rttTime);
            this.periodTcpTxPacket = (float) (curInfo.tcpTxPackte - initInfo.tcpTxPackte);
            this.periodTcpRxPacket = (float) (curInfo.tcpRxPackte - initInfo.tcpRxPackte);
            this.periodTcpRsPacket = (float) (curInfo.tcpReSendPackte - initInfo.tcpReSendPackte);
            this.periodTxGood = (float) (curInfo.txGood - initInfo.txGood);
            this.periodTxBad = (float) (curInfo.txBad - initInfo.txBad);
            this.periodTxByte = (float) (curInfo.getTxByte() - initInfo.getTxByte());
            this.periodRxByte = (float) (curInfo.getRxByte() - initInfo.getRxByte());
            float f = this.periodRttPacket;
            if (f != 0.0f) {
                this.periodRttAvg = this.periodRtt / f;
            }
            float f2 = this.periodTxGood;
            float f3 = this.periodTxBad;
            if (f2 + f3 != 0.0f) {
                this.otaRate = f3 / (f2 + f3);
            }
            float f4 = this.periodTcpTxPacket;
            if (f4 != 0.0f) {
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

        public void toPrintfString() {
            HwAPPQoEUtils.logD(HwAPKQoEQualityMonitor.TAG, false, "periodRTT =  %{public}f periodRTTPacket =  %{public}f periodTcpTxPacket =  %{public}f periodTcpRxPacket =  %{public}f periodTcpRsPacket = %{public}f periodTxGood =  %{public}f periodTxBad =  %{public}f periodTxByte =  %{public}f periodRxByte =  %{public}f otaRate =  %{public}f tcpResendRate =  %{public}f", Float.valueOf(this.periodRtt), Float.valueOf(this.periodRttPacket), Float.valueOf(this.periodTcpTxPacket), Float.valueOf(this.periodTcpRxPacket), Float.valueOf(this.periodTcpRsPacket), Float.valueOf(this.periodTxGood), Float.valueOf(this.periodTxBad), Float.valueOf(this.periodTxByte), Float.valueOf(this.periodRxByte), Float.valueOf(this.otaRate), Float.valueOf(this.tcpResendRate));
        }
    }
}
