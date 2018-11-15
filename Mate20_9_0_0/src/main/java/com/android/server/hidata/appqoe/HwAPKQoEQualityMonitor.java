package com.android.server.hidata.appqoe;

import android.common.HwFrameworkFactory;
import android.content.Context;
import android.net.wifi.RssiPacketCountInfo;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import com.android.server.hidata.HwHidataJniAdapter;
import com.android.server.hidata.hiradio.HwWifiBoost;

public class HwAPKQoEQualityMonitor {
    private static final int BAD_AVE_RTT = 800;
    private static final int CMD_QUERY_HIDATA_INFO = 23;
    private static final int CMD_QUERY_PKTS = 15;
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
    private static final int STRONG_RSSI = 2;
    private static String TAG = "HiData_HwAPKQoEQualityMonitor";
    private static final float TX_GOOD_RATE = 0.2f;
    private static final int VERY_BAD_AVE_RTT = 1500;
    public boolean isMonitoring = false;
    private HwAPPQoEAPKConfig mAPKConfig;
    private HwAPPChrExcpInfo mAPPQoEInfo = new HwAPPChrExcpInfo();
    private HwAPKTcpInfo mCurrentHwAPKTcpInfo;
    private HwAPPStateInfo mCurrentInfo;
    private int mCurrentUID = 0;
    private Handler mHandler;
    private HwAPPQoEResourceManger mHwAPPQoEResourceManger;
    private HwHidataJniAdapter mHwHidataJniAdapter;
    private HwWifiBoost mHwWifiBoost;
    private int mIsNoRxTime = 0;
    private HwAPKTcpInfo mLastHwAPKTcpInfo;
    private HwAPPQoEUserLearning mLearningManager = null;
    private Handler mLocalHandler;
    private Object mLock = new Object();
    private int mPeriodTime = 0;
    private int mScenceId = 0;
    private int mSystemTotalRxpackte = 0;
    private WifiManager mWifiManager;

    private static class HwAPKTcpInfo {
        public int rttPackteNum;
        public int rttTime;
        public int tcpReSendPackte;
        public int tcpRxPackte;
        public int tcpTxPackte;
        public int txBad;
        public int txGood;

        public HwAPKTcpInfo() {
            this.rttTime = 0;
            this.rttPackteNum = 0;
            this.tcpTxPackte = 0;
            this.tcpRxPackte = 0;
            this.tcpReSendPackte = 0;
        }

        public HwAPKTcpInfo(int[] info, RssiPacketCountInfo otaInfo) {
            this.rttTime = info[0];
            this.rttPackteNum = info[1];
            this.tcpTxPackte = info[6];
            this.tcpRxPackte = info[7];
            this.tcpReSendPackte = info[8];
            this.txGood = otaInfo.txgood;
            this.txBad = otaInfo.txbad;
        }
    }

    public HwAPKQoEQualityMonitor(Handler handler, Context context) {
        this.mHandler = handler;
        this.mHwAPPQoEResourceManger = HwAPPQoEResourceManger.getInstance();
        this.mHwHidataJniAdapter = HwHidataJniAdapter.getInstance();
        this.mHwWifiBoost = HwWifiBoost.getInstance(context);
        this.mWifiManager = (WifiManager) context.getSystemService("wifi");
        this.mLearningManager = HwAPPQoEUserLearning.getInstance();
    }

    public void startMonitor(HwAPPStateInfo info) {
        int uid = info.mAppUID;
        int scenceId = info.mScenceId;
        int appId = info.mAppId;
        String str;
        StringBuilder stringBuilder;
        if (this.isMonitoring) {
            str = TAG;
            stringBuilder = new StringBuilder();
            stringBuilder.append(" uid = ");
            stringBuilder.append(uid);
            stringBuilder.append(" mCurrentUID = ");
            stringBuilder.append(this.mCurrentUID);
            stringBuilder.append(" scenceId = ");
            stringBuilder.append(scenceId);
            stringBuilder.append(" mScenceId = ");
            stringBuilder.append(this.mScenceId);
            HwAPPQoEUtils.logD(str, stringBuilder.toString());
            if (uid == this.mCurrentUID && scenceId != this.mScenceId) {
                this.mLocalHandler.removeMessages(1);
                this.mLocalHandler.removeMessages(2);
            } else {
                return;
            }
        }
        str = TAG;
        stringBuilder = new StringBuilder();
        stringBuilder.append("uid = ");
        stringBuilder.append(uid);
        stringBuilder.append(" appId = ");
        stringBuilder.append(appId);
        stringBuilder.append(" scenceId = ");
        stringBuilder.append(scenceId);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
        initHandlerThread();
        this.mCurrentInfo = info;
        this.mAPKConfig = this.mHwAPPQoEResourceManger.getAPKScenceConfig(scenceId);
        if (this.mAPKConfig == null) {
            HwAPPQoEUtils.logD(TAG, "mAPKConfig == null");
            stopMonitor();
            return;
        }
        this.isMonitoring = true;
        this.mScenceId = scenceId;
        this.mCurrentUID = uid;
        if (this.mLearningManager != null) {
            if (this.mLearningManager.getUserTypeByAppId(info.mAppId) == 1) {
                HwAPPQoEUtils.logD(TAG, " HwAPKQoEQualityMonitor is a COMMON user");
                this.mPeriodTime = (this.mAPKConfig.mAppPeriod * 1000) * 2;
            } else {
                this.mPeriodTime = this.mAPKConfig.mAppPeriod * 1000;
                HwAPPQoEUtils.logD(TAG, " HwAPKQoEQualityMonitor is a USER_TYPE_RADICAL user");
            }
        }
        this.mLocalHandler.sendEmptyMessage(1);
    }

    public void stopMonitor() {
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(" HwAPKQoEQualityMonitor stop monitor isMonitoring = ");
        stringBuilder.append(this.isMonitoring);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
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
            public void handleMessage(Message msg) {
                int[] result;
                RssiPacketCountInfo otaInfo;
                switch (msg.what) {
                    case 1:
                        result = HwAPKQoEQualityMonitor.this.mHwHidataJniAdapter.sendQoECmd(23, HwAPKQoEQualityMonitor.this.mCurrentUID);
                        otaInfo = HwAPKQoEQualityMonitor.this.getOTAInfo();
                        if (result != null && result.length >= 10) {
                            HwAPKQoEQualityMonitor.this.mLastHwAPKTcpInfo = new HwAPKTcpInfo(result, otaInfo);
                            sendEmptyMessageDelayed(2, (long) HwAPKQoEQualityMonitor.this.mPeriodTime);
                            break;
                        }
                        sendEmptyMessageDelayed(1, (long) HwAPKQoEQualityMonitor.this.mPeriodTime);
                        break;
                    case 2:
                        result = HwAPKQoEQualityMonitor.this.mHwHidataJniAdapter.sendQoECmd(23, HwAPKQoEQualityMonitor.this.mCurrentUID);
                        otaInfo = HwAPKQoEQualityMonitor.this.getOTAInfo();
                        if (result != null && result.length >= 10) {
                            HwAPKQoEQualityMonitor.this.mCurrentHwAPKTcpInfo = new HwAPKTcpInfo(result, otaInfo);
                            HwAPKQoEQualityMonitor.this.isAPKStallThisPeriod(HwAPKQoEQualityMonitor.this.mLastHwAPKTcpInfo, HwAPKQoEQualityMonitor.this.mCurrentHwAPKTcpInfo);
                            HwAPKQoEQualityMonitor.this.mLastHwAPKTcpInfo = HwAPKQoEQualityMonitor.this.mCurrentHwAPKTcpInfo;
                        }
                        sendEmptyMessageDelayed(2, (long) HwAPKQoEQualityMonitor.this.mPeriodTime);
                        break;
                }
                super.handleMessage(msg);
            }
        };
    }

    private void release() {
        if (this.mLocalHandler != null) {
            Looper looper = this.mLocalHandler.getLooper();
            if (looper != null && looper != Looper.getMainLooper()) {
                looper.quitSafely();
            }
        }
    }

    /* JADX WARNING: Removed duplicated region for block: B:94:0x0226  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0232 A:{SYNTHETIC, Splitter: B:98:0x0232} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x01da A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0226  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0232 A:{SYNTHETIC, Splitter: B:98:0x0232} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x01da A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0226  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0232 A:{SYNTHETIC, Splitter: B:98:0x0232} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x01da A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0226  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0232 A:{SYNTHETIC, Splitter: B:98:0x0232} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x01da A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0226  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0232 A:{SYNTHETIC, Splitter: B:98:0x0232} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x01da A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0226  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0232 A:{SYNTHETIC, Splitter: B:98:0x0232} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x01da A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0226  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0232 A:{SYNTHETIC, Splitter: B:98:0x0232} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x01da A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0226  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0232 A:{SYNTHETIC, Splitter: B:98:0x0232} */
    /* JADX WARNING: Removed duplicated region for block: B:76:0x01da A:{SKIP} */
    /* JADX WARNING: Removed duplicated region for block: B:94:0x0226  */
    /* JADX WARNING: Removed duplicated region for block: B:98:0x0232 A:{SYNTHETIC, Splitter: B:98:0x0232} */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    private void isAPKStallThisPeriod(HwAPKTcpInfo initInfo, HwAPKTcpInfo curInfo) {
        Throwable th;
        HwAPKTcpInfo hwAPKTcpInfo = initInfo;
        HwAPKTcpInfo hwAPKTcpInfo2 = curInfo;
        int periodRTT = 0;
        int periodRTTPacket = 0;
        WifiInfo info = this.mWifiManager.getConnectionInfo();
        int periodTcpTxPacket;
        int periodTcpRxPacket;
        int periodTcpRsPacket;
        int tcpResendRate;
        int periodTxGood;
        double tr;
        double aveRtt;
        if (info != null) {
            int periodRTT2;
            double tr2;
            int periodTxBad;
            double otaRate;
            double aveRtt2;
            int result;
            int result2;
            periodTcpTxPacket = 0;
            periodTcpRxPacket = 0;
            int periodTcpTxPacket2 = HwFrameworkFactory.getHwInnerWifiManager().calculateSignalLevelHW(info.getFrequency(), info.getRssi());
            int periodTcpRxPacket2 = hwAPKTcpInfo2.rttPackteNum - hwAPKTcpInfo.rttPackteNum;
            if (periodTcpRxPacket2 > 0) {
                periodRTT2 = (hwAPKTcpInfo2.rttTime - hwAPKTcpInfo.rttTime) / periodTcpRxPacket2;
            } else {
                periodRTT2 = 0;
            }
            int rssiLevel = periodRTT2;
            info = hwAPKTcpInfo2.tcpTxPackte - hwAPKTcpInfo.tcpTxPackte;
            periodTcpRsPacket = 0;
            int periodTcpRsPacket2 = hwAPKTcpInfo2.tcpRxPackte - hwAPKTcpInfo.tcpRxPackte;
            tcpResendRate = 0;
            int tcpResendRate2 = hwAPKTcpInfo2.tcpReSendPackte - hwAPKTcpInfo.tcpReSendPackte;
            periodTxGood = 0;
            int periodTxGood2 = hwAPKTcpInfo2.txGood - hwAPKTcpInfo.txGood;
            int periodTxBad2 = hwAPKTcpInfo2.txBad - hwAPKTcpInfo.txBad;
            tr = 0.0d;
            if (((double) info) > 0.0d) {
                aveRtt = 0.0d;
                tr2 = ((double) tcpResendRate2) / ((double) info);
            } else {
                aveRtt = 0.0d;
                tr2 = tr;
            }
            int periodTxBad3 = info;
            int rx = periodTcpRsPacket2;
            double aveRtt3 = (double) rssiLevel;
            if (((double) periodTxGood2) > 0.0d) {
                periodTxBad = periodTxBad2;
                otaRate = ((double) periodTxBad2) / ((double) periodTxGood2);
            } else {
                periodTxBad = periodTxBad2;
                otaRate = 0.0d;
            }
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("Period Quality: period = ");
            stringBuilder.append(this.mPeriodTime);
            stringBuilder.append(" rtt = ");
            stringBuilder.append(rssiLevel);
            stringBuilder.append(" periodRTTPacket = ");
            stringBuilder.append(periodTcpRxPacket2);
            stringBuilder.append(" tcpRsRate = ");
            stringBuilder.append(tr2);
            stringBuilder.append(" otaRate = ");
            stringBuilder.append(otaRate);
            stringBuilder.append(" Txpacket = ");
            stringBuilder.append(info);
            stringBuilder.append(" periodTcpRxPacket = ");
            stringBuilder.append(periodTcpRsPacket2);
            stringBuilder.append(" RsPacket = ");
            stringBuilder.append(tcpResendRate2);
            stringBuilder.append(" txGood = ");
            stringBuilder.append(periodTxGood2);
            stringBuilder.append(" txBad = ");
            int periodTxBad4 = periodTxBad;
            stringBuilder.append(periodTxBad4);
            int periodTxBad5 = periodTxBad4;
            stringBuilder.append(" rssiLevel = ");
            stringBuilder.append(periodTcpTxPacket2);
            HwAPPQoEUtils.logD(str, stringBuilder.toString());
            if (periodTcpTxPacket2 == 3) {
                aveRtt2 = rx;
            } else if (periodTcpTxPacket2 == 4) {
                aveRtt2 = rx;
            } else {
                if (periodTcpTxPacket2 <= 2) {
                    if (periodTxBad3 >= 3) {
                        periodTxBad4 = rx;
                        if (periodTxBad4 <= 100) {
                            if (aveRtt3 > 0.0d && aveRtt3 < 800.0d && periodTcpRxPacket2 >= 2) {
                                HwAPPQoEUtils.logD(TAG, "weak rssi but aveRtt is good");
                                result = 106;
                                aveRtt2 = periodTxBad4;
                                periodRTT2 = result;
                                if (periodTxBad3 > 0) {
                                }
                                if (aveRtt2 > null) {
                                }
                                sendMessageToSTM(periodRTT2);
                                result2 = periodRTT2;
                                synchronized (this.mLock) {
                                }
                            } else if (aveRtt3 > 1500.0d && periodTcpRxPacket2 >= 2) {
                                HwAPPQoEUtils.logD(TAG, "weak rssi rtt bad");
                                result = 107;
                                aveRtt2 = periodTxBad4;
                                periodRTT2 = result;
                                if (periodTxBad3 > 0) {
                                }
                                if (aveRtt2 > null) {
                                }
                                sendMessageToSTM(periodRTT2);
                                result2 = periodRTT2;
                                synchronized (this.mLock) {
                                }
                            } else if (otaRate <= 0.20000000298023224d || periodTxBad3 <= 20) {
                                if (tr2 >= 0.20000000298023224d) {
                                    HwAPPQoEUtils.logD(TAG, "weak rssi tcp bad");
                                    result = 107;
                                    aveRtt2 = periodTxBad4;
                                    periodRTT2 = result;
                                    if (periodTxBad3 > 0 || aveRtt2 != null || periodRTT2 == 107) {
                                        if (aveRtt2 > null) {
                                            this.mSystemTotalRxpackte = 0;
                                            this.mIsNoRxTime = 0;
                                        }
                                        sendMessageToSTM(periodRTT2);
                                    } else {
                                        if (periodTcpTxPacket2 > 1.0E-323d) {
                                            otaRate = 3;
                                        } else {
                                            otaRate = 1;
                                        }
                                        if (this.mIsNoRxTime >= otaRate) {
                                            HwAPPQoEUtils.logD(TAG, "no rx apk bad");
                                            periodRTT2 = 107;
                                            sendMessageToSTM(107);
                                            this.mIsNoRxTime = 0;
                                        } else if (this.mIsNoRxTime == 0) {
                                            isSystemTcpDataNoRx(true);
                                            this.mIsNoRxTime++;
                                        } else if (isSystemTcpDataNoRx(false)) {
                                            HwAPPQoEUtils.logD(TAG, "system data no rx too");
                                            this.mIsNoRxTime++;
                                        }
                                    }
                                    result2 = periodRTT2;
                                    synchronized (this.mLock) {
                                        try {
                                            this.mAPPQoEInfo.netType = 800;
                                            this.mAPPQoEInfo.rtt = rssiLevel;
                                            this.mAPPQoEInfo.txPacket = info;
                                            this.mAPPQoEInfo.rxPacket = periodTcpRsPacket2;
                                            this.mAPPQoEInfo.rsPacket = tcpResendRate2;
                                            this.mAPPQoEInfo.para1 = periodTxGood2;
                                            this.mAPPQoEInfo.para2 = periodTxBad5;
                                            this.mAPPQoEInfo.para3 = periodTcpRxPacket2;
                                            this.mAPPQoEInfo.para4 = result2;
                                            this.mAPPQoEInfo.rssi = periodTcpTxPacket2;
                                            return;
                                        } catch (Throwable th2) {
                                            th = th2;
                                            throw th;
                                        }
                                    }
                                }
                                aveRtt2 = periodTxBad4;
                            } else {
                                HwAPPQoEUtils.logD(TAG, "weak rssi ota bad");
                                result = 107;
                                aveRtt2 = periodTxBad4;
                                periodRTT2 = result;
                                if (periodTxBad3 > 0) {
                                }
                                if (aveRtt2 > null) {
                                }
                                sendMessageToSTM(periodRTT2);
                                result2 = periodRTT2;
                                synchronized (this.mLock) {
                                }
                            }
                        }
                    } else {
                        periodTxBad4 = rx;
                    }
                    if (aveRtt3 <= 1500.0d || periodTcpRxPacket2 < 2) {
                        if (periodTxBad4 >= 1) {
                            HwAPPQoEUtils.logD(TAG, "apk good");
                            result = 106;
                            aveRtt2 = periodTxBad4;
                            periodRTT2 = result;
                            if (periodTxBad3 > 0) {
                            }
                            if (aveRtt2 > null) {
                            }
                            sendMessageToSTM(periodRTT2);
                            result2 = periodRTT2;
                            synchronized (this.mLock) {
                            }
                        }
                        aveRtt2 = periodTxBad4;
                    } else {
                        HwAPPQoEUtils.logD(TAG, "very bad rtt apk bad");
                        result = 107;
                        aveRtt2 = periodTxBad4;
                        periodRTT2 = result;
                        if (periodTxBad3 > 0) {
                        }
                        if (aveRtt2 > null) {
                        }
                        sendMessageToSTM(periodRTT2);
                        result2 = periodRTT2;
                        synchronized (this.mLock) {
                        }
                    }
                } else {
                    aveRtt2 = rx;
                }
                periodRTT2 = HwAPPQoEUtils.MSG_APP_STATE_UNKNOW;
                if (periodTxBad3 > 0) {
                }
                if (aveRtt2 > null) {
                }
                sendMessageToSTM(periodRTT2);
                result2 = periodRTT2;
                synchronized (this.mLock) {
                }
            }
            if (aveRtt3 <= 1500.0d || periodTcpRxPacket2 < 3) {
                if (tr2 >= 0.30000001192092896d && periodTxBad3 >= 20 && aveRtt <= 100) {
                    if (aveRtt3 <= 0.0d || aveRtt3 >= 800.0d || periodTcpRxPacket2 < 3) {
                        HwAPPQoEUtils.logD(TAG, "STRONG_RSSI tcp bad");
                        result = 107;
                    } else {
                        HwAPPQoEUtils.logD(TAG, "tcp is bad but rtt is good ");
                        result = 106;
                    }
                    periodRTT2 = result;
                    if (periodTxBad3 > 0) {
                    }
                    if (aveRtt2 > null) {
                    }
                    sendMessageToSTM(periodRTT2);
                    result2 = periodRTT2;
                    synchronized (this.mLock) {
                    }
                }
                periodRTT2 = HwAPPQoEUtils.MSG_APP_STATE_UNKNOW;
                if (periodTxBad3 > 0) {
                }
                if (aveRtt2 > null) {
                }
                sendMessageToSTM(periodRTT2);
                result2 = periodRTT2;
                synchronized (this.mLock) {
                }
            } else {
                result = 107;
                periodRTT2 = result;
                if (periodTxBad3 > 0) {
                }
                if (aveRtt2 > null) {
                }
                sendMessageToSTM(periodRTT2);
                result2 = periodRTT2;
                synchronized (this.mLock) {
                }
            }
        } else {
            periodTcpTxPacket = 0;
            periodTcpRxPacket = 0;
            periodTcpRsPacket = 0;
            tcpResendRate = 0;
            periodTxGood = 0;
            tr = 0.0d;
            aveRtt = 0.0d;
        }
    }

    private void sendMessageToSTM(int action) {
        this.mHandler.sendMessage(this.mHandler.obtainMessage(action, this.mCurrentInfo));
    }

    private boolean isSystemTcpDataNoRx(boolean isInit) {
        int[] result = this.mHwHidataJniAdapter.sendQoECmd(15, 0);
        if (result == null || result.length < 10) {
            return false;
        }
        if (isInit) {
            HwAPPQoEUtils.logD(TAG, "isSystemTcpDataNoRx init");
            this.mSystemTotalRxpackte = result[7];
            return false;
        }
        int rxPackets = result[7] - this.mSystemTotalRxpackte;
        String str = TAG;
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("isSystemTcpDataNoRx rxPackets = ");
        stringBuilder.append(rxPackets);
        stringBuilder.append(" result[7] = ");
        stringBuilder.append(result[7]);
        stringBuilder.append(" mSystemTotalRxpackte = ");
        stringBuilder.append(this.mSystemTotalRxpackte);
        HwAPPQoEUtils.logD(str, stringBuilder.toString());
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
            String str = TAG;
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append("curAPPQoEInfo:");
            stringBuilder.append(this.mAPPQoEInfo.toString());
            HwAPPQoEUtils.logD(str, stringBuilder.toString());
            hwAPPChrExcpInfo = this.mAPPQoEInfo;
        }
        return hwAPPChrExcpInfo;
    }
}
